import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Original periodic mechanical synthesis. No recordings or runtime encoder dependency. */
public final class VehicleAudioGenerator {
    static final int RATE=32000, SAMPLES=128000;
    static final String[] NAMES={"engine","motorcycle","boat","plane","helicopter","drone"};
    static final double[] FIRING={32,46,40,48,18,240};
    public static void main(String[] args) throws Exception {
        if(args.length<1 || args.length>2) throw new IllegalArgumentException("output directory [ffmpeg executable]");
        String encoder;
        if(args.length==2) encoder=args[1];
        else {
            Class<?> locator=Class.forName("ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator");
            encoder=(String)locator.getMethod("getExecutablePath").invoke(locator.getConstructor().newInstance());
        }
        Path root=Path.of(args[0]).resolve("assets/harvester/sounds");Files.createDirectories(root);
        Path temp=Files.createTempDirectory("harvester-audio-");List<String> manifest=new ArrayList<>();
        try {
            run(temp,"version",List.of(encoder,"-version"));
            Files.copy(temp.resolve("version.log"),root.resolve("encoder-version.txt"),StandardCopyOption.REPLACE_EXISTING);
            for(int p=0;p<NAMES.length;p++) {
                String name=NAMES[p];double[] samples=synthesize(p);
                ByteBuffer pcm=ByteBuffer.allocate(SAMPLES*2).order(ByteOrder.LITTLE_ENDIAN);
                for(double value:samples) pcm.putShort((short)Math.round(value*32767));
                Path wav=temp.resolve(name+".wav"),ogg=root.resolve(name+".ogg"),decoded=temp.resolve(name+".pcm");
                AudioFormat format=new AudioFormat(RATE,16,1,true,false);
                try(var stream=new AudioInputStream(new ByteArrayInputStream(pcm.array()),format,SAMPLES)) { AudioSystem.write(stream,AudioFileFormat.Type.WAVE,wav.toFile()); }
                run(temp,name+"-encode",List.of(encoder,"-v","error","-y","-i",wav.toString(),"-map_metadata","-1","-fflags","+bitexact","-flags:a","+bitexact","-c:a","libvorbis","-q:a","4",ogg.toString()));
                run(temp,name+"-decode",List.of(encoder,"-v","error","-y","-i",ogg.toString(),"-ac","1","-ar",Integer.toString(RATE),"-f","s16le",decoded.toString()));
                if(Files.size(decoded)!=SAMPLES*2) throw new IOException("Unexpected decoded sample count: "+name);
                byte[] data=Files.readAllBytes(ogg);
                if(data.length<100 || data[0]!='O' || data[1]!='g' || data[2]!='g' || data[3]!='S') throw new IOException("Not Ogg: "+name);
                int id=-1;
                for(int i=0;i<Math.min(128,data.length-16);i++) if(data[i]==1 && new String(data,i+1,6,java.nio.charset.StandardCharsets.US_ASCII).equals("vorbis")) { id=i;break; }
                if(id<0 || data[id+11]!=1 || ByteBuffer.wrap(data,id+12,4).order(ByteOrder.LITTLE_ENDIAN).getInt()!=RATE) throw new IOException("Invalid mono Vorbis: "+name);
                ByteBuffer raw=ByteBuffer.wrap(Files.readAllBytes(decoded)).order(ByteOrder.LITTLE_ENDIAN);
                double peak=0,sum=0,squares=0,first=0,last=0,maxStep=0,prev=0;
                for(int i=0;i<SAMPLES;i++) {
                    double value=raw.getShort()/32768.0;if(i==0) first=value;else maxStep=Math.max(maxStep,Math.abs(value-prev));
                    prev=value;last=value;peak=Math.max(peak,Math.abs(value));sum+=value;squares+=value*value;
                }
                double rms=Math.sqrt(squares/SAMPLES),seam=Math.abs(last-first),dc=Math.abs(sum/SAMPLES);
                if(rms<.07 || rms>.20 || peak>=.95 || dc>.002 || seam>.06 || seam>maxStep*1.5+.001) throw new IOException("Audio quality contract failed: "+name+" rms="+rms+" peak="+peak+" seam="+seam+" dc="+dc);
                String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
                manifest.add(String.format(Locale.ROOT,"\"%s\":{\"sha256\":\"%s\",\"bytes\":%d,\"samples\":%d,\"channels\":1,\"sampleRate\":%d,\"rmsDb\":%.4f,\"peak\":%.6f,\"seamDelta\":%.6f,\"dcOffset\":%.6f}",name,sha,data.length,SAMPLES,RATE,20*Math.log10(rms),peak,seam,dc));
                System.out.printf(Locale.ROOT,"PASS %s: mono %d Hz, %d samples, RMS %.2f dBFS, peak %.3f, seam %.5f, sha256=%s%n",name,RATE,SAMPLES,20*Math.log10(rms),peak,seam,sha);
            }
            Files.writeString(root.resolve("manifest.json"),"{"+String.join(",",manifest)+"}\n");
            Files.writeString(root.resolve("LICENSE.txt"),"Original synthesized Harvester audio, 2026. CC0-1.0, as the repository LICENSE; https://creativecommons.org/publicdomain/zero/1.0/. No sampled recordings. FFmpeg/JAVE are external build tools, not included in this mod.\n");
        } finally { try(var walk=Files.walk(temp)) { for(Path p:walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
    }
    static double[] synthesize(int profile) {
        if(profile<0 || profile>=NAMES.length) throw new IllegalArgumentException("Unknown audio profile");
        Random random=new Random(0x48415256L+NAMES[profile].hashCode());
        double[] noise=new double[SAMPLES];for(int i=0;i<SAMPLES;i++) noise[i]=random.nextGaussian();
        double[] low=periodicLowpass(noise,.975),mid=periodicLowpass(noise,.72),air=periodicLowpass(noise,.28);
        double[] result=new double[SAMPLES];double mean=0;
        for(int i=0;i<SAMPLES;i++) {
            double t=i/(double)RATE,phase=2*Math.PI*FIRING[profile]*t;
            double pulse=Math.pow(Math.max(0,Math.sin(phase)),12),body=0;
            if(profile<4) {
                for(int h=1;h<=9;h++) body+=Math.sin(phase*h+.13*h)/Math.pow(h,1.55);
                double breath=.7+.3*Math.sin(phase*.5);
                result[i]=.22*body+.28*(pulse-.113)+low[i]*1.15+(mid[i]-low[i])*(.13+.24*pulse)*breath;
                if(profile==1) result[i]+=.055*Math.sin(phase*.5)+.025*Math.sin(phase*5);
                if(profile==2) result[i]+=(mid[i]-low[i])*.18;
                if(profile==3) result[i]+=(air[i]-mid[i])*.035+Math.sin(phase*.5)*.05;
            } else if(profile==4) {
                double chop=Math.pow(.5+.5*Math.sin(phase),4);
                result[i]=.22*Math.sin(phase)+.11*Math.sin(phase*2)+.8*low[i]+(mid[i]-low[i])*(.18+.75*chop)+.025*Math.sin(phase*8);
            } else {
                double beat=1+.09*Math.sin(2*Math.PI*3*t);
                result[i]=beat*(.17*Math.sin(phase)+.065*Math.sin(phase*2)+.022*Math.sin(phase*4))+.13*(mid[i]-low[i])+.022*(air[i]-mid[i]);
            }
            result[i]=Math.tanh(result[i]);mean+=result[i];
        }
        mean/=SAMPLES;double sum=0,peak=0;
        for(int i=0;i<SAMPLES;i++) { result[i]-=mean;sum+=result[i]*result[i];peak=Math.max(peak,Math.abs(result[i])); }
        double gain=Math.min(.12/Math.sqrt(sum/SAMPLES),.72/peak);for(int i=0;i<SAMPLES;i++) result[i]*=gain;
        return result;
    }
    private static double[] periodicLowpass(double[] input,double memory) {
        double state=0;for(double x:input) state=state*memory+x*(1-memory);
        state/=1-Math.pow(memory,input.length);double[] out=new double[input.length];
        for(int i=0;i<input.length;i++) { state=state*memory+input[i]*(1-memory);out[i]=state; }return out;
    }
    private static void run(Path temp,String name,List<String> command) throws Exception {
        Path log=temp.resolve(name+".log");Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)) { process.destroyForcibly();throw new IOException("Encoder timeout: "+name); }
        if(process.exitValue()!=0) throw new IOException("Encoder failed: "+Files.readString(log));
    }
}
