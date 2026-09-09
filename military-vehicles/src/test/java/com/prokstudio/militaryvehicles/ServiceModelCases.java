package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.ExpansionGeometry;
import com.prokstudio.militaryvehicles.core.TruckGeometry;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;
import com.prokstudio.militaryvehicles.core.VehicleKind;
import java.util.*;

/** Standalone checks for the service-body crew kit: presence, collider fit, clearances and an untouched donor chassis. */
public final class ServiceModelCases {
    private static final List<String> SHARED=List.of("beacon_mount","warning_beacon","extinguisher_bracket",
        "fire_extinguisher","supply_rack","jerry_cans");
    private static final List<String> SIDED=List.of("crew_step_","wheel_chock_","work_lamp_housing_","work_lamp_",
        "grab_handle_","mud_guard_mid_");
    private static final List<String> DROPPED=List.of("bed_sides","bed_top_rails","tailgate_hinges","spare_tire","spare_hub");
    private static final float EPS=1e-4f;
    private int checks;

    private static List<String> extras(VehicleKind kind) {
        return switch(kind) {
            case TANKER->List.of("grounding_reel","grounding_cable","hazard_chevrons_rear");
            case WORKSHOP->List.of("generator_set","generator_vents","generator_exhaust","hazard_chevrons_rear");
            case RECOVERY->List.of("chain_locker","snatch_blocks","light_bar_mount","light_bar");
            default->throw new IllegalArgumentException("not a service body: "+kind);
        };
    }
    private static Set<String> shared() {
        Set<String> names=new LinkedHashSet<>(SHARED);
        for(String base:SIDED) { names.add(base+"-1"); names.add(base+"1"); }
        return names;
    }
    private static Set<String> kit(VehicleKind kind) {
        Set<String> names=shared();
        names.addAll(extras(kind));
        return names;
    }
    private void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    private static float[] world(Part p,Box b) {
        return new float[]{p.x()+b.x(),p.y()+b.y(),p.z()+b.z(),
            p.x()+b.x()+b.w(),p.y()+b.y()+b.h(),p.z()+b.z()+b.d()};
    }
    private static boolean overlaps(float[] a,float[] c) {
        return Math.min(a[3],c[3])-Math.max(a[0],c[0])>EPS
            &&Math.min(a[4],c[4])-Math.max(a[1],c[1])>EPS
            &&Math.min(a[5],c[5])-Math.max(a[2],c[2])>EPS;
    }
    /** Radius swept by a spinning part around its own origin, measured in the side plane. */
    private static float rollingRadius(Part p) {
        float r=0;
        for(Box b:p.boxes()) for(float y:new float[]{b.y(),b.y()+b.h()}) for(float z:new float[]{b.z(),b.z()+b.d()})
            r=Math.max(r,(float)Math.hypot(y,z));
        return r;
    }
    private static float clamp(float value,float low,float high) { return value<low?low:value>high?high:value; }

    void everyServiceBodyCarriesTheCrewKit() {
        for(VehicleKind kind:VehicleKind.values()) {
            if(!kind.support()&&!kind.tracked()) continue;
            Set<String> names=new LinkedHashSet<>();
            for(Part part:ExpansionGeometry.create(kind)) check(names.add(part.name()),"duplicate part "+part.name()+" on "+kind);
            if(kind.support()) {
                for(String required:kit(kind)) check(names.contains(required),kind+" is missing "+required);
                check(names.contains("hazard_chevrons_rear")!=(kind==VehicleKind.RECOVERY),
                    "rear chevrons must not fight the recovery blade");
                for(String borrowed:List.of("sprocket_teeth_-1","idler_rim_1","return_roller_-1_0","track_scraper_1"))
                    check(!names.contains(borrowed),kind+" must not borrow tracked running gear: "+borrowed);
                for(String dropped:DROPPED) check(!names.contains(dropped),kind+" still carries the cargo bed part "+dropped);
            } else for(String crew:shared()) check(!names.contains(crew),kind+" must not carry the wheeled service kit: "+crew);
        }
    }

    void crewKitStaysInsideTheCollider() {
        for(VehicleKind kind:VehicleKind.values()) {
            if(!kind.support()) continue;
            for(Part part:ExpansionGeometry.create(kind)) {
                if(part.wheel()) continue;
                for(Box b:part.boxes()) {
                    float[] o=world(part,b);
                    for(float x:new float[]{o[0],o[3]}) for(float y:new float[]{o[1],o[4]}) for(float z:new float[]{o[2],o[5]}) {
                        check(y>=-1e-5f,part.name()+" sinks under the ground on "+kind);
                        check(y/16<=kind.height+1e-5f,part.name()+" pokes through the roof of "+kind);
                        check(Math.hypot(x,z)/16<=kind.width/2+1e-5f,part.name()+" pokes out of the side of "+kind);
                    }
                }
            }
        }
    }

    void crewKitNeverGrowsIntoTheChassis() {
        for(VehicleKind kind:VehicleKind.values()) {
            if(!kind.support()) continue;
            Set<String> kit=kit(kind);
            List<float[]> own=new ArrayList<>(),chassis=new ArrayList<>();
            for(Part part:ExpansionGeometry.create(kind)) {
                boolean mine=kit.contains(part.name());
                if(part.wheel()) { check(!mine,"the crew kit must not spin with the wheels"); continue; }
                for(Box b:part.boxes()) (mine?own:chassis).add(world(part,b));
            }
            check(own.size()>=20,kind+" lost crew kit boxes: "+own.size());
            for(int i=0;i<own.size();i++) {
                for(float[] other:chassis) check(!overlaps(own.get(i),other),kind+" crew kit grows into the chassis");
                for(int j=i+1;j<own.size();j++) check(!overlaps(own.get(i),own.get(j)),kind+" crew kit boxes overlap each other");
            }
        }
    }

    void crewKitClearsTheRollingWheels() {
        for(VehicleKind kind:VehicleKind.values()) {
            if(!kind.support()) continue;
            Set<String> kit=kit(kind);
            List<float[]> own=new ArrayList<>();
            List<Part> rollers=new ArrayList<>();
            for(Part part:ExpansionGeometry.create(kind)) {
                if(part.wheel()) rollers.add(part);
                else if(kit.contains(part.name())) for(Box b:part.boxes()) own.add(world(part,b));
            }
            check(!rollers.isEmpty(),kind+" has no wheels");
            for(Part roller:rollers) {
                float radius=rollingRadius(roller),low=Float.MAX_VALUE,high=-Float.MAX_VALUE;
                for(Box b:roller.boxes()) { low=Math.min(low,roller.x()+b.x()); high=Math.max(high,roller.x()+b.x()+b.w()); }
                for(float[] box:own) {
                    if(box[3]<=low+EPS||box[0]>=high-EPS) continue;
                    float y=clamp(roller.y(),box[1],box[4]),z=clamp(roller.z(),box[2],box[5]);
                    check(Math.hypot(y-roller.y(),z-roller.z())>=radius-1e-3f,
                        kind+" crew kit box fouls the swept circle of "+roller.name());
                }
            }
        }
    }

    void serviceBodiesKeepTheDonorTruckUnchanged() {
        Map<String,Part> truck=new LinkedHashMap<>();
        for(Part part:TruckGeometry.create()) truck.put(part.name(),part);
        for(VehicleKind kind:VehicleKind.values()) {
            if(!kind.support()) continue;
            int tires=0;
            for(Part part:ExpansionGeometry.create(kind)) {
                if(part.wheel()&&part.material().equals("rubber")) {
                    tires++;
                    check(part.y()==kind.wheelRadius,"tire "+part.name()+" left its rolling radius on "+kind);
                }
                Part old=truck.get(part.name());
                if(old==null||part.wheel()) continue;
                check(old.material().equals(part.material())&&old.boxes().equals(part.boxes()),
                    "the service body edited the original "+part.name());
            }
            check(tires==kind.wheels,kind+" carries "+tires+" tires instead of "+kind.wheels);
        }
    }

    public int runAll() {
        everyServiceBodyCarriesTheCrewKit();
        crewKitStaysInsideTheCollider();
        crewKitNeverGrowsIntoTheChassis();
        crewKitClearsTheRollingWheels();
        serviceBodiesKeepTheDonorTruckUnchanged();
        return checks;
    }
}
