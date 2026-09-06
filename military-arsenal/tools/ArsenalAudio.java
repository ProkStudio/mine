import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Original layered game SFX, CC0. Encoder is build-only; no external recordings. */
public final class ArsenalAudio {
    private static final int RATE=44100;
    private static final String[] NAMES={"pistol","smg","rifle","shotgun","precision","rocket","grenade","reload_out","reload_in","bolt","dry","impact"};
    private static final double[] LENGTHS={.55,.38,.65,.8,.95,1.2,.65,.35,.32,.28,.12,1.5};
    private static final double[] BODY={155,200,105,72,88,48,62,620,820,950,1450,38};
    private static double[] synth(int n,int variant) {
        int size=(int)(RATE*LENGTHS[n]); double[] v=new double[size];
        Random random=new Random(7361L+NAMES[n].hashCode()*31L+variant);
        double low=0,mid=0,previous=0,phase=0;
        for(int i=0;i<size;i++) {
            double t=(double)i/RATE,noise=random.nextDouble()*2-1;
            low+=.028*(noise-low); mid+=.22*(noise-mid);
            double high=noise-mid,band=mid-low;
            phase+=2*Math.PI*(BODY[n]*(1+.06*variant)*(.72+.6*Math.exp(-t*22)))/RATE;
            double body=Math.sin(phase)+.3*Math.sin(phase*1.73);
            double attack=Math.min(1,t/.0015),value;
            if(n<5 || n==6) {
                // Short crack + resonant low body + broadband report and lingering air.
                double decay=LENGTHS[n]*(n==1?.2:.26);
                value=.32*high*Math.exp(-t/.009)+1.7*band*Math.exp(-t/decay)
                    +.32*body*Math.exp(-t/(decay*.85))+1.3*low*Math.exp(-t/(decay*1.7));
            } else if(n==5) {
                // Sustained launcher rush, not a pistol click at a lower pitch.
                attack=Math.min(1,t/.018);
                value=(1.9*band+2.3*low+.10*body)*Math.exp(-t/.29)
                    +.35*high*Math.exp(-t/.035);
            } else if(n==11) {
                attack=Math.min(1,t/.007);
                value=(3.6*low+.3*body)*Math.exp(-t/.38)
                    +1.2*band*Math.exp(-t/.14)+.12*high*Math.exp(-t/.018);
            } else {
                double first=n==10?.012:.03,second=n==10?.035:.13;
                double click=Math.exp(-Math.pow((t-first)/.005,2))+.65*Math.exp(-Math.pow((t-second)/.009,2));
                double slide=n==10?0:Math.sin(Math.PI*Math.min(1,t/.2))*Math.exp(-t/.1);
                value=(.6*high+.25*body)*click+.7*band*slide;
            }
            value*=attack*Math.min(1,(LENGTHS[n]-t)/.045);
            // Remove DC without thinning the audible body.
            previous=previous*.999+value*.001; v[i]=value-previous;
        }
        if(n<7 || n==11) {
            double[] dry=v.clone();
            for(int delay:new int[]{(int)(RATE*.047),(int)(RATE*.093),(int)(RATE*.151)})
                for(int i=delay;i<size;i++) v[i]+=dry[i-delay]*(delay<RATE*.05?.17:delay<RATE*.1?.10:.055);
        }
        double peak=Arrays.stream(v).map(Math::abs).max().orElse(1);
        for(int i=0;i<size;i++) v[i]=v[i]*.68/Math.max(.001,peak)*Math.min(1,(size-i)/(RATE*.02));
        return v;
    }
    public static void main(String[] args) throws Exception {
        String ffmpeg;
        if(args.length>1) ffmpeg=args[1];
        else { Class<?> locator=Class.forName("ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator"); ffmpeg=(String)locator.getMethod("getExecutablePath").invoke(locator.getConstructor().newInstance()); }
        Path root=Path.of(args[0]).resolve("assets/arsenal/sounds"),temp=Files.createTempDirectory("arsenal-audio-");
        Files.createDirectories(root);
        try {
            for(int n=0;n<NAMES.length;n++) for(int variant=0;variant<(n<7?2:1);variant++) {
                String name=NAMES[n]+(variant==0?"":"_2"); double[] values=synth(n,variant);
                ByteBuffer pcm=ByteBuffer.allocate(values.length*2).order(ByteOrder.LITTLE_ENDIAN);
                for(double v:values) pcm.putShort((short)Math.round(v*32767));
                Path wav=temp.resolve(name+".wav"),ogg=root.resolve(name+".ogg"),decoded=temp.resolve(name+".pcm");
                try(var audio=new AudioInputStream(new ByteArrayInputStream(pcm.array()),new AudioFormat(RATE,16,1,true,false),values.length)) { AudioSystem.write(audio,AudioFileFormat.Type.WAVE,wav.toFile()); }
                run(ffmpeg,temp,List.of("-v","error","-y","-i",wav.toString(),"-map_metadata","-1","-fflags","+bitexact","-flags:a","+bitexact","-c:a","libvorbis","-q:a","5",ogg.toString()));
                run(ffmpeg,temp,List.of("-v","error","-y","-i",ogg.toString(),"-ac","1","-ar",Integer.toString(RATE),"-f","s16le",decoded.toString()));
                byte[] data=Files.readAllBytes(decoded); ByteBuffer buffer=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                int max=0,index=0; double total=0,tail=0;
                while(buffer.remaining()>=2) { double sample=buffer.getShort(); max=Math.max(max,(int)Math.abs(sample)); total+=sample*sample; if(index++>RATE*.04) tail+=sample*sample; }
                if(Math.abs(data.length/2-values.length)>RATE/20 || max<100 || max>=32767) throw new IOException("Silent, clipped or truncated: "+name);
                if((n<7 || n==11) && tail/Math.max(1,total)<.12) throw new IOException("Report collapsed into a click: "+name);
                System.out.printf(Locale.ROOT,"PASS mono Vorbis %s: peak=%d, energy after 40ms=%.1f%%%n",name,max,100*tail/Math.max(1,total));
            }
            Files.writeString(root.resolve("LICENSE.txt"),"Original layered procedural Military Arsenal game sound effects. CC0-1.0. No external recordings. FFmpeg/JAVE are build-only and not shipped.\n");
        } finally { try(var walk=Files.walk(temp)) { for(Path file:walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file); } }
    }
    private static void run(String executable,Path temp,List<String> args) throws Exception {
        List<String> cmd=new ArrayList<>();cmd.add(executable);cmd.addAll(args);Path log=temp.resolve("encoder.log");
        Process p=new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!p.waitFor(30,TimeUnit.SECONDS)) { p.destroyForcibly();throw new IOException("Audio encoder timeout"); }
        if(p.exitValue()!=0) throw new IOException("Audio encoder failed: "+Files.readString(log));
    }
}
