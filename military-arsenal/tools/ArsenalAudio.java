import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Original procedural game sound design, CC0. No sampled recordings or real-weapon recordings. */
public final class ArsenalAudio {
    private static final int RATE=22050;
    private static final String[] NAMES={"pistol","smg","rifle","shotgun","precision","rocket","grenade","reload_out","reload_in","bolt","dry","impact"};
    private static final double[] LENGTHS={.24,.16,.3,.42,.55,.7,.36,.25,.22,.18,.1,.95};
    private static final double[] TONES={180,260,125,80,110,55,65,620,820,950,1450,38};
    public static void main(String[] args) throws Exception {
        String ffmpeg;
        if(args.length>1) ffmpeg=args[1];
        else { Class<?> locator=Class.forName("ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator"); ffmpeg=(String)locator.getMethod("getExecutablePath").invoke(locator.getConstructor().newInstance()); }
        Path root=Path.of(args[0]).resolve("assets/arsenal/sounds"),temp=Files.createTempDirectory("arsenal-audio-");
        Files.createDirectories(root);
        try {
            for(int n=0;n<NAMES.length;n++) {
                int samples=(int)(RATE*LENGTHS[n]); double[] values=new double[samples]; Random random=new Random(7361+NAMES[n].hashCode());
                double peak=0,low=0;
                for(int i=0;i<samples;i++) {
                    double t=(double)i/RATE,noise=random.nextDouble()*2-1; low=low*.8+noise*.2;
                    double decay=Math.exp(-t/(LENGTHS[n]*.18)),attack=Math.min(1,t/.002);
                    double tone=Math.sin(2*Math.PI*(TONES[n]*t-20*t*t));
                    double ring=Math.sin(2*Math.PI*TONES[n]*3.17*t)*Math.exp(-t*36);
                    double value=(.45*tone+.36*noise+.18*low+.1*ring)*attack*decay;
                    if(n==5) value=(noise*.6+low*.7+tone*.1)*Math.min(1,t/.025)*Math.exp(-t*6);
                    if(n==11) value=(low+tone*.2)*Math.min(1,t/.01)*Math.exp(-t*4.5)+noise*.15*Math.exp(-t*16);
                    if(n>=7&&n<=10) {
                        double click=Math.exp(-Math.pow((t-.045)/.004,2))+Math.exp(-Math.pow((t-.115)/.006,2))*.7;
                        value=(noise*.5+tone*.25+ring*.25)*click;
                    }
                    value*=Math.min(1,(LENGTHS[n]-t)/.02);
                    values[i]=value; peak=Math.max(peak,Math.abs(value));
                }
                ByteBuffer pcm=ByteBuffer.allocate(samples*2).order(ByteOrder.LITTLE_ENDIAN);
                for(double v:values) pcm.putShort((short)Math.round(v*.62/Math.max(.001,peak)*32767));
                Path wav=temp.resolve(NAMES[n]+".wav"),ogg=root.resolve(NAMES[n]+".ogg"),decoded=temp.resolve(NAMES[n]+".pcm");
                try(var audio=new AudioInputStream(new ByteArrayInputStream(pcm.array()),new AudioFormat(RATE,16,1,true,false),samples)) { AudioSystem.write(audio,AudioFileFormat.Type.WAVE,wav.toFile()); }
                run(ffmpeg,temp,List.of("-v","error","-y","-i",wav.toString(),"-map_metadata","-1","-fflags","+bitexact","-flags:a","+bitexact","-c:a","libvorbis","-q:a","3",ogg.toString()));
                run(ffmpeg,temp,List.of("-v","error","-y","-i",ogg.toString(),"-ac","1","-ar",Integer.toString(RATE),"-f","s16le",decoded.toString()));
                byte[] data=Files.readAllBytes(decoded); ByteBuffer buffer=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                int max=0; while(buffer.remaining()>=2) max=Math.max(max,Math.abs((int)buffer.getShort()));
                if(data.length<samples || max<100 || max>=32767) throw new IOException("Silent, clipped or truncated sound: "+NAMES[n]);
                System.out.println("PASS Vorbis mono decode, no clipping: "+NAMES[n]+" peak="+max);
            }
            Files.writeString(root.resolve("LICENSE.txt"),"Original procedural Military Arsenal sound effects. CC0-1.0. No external recordings. FFmpeg and JAVE are build-only tools, not shipped in the mod.\n");
        } finally { try(var walk=Files.walk(temp)) { for(Path file:walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file); } }
    }
    private static void run(String executable,Path temp,List<String> args) throws Exception {
        List<String> cmd=new ArrayList<>(); cmd.add(executable);cmd.addAll(args); Path log=temp.resolve("encoder.log");
        Process p=new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!p.waitFor(30,TimeUnit.SECONDS)) { p.destroyForcibly();throw new IOException("Audio encoder timeout"); }
        if(p.exitValue()!=0) throw new IOException("Audio encoder failed: "+Files.readString(log));
    }
}
