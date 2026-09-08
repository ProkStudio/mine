import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Original, periodic CC0 mechanical loop. Encoder is a build tool, never a mod dependency. */
public final class GenerateEngineAudio {
    private static final int RATE = 32000, SAMPLES = RATE * 4;
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("output directory [ffmpeg executable]");
        String encoder;
        if (args.length == 2) encoder = args[1];
        else {
            Class<?> locator = Class.forName("ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator");
            encoder = (String) locator.getMethod("getExecutablePath").invoke(locator.getConstructor().newInstance());
        }
        Path root = Path.of(args[0]).resolve("assets/militaryvehicles/sounds");
        Files.createDirectories(root);
        Path temp = Files.createTempDirectory("militaryvehicles-audio-");
        try {
            double[] samples = synthesize();
            ByteBuffer pcm = ByteBuffer.allocate(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (double value : samples) pcm.putShort((short) Math.round(value * 32767));
            Path wav = temp.resolve("truck.wav"), ogg = root.resolve("truck_engine.ogg"), decoded = temp.resolve("decoded.pcm");
            AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
            try (var stream = new AudioInputStream(new ByteArrayInputStream(pcm.array()), format, SAMPLES)) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wav.toFile());
            }
            run(temp, "encode", List.of(encoder, "-v", "error", "-y", "-i", wav.toString(), "-map_metadata", "-1",
                "-fflags", "+bitexact", "-flags:a", "+bitexact", "-c:a", "libvorbis", "-q:a", "4", ogg.toString()));
            run(temp, "decode", List.of(encoder, "-v", "error", "-y", "-i", ogg.toString(), "-ac", "1", "-ar",
                Integer.toString(RATE), "-f", "s16le", decoded.toString()));
            if (Files.size(decoded) != SAMPLES * 2L) throw new IOException("Wrong decoded sample count");
            byte[] data = Files.readAllBytes(ogg);
            if (data.length < 128 || !new String(data, 0, 4, StandardCharsets.US_ASCII).equals("OggS")) throw new IOException("Not Ogg");
            int id = -1;
            for (int i = 0; i < Math.min(128, data.length - 16); i++) {
                if (data[i] == 1 && new String(data, i + 1, 6, StandardCharsets.US_ASCII).equals("vorbis")) { id = i; break; }
            }
            if (id < 0 || data[id + 11] != 1 || ByteBuffer.wrap(data, id + 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() != RATE)
                throw new IOException("Expected mono 32 kHz Vorbis");
            ByteBuffer raw = ByteBuffer.wrap(Files.readAllBytes(decoded)).order(ByteOrder.LITTLE_ENDIAN);
            double peak = 0, sum = 0, squares = 0, first = 0, last = 0, maxStep = 0, previous = 0;
            for (int i = 0; i < SAMPLES; i++) {
                double value = raw.getShort() / 32768.0;
                if (i == 0) first = value; else maxStep = Math.max(maxStep, Math.abs(value - previous));
                previous = value; last = value; peak = Math.max(peak, Math.abs(value)); sum += value; squares += value * value;
            }
            double rms = Math.sqrt(squares / SAMPLES), seam = Math.abs(last - first), dc = Math.abs(sum / SAMPLES);
            if (rms < .07 || rms > .20 || peak >= .95 || dc > .002 || seam > .06 || seam > maxStep * 1.5 + .001)
                throw new IOException("Decoded audio quality contract failed: rms=" + rms + " peak=" + peak + " seam=" + seam + " dc=" + dc);
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
            String manifest = String.format(Locale.ROOT,
                "{\"truck_engine\":{\"sha256\":\"%s\",\"bytes\":%d,\"samples\":%d,\"channels\":1,\"sampleRate\":%d,\"rms\":%.8f,\"peak\":%.8f,\"seamDelta\":%.8f,\"dcOffset\":%.8f}}%n",
                sha, data.length, SAMPLES, RATE, rms, peak, seam, dc);
            Files.writeString(root.resolve("manifest.json"), manifest, StandardCharsets.UTF_8);
            Files.writeString(root.resolve("LICENSE.txt"),
                "Original synthesized Military Vehicles audio, 2026. CC0-1.0, as the repository LICENSE. No sampled recordings. FFmpeg/JAVE are external build tools, not included in the mod.\n",
                StandardCharsets.UTF_8);
            System.out.println("TRUCK_AUDIO_PASS " + manifest.strip());
        } finally {
            try (var walk = Files.walk(temp)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }

    private static double[] synthesize() {
        Random random = new Random(0x3658364D494C4CL);
        double[] noise = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) noise[i] = random.nextGaussian();
        double[] low = periodicLowpass(noise, .978), mid = periodicLowpass(noise, .74);
        double[] result = new double[SAMPLES]; double mean = 0;
        for (int i = 0; i < SAMPLES; i++) {
            double t = i / (double) RATE, phase = 2 * Math.PI * 28 * t;
            double pulse = Math.pow(Math.max(0, Math.sin(phase)), 10), body = 0;
            for (int h = 1; h <= 8; h++) body += Math.sin(phase * h + .17 * h) / Math.pow(h, 1.65);
            double breath = .82 + .18 * Math.sin(phase * .5);
            result[i] = Math.tanh(.27 * body + .23 * (pulse - .123) + .055 * Math.sin(phase * .5)
                + .9 * low[i] + (mid[i] - low[i]) * (.14 + .25 * pulse) * breath);
            mean += result[i];
        }
        mean /= SAMPLES; double squares = 0, peak = 0;
        for (int i = 0; i < SAMPLES; i++) { result[i] -= mean; squares += result[i] * result[i]; peak = Math.max(peak, Math.abs(result[i])); }
        double gain = Math.min(.12 / Math.sqrt(squares / SAMPLES), .72 / peak);
        for (int i = 0; i < SAMPLES; i++) result[i] *= gain;
        return result;
    }

    private static double[] periodicLowpass(double[] input, double memory) {
        double state = 0;
        for (double value : input) state = state * memory + value * (1 - memory);
        state /= 1 - Math.pow(memory, input.length);
        double[] out = new double[input.length];
        for (int i = 0; i < input.length; i++) { state = state * memory + input[i] * (1 - memory); out[i] = state; }
        return out;
    }

    private static void run(Path temp, String name, List<String> command) throws Exception {
        Path log = temp.resolve(name + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new IOException("Audio encoder timeout: " + name);
            if (process.exitValue() != 0) throw new IOException("Audio encoder failed: " + Files.readString(log));
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
        }
    }
}
