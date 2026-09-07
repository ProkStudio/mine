import com.harvester.vehicle.*;
import java.nio.file.*;
import java.util.*;

/** Offline contracts and editable-mesh export, not a Minecraft client run. */
public final class VehiclePolishSmoke {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?"build/polish-meshes":args[0]);Files.createDirectories(output);
        for(VehicleType type:VehicleType.values()) {
            check(Math.abs(type.width-type.blueprintWidth*1.125)<1e-6,"Collision width scale");check(Math.abs(type.height-type.blueprintHeight*1.125)<1e-6,"Collision height scale");
            var parts=VehicleGeometry.create(type);var atlas=VehicleAtlas.layout(parts);check(parts.equals(VehicleGeometry.create(type)),"Deterministic blueprint: "+type);
            int cubes=0;Set<String> names=new HashSet<>();
            for(var p:parts) {
                check(names.add(p.name()),"Duplicate name: "+p.name());check(!p.boxes().isEmpty(),"Empty assembly");check(VehicleGeometry.MATERIALS.contains(p.material()),"Known material");
                for(var b:p.boxes()) { cubes++;check(Float.isFinite(b.x()+b.y()+b.z()+b.w()+b.h()+b.d()) && b.w()>0 && b.h()>0 && b.d()>0,"Finite positive geometry"); }
            }
            check(cubes<900,"Cuboid budget");check(atlas.size()<=1024,"Atlas budget");var islands=new ArrayList<>(atlas.islands().values());
            for(int i=0;i<islands.size();i++) for(int j=i+1;j<islands.size();j++) {
                var a=islands.get(i);var b=islands.get(j);
                check(a.u()+a.width()+4<=b.u() || b.u()+b.width()+4<=a.u() || a.v()+a.height()+4<=b.v() || b.v()+b.height()+4<=a.v(),"UV gutters overlap: "+type);
            }
            check(Double.isFinite(VehicleMechanics.renderRadius(type,parts)),"Finite frustum envelope");
            check(!names.stream().anyMatch(n->n.startsWith("header_ram_") || n.startsWith("blade_ram_")),"Legacy disconnected hydraulics removed");
            switch(type.family) {
                case COMBINE -> { check(names.contains("reel_spokes"),"Reel spokes");check(names.contains("header_bearing_-1"),"Header bearings"); }
                case DOZER -> check(names.contains("blade_clevis_1"),"Blade joints");
                case PICKUP -> check(names.contains("mirror_bracket_1") && names.contains("driveshaft"),"Truck attachments");
                case MOTORCYCLE -> check(names.contains("front_yoke") && names.contains("footrest_crossbar_1"),"Bike attachment paths");
                case BOAT -> check(names.contains("outboard_gearcase") && names.contains("boarding_ladder_1"),"Boat drivetrain and ladder");
                case PLANE -> check(names.contains("propeller_shaft") && names.contains("wing_strut_socket_1"),"Plane attachments");
                case HELICOPTER -> check(names.contains("tail_rotor_gearbox") && names.contains("seat_pedestal"),"Helicopter attachments");
                case DRONE -> {
                    for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1}) {
                        var p=parts.stream().filter(v->v.name().equals("radial_boom_"+x+"_"+z)).findFirst().orElseThrow();
                        double endpoint=z*type.blueprintWidth*6*Math.sqrt(2),angle=Math.toRadians(p.restYaw());
                        check(Math.abs(endpoint*Math.sin(angle)-x*type.blueprintWidth*6)<1e-5,"Boom touches motor X");check(Math.abs(endpoint*Math.cos(angle)-z*type.blueprintWidth*6)<1e-5,"Boom touches motor Z");
                    }
                }
            }
            export(output,type,parts);System.out.println(type.id+": "+cubes+" cuboids; "+parts.size()+" parts; atlas="+atlas.size());
        }
        for(int i=0;i<=400;i++) {
            double lift=i/100.0;var h=VehicleMechanics.hydraulic(true,lift,0,0);double pitch=h.deltaPitch()+Math.PI/4;
            check(Math.abs(16-Math.sin(pitch)*h.length()-(8+lift))<1e-9,"Header endpoint Y");check(Math.abs(18+Math.cos(pitch)*h.length()-26)<1e-9,"Header endpoint Z");check(h.pistonScale()>0 && h.pistonScale()<=1.01,"Piston remains inside barrel");
        }
        var mechanics=new VehicleMechanics();mechanics.update(0,VehicleType.PICKUP,true,true,1,1,.2);var wiping=mechanics.update(4,VehicleType.PICKUP,true,true,1,1,.2);check(wiping.wiper()<1.15,"Wiper sweeps in rain");
        VehicleMechanics.Frame parked=null;for(int i=5;i<100;i++) parked=mechanics.update(i,VehicleType.PICKUP,false,false,0,0,0);
        check(parked!=null && Math.abs(parked.wiper()-1.15)<1e-6,"Wiper returns to park without teleport");check(!parked.brakeLamp() && !parked.reverseLamp(),"Parked lamps off");
        for(VehicleType t:VehicleType.values()) {
            var sound=new VehicleSoundEnvelope();VehicleSoundEnvelope.Frame f=null;for(int i=0;i<100;i++) f=sound.update(t,1,true,1,0,true,.65);
            check(f.volume()>.35 && f.rpm()>.6,"Loaded stationary engine reacts to throttle");for(int i=0;i<250;i++) f=sound.update(t,1,false,0,0,false,.65);check(f.volume()<1e-8 && f.rpm()<1e-8,"Shutdown settles");
            f=sound.update(t,Double.NaN,true,Double.NaN,Double.POSITIVE_INFINITY,false,Double.NaN);check(Float.isFinite(f.volume()+f.pitch()+f.rpm()),"Invalid audio input bounded");
        }
        System.out.println("Polish smoke PASS: "+checks+" assertions. Offline contracts only; no Minecraft launch or listening test.");
    }
    private static void export(Path output,VehicleType type,List<VehicleGeometry.Part> parts) throws Exception {
        StringBuilder s=new StringBuilder("{\"id\":\"").append(type.id).append("\",\"title\":\"").append(type.displayName).append("\",\"scale\":").append(VehicleType.MODEL_SCALE).append(",\"parts\":[");boolean first=true;
        for(var p:parts) {
            if(!first) s.append(',');first=false;s.append("{\"name\":\"").append(p.name()).append("\",\"material\":\"").append(p.material()).append("\",\"color\":").append(VehicleGeometry.materialColor(p.material(),type));
            s.append(",\"pivot\":[").append(p.px()).append(',').append(p.py()).append(',').append(p.pz()).append("],\"rest\":[").append(p.restPitch()).append(',').append(p.restYaw()).append(',').append(p.restRoll()).append("],\"axis\":\"").append(p.axis()).append("\",\"boxes\":[");
            boolean fb=true;for(var b:p.boxes()) { if(!fb) s.append(',');fb=false;s.append('[').append(b.x()).append(',').append(b.y()).append(',').append(b.z()).append(',').append(b.w()).append(',').append(b.h()).append(',').append(b.d()).append(']'); }s.append("]}");
        }
        Files.writeString(output.resolve(type.id+".json"),s.append("]}\n").toString());
    }
}
