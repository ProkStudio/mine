import com.harvester.vehicle.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Offline acceptance of pure geometry/animation/resource contracts. Not a Minecraft client test. */
public final class VehicleVisualSmoke {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    private static PassengerAnimation.Input rider(double time,VehicleType type,int seat,long identity,double steer,double drive,double speed) {
        return new PassengerAnimation.Input(type,seat,identity,time,steer,drive,speed,false);
    }
    private static void finitePose(PassengerAnimation.Pose p) {
        for(var joint:List.of(p.rightArm(),p.leftArm(),p.rightLeg(),p.leftLeg())) {
            check(Float.isFinite(joint.pitch()) && Float.isFinite(joint.yaw()) && Float.isFinite(joint.roll()),"Finite rider joint");
            check(Math.abs(joint.pitch())<1.7 && Math.abs(joint.yaw())<.9 && Math.abs(joint.roll())<.1,"Bounded rider joint");
        }
        check(Float.isFinite(p.leanPitch()) && Math.abs(p.leanPitch())<=.113,"Bounded pitch");
        check(Float.isFinite(p.leanRoll()) && Math.abs(p.leanRoll())<=.041,"Bounded roll");
        check(p.weight()>=0 && p.weight()<=1,"Entry blend");
    }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]);VehicleAssetGenerator.main(new String[]{output.toString()});
        Map<String,byte[]> before=new TreeMap<>();
        try(var files=Files.walk(output)) { for(Path p:files.filter(Files::isRegularFile).toList()) before.put(output.relativize(p).toString(),Files.readAllBytes(p)); }
        VehicleAssetGenerator.main(new String[]{output.toString()});
        for(var f:before.entrySet()) check(Arrays.equals(f.getValue(),Files.readAllBytes(output.resolve(f.getKey()))),"Deterministic file "+f.getKey());
        for(VehicleType type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type);var atlas=VehicleAtlas.layout(parts);var names=new HashSet<String>();
            check(atlas.equals(VehicleAtlas.layout(parts)),"Deterministic layout "+type);
            check(atlas.size()<=1024,"Texture budget "+type);
            for(var p:parts) {
                check(names.add(p.name()),"Unique part "+p.name());
                check(VehicleGeometry.MATERIALS.contains(p.material()),"Known material "+p.material());
                int axes=(p.restPitch()!=0?1:0)+(p.restYaw()!=0?1:0)+(p.restRoll()!=0?1:0);
                check(axes<=1,"Portable item rotation "+p.name());
                for(int i=0;i<p.boxes().size();i++) {
                    var b=p.boxes().get(i);check(b.w()>0 && b.h()>0 && b.d()>0,"Positive cube");
                    var uv=atlas.island(p.name(),i);check(uv.u()>=2 && uv.v()>=2,"Gutters");
                    check(uv.u()+uv.width()<=atlas.size() && uv.v()+uv.height()<=atlas.size(),"Packed bounds");
                }
            }
            var islands=new ArrayList<>(atlas.islands().values());
            for(int i=0;i<islands.size();i++) for(int j=i+1;j<islands.size();j++) {
                var a=islands.get(i);var b=islands.get(j);
                check(a.u()+a.width()<=b.u() || b.u()+b.width()<=a.u() || a.v()+a.height()<=b.v() || b.v()+b.height()<=a.v(),"Disjoint UVs "+type);
            }
            var image=ImageIO.read(output.resolve("assets/harvester/textures/vehicle/atlas_"+type.id+".png").toFile());
            check(image!=null && image.getWidth()==atlas.size(),"Runtime atlas "+type);
            for(int seat=0;seat<type.seats;seat++) {
                var animation=new PassengerAnimation();
                for(int tick=0;tick<80;tick++) finitePose(animation.update(rider(tick,type,seat,seat,Math.sin(tick*.15),tick<40?1:-1,Math.sin(tick*.06)*type.speed)));
                finitePose(animation.update(rider(Double.NaN,type,seat,seat,Double.NaN,Double.POSITIVE_INFINITY,-100)));
            }
        }
        var a=new PassengerAnimation();var b=new PassengerAnimation();
        a.update(rider(0,VehicleType.PICKUP,0,1,0,0,0));b.update(rider(0,VehicleType.PICKUP,0,1,0,0,0));
        var whole=a.update(rider(1,VehicleType.PICKUP,0,1,1,1,0));b.update(rider(.5,VehicleType.PICKUP,0,1,1,1,0));
        var halves=b.update(rider(1,VehicleType.PICKUP,0,1,1,1,0));
        check(Math.abs(whole.rightArm().pitch()-halves.rightArm().pitch())<1e-6,"Rider subdivision");
        check(Math.abs(whole.weight()-halves.weight())<1e-6,"Entry subdivision");
        var pillion=new PassengerAnimation();var calm=pillion.update(rider(0,VehicleType.MOTORCYCLE,1,1,0,0,0));
        var turn=pillion.update(rider(1,VehicleType.MOTORCYCLE,1,1,1,1,.3));
        check(calm.rightArm().equals(turn.rightArm()) && calm.leftArm().equals(turn.leftArm()),"Pillion does not steer");
        check(PassengerPose.keepVanillaArms(true,0) && PassengerPose.keepVanillaArms(false,.5f),"Item/attack priority");
        check(a.update(rider(2,VehicleType.PLANE,0,2,0,0,0)).weight()==0,"Seat entry reset");
        var present=new VehiclePresentation();VehiclePresentation.Frame frame=null;
        for(int i=0;i<200;i++) frame=present.update(new VehiclePresentation.Input(i,VehicleType.PICKUP,i<40,false,i<50,true,i<40?.2:0,i<40?1:0,0,0,0,1,1));
        check(frame!=null && frame.rpm()<1e-5,"Engine settles after shutdown");
        System.out.println("Visual smoke PASS: "+VehicleType.values().length+" variants, "+checks+" assertions. No client/shader validation implied.");
    }
}
