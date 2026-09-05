import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Original periodic synthesis; encoder is a build tool, never bundled in the mod. */
public final class VehicleAudioGenerator {
    static final int RATE=16000, SAMPLES=32000, SOURCE_SAMPLES=SAMPLES;
    static final String[] NAMES={"engine","motorcycle","boat","plane","helicopter","drone"};
    static final int[] FREQUENCIES={40,75,45,95,20,240};
    static final int[] HARMONICS={10,8,10,7,16,5};
    static final double[] PULSES={.60,.45,.65,.30,.80,.12};
    public static void main(String[] args) throws Exception {
        if(args.length<1 || args.length>2) throw new IllegalArgumentException("output directory [ffmpeg executable]");
        String encoder;
        if(args.length==2) encoder=args[1];
        else {
            Class<?> locator=Class.forName("ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator");
            encoder=(String)locator.getMethod("getExecutablePath").invoke(locator.getConstructor().newInstance());
        }
        Path root=Path.of(args[0]).resolve("assets/harvester/sounds"); Files.createDirectories(root);
        Path temp=Files.createTempDirectory("harvester-audio-");
        List<String> manifest=new ArrayList<>();
        try {
            run(temp,"version",List.of(encoder,"-version"));
            Files.copy(temp.resolve("version.log"),root.resolve("encoder-version.txt"),StandardCopyOption.REPLACE_EXISTING);
            for(int p=0;p<NAMES.length;p++) {
                String name=NAMES[p]; Random random=new Random(name.hashCode());
                double[] phases=new double[18]; for(int k=0;k<18;k++) phases[k]=random.nextDouble()*2*Math.PI;
                double[] samples=new double[SOURCE_SAMPLES]; double peak=0;
                for(int i=0;i<SOURCE_SAMPLES;i++) {
                    double t=(double)i/RATE,tone=0,air=0;
                    for(int k=1;k<=HARMONICS[p];k++) tone+=Math.sin(2*Math.PI*FREQUENCIES[p]*k*t)/Math.pow(k,1.45);
                    for(int k=0;k<18;k++) air+=Math.sin(2*Math.PI*(315+k*175)*t+phases[k])/18;
                    double env=1-PULSES[p]+PULSES[p]*Math.pow(.5+.5*Math.sin(Math.PI*FREQUENCIES[p]*t),2);
                    samples[i]=.4*tone*env+.09*air; peak=Math.max(peak,Math.abs(samples[i]));
                }
                ByteBuffer pcm=ByteBuffer.allocate(SOURCE_SAMPLES*2).order(ByteOrder.LITTLE_ENDIAN);
                for(double value:samples) pcm.putShort((short)Math.round(value*.65/peak*32767));
                Path wav=temp.resolve(name+".wav"),ogg=root.resolve(name+".ogg"),decoded=temp.resolve(name+".pcm");
                AudioFormat format=new AudioFormat(RATE,16,1,true,false);
                try(var stream=new AudioInputStream(new ByteArrayInputStream(pcm.array()),format,SOURCE_SAMPLES)) {
                    AudioSystem.write(stream,AudioFileFormat.Type.WAVE,wav.toFile());
                }
                run(temp,name+"-encode",List.of(encoder,"-v","error","-y","-i",wav.toString(),"-map_metadata","-1","-c:a","libvorbis","-q:a","2",ogg.toString()));
                run(temp,name+"-decode",List.of(encoder,"-v","error","-y","-i",ogg.toString(),"-ac","1","-ar",Integer.toString(RATE),"-f","s16le",decoded.toString()));
                if(Files.size(decoded)!=SAMPLES*2) throw new IOException("Unexpected decoded sample count: "+name);
                byte[] data=Files.readAllBytes(ogg);
                if(data.length<100 || data[0]!='O' || data[1]!='g' || data[2]!='g' || data[3]!='S') throw new IOException("Not Ogg: "+name);
                int id=-1;
                for(int i=0;i<Math.min(128,data.length-16);i++) if(data[i]==1 && new String(data,i+1,6,java.nio.charset.StandardCharsets.US_ASCII).equals("vorbis")) { id=i; break; }
                if(id<0 || data[id+11]!=1 || ByteBuffer.wrap(data,id+12,4).order(ByteOrder.LITTLE_ENDIAN).getInt()!=RATE) throw new IOException("Not mono Vorbis at expected rate: "+name);
                byte[] raw=Files.readAllBytes(decoded); ByteBuffer decodedPcm=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
                int max=0; for(int i=0;i<SAMPLES;i++) max=Math.max(max,Math.abs((int)decodedPcm.getShort()));
                if(max<100 || max>=32767) throw new IOException("Silent/clipped decoded audio: "+name);
                String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
                manifest.add("\""+name+"\":{\"sha256\":\""+sha+"\",\"bytes\":"+data.length+",\"samples\":"+SAMPLES+",\"channels\":1,\"sampleRate\":"+RATE+"}");
                System.out.println("PASS mono Vorbis decode: "+name+" samples="+SAMPLES+" sha256="+sha);
            }
            Files.writeString(root.resolve("manifest.json"),"{"+String.join(",",manifest)+"}\n");
            Files.writeString(root.resolve("LICENSE.txt"),"Original synthesized Harvester audio, 2026. CC0-1.0, as the repository LICENSE; https://creativecommons.org/publicdomain/zero/1.0/. No sampled recordings. FFmpeg/JAVE are external build tools, not included in this mod.\n");
        } finally {
            try(var walk=Files.walk(temp)) { for(Path p:walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
        }
    }
    private static void run(Path temp,String name,List<String> command) throws Exception {
        Path log=temp.resolve(name+".log");
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("Encoder timeout: "+name); }
        if(process.exitValue()!=0) throw new IOException("Encoder failed: "+Files.readString(log));
    }
}
