package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleReleasePolishTest {
    @Test void scaleAppliedExactlyOnce() {
        for(var t:VehicleType.values()) {
            assertEquals(t.blueprintWidth*1.125,t.width,1e-6);assertEquals(t.blueprintHeight*1.125,t.height,1e-6);
            var s=VehicleGeometry.seat(t,0);var p=VehicleRig.transform(t,VehicleRig.Pose.ZERO,s.x()/16,s.top()/16,s.z()/16);
            assertEquals(s.top()/16*1.125,p.y(),1e-9);assertEquals(s.z()/16*1.125,p.z(),1e-9);assertEquals(.7040625,VehicleRig.PLAYER_HIP,0);
        }
    }
    @Test void hydraulicEndpointsStayAttached() {
        for(int i=0;i<=400;i++) {
            double lift=i/100.0;var h=VehicleMechanics.hydraulic(true,lift,0,0);double pitch=h.deltaPitch()+Math.PI/4;
            assertEquals(8+lift,16-Math.sin(pitch)*h.length(),1e-9);assertEquals(26,18+Math.cos(pitch)*h.length(),1e-9);
            assertTrue(h.pistonScale()>0 && h.pistonScale()<=1.01);
        }
    }
    @Test void wipersReturnToPark() {
        var a=new VehicleMechanics();a.update(0,VehicleType.PICKUP,true,true,1,0,0);
        assertTrue(a.update(5,VehicleType.PICKUP,true,true,1,0,0).wiper()<1.15);VehicleMechanics.Frame f=null;
        for(int i=6;i<100;i++) f=a.update(i,VehicleType.PICKUP,false,false,0,0,0);
        assertNotNull(f);assertEquals(1.15,f.wiper(),1e-6);assertEquals(1.15,a.update(101,VehicleType.BOAT,false,false,0,0,0).wiper(),1e-6);
    }
    @Test void signalsUseActualDirection() {
        var a=new VehicleMechanics();var brake=a.update(0,VehicleType.PICKUP,false,true,1,-1,.2);
        assertTrue(brake.brakeLamp());assertFalse(brake.reverseLamp());var reverse=a.update(1,VehicleType.PICKUP,false,true,1,-1,-.2);
        assertFalse(reverse.brakeLamp());assertTrue(reverse.reverseLamp());var parked=a.update(2,VehicleType.PICKUP,false,false,0,0,0);
        assertFalse(parked.brakeLamp());assertFalse(parked.reverseLamp());
    }
    @Test void audioEnvelopeSettlesAndIsFinite() {
        for(var t:VehicleType.values()) {
            var a=new VehicleSoundEnvelope();var b=new VehicleSoundEnvelope();var whole=a.update(t,1,true,1,0,true,.65);b.update(t,.5,true,1,0,true,.65);var halves=b.update(t,.5,true,1,0,true,.65);
            assertEquals(whole.volume(),halves.volume(),1e-6);assertEquals(whole.rpm(),halves.rpm(),1e-6);VehicleSoundEnvelope.Frame f=whole;
            for(int i=0;i<250;i++) f=a.update(t,1,false,0,0,false,.65);assertTrue(f.volume()<1e-8 && f.rpm()<1e-8);
            f=a.update(t,Double.NaN,true,Double.NaN,Double.POSITIVE_INFINITY,false,Double.NaN);assertTrue(Float.isFinite(f.pitch()+f.volume()+f.rpm()));
        }
    }
    @Test void everyFamilyHasMechanicalRoots() {
        for(var t:VehicleType.values()) {
            var parts=VehicleGeometry.create(t);String root=switch(t.family) {case COMBINE -> "header_chassis_anchor_1";case DOZER -> "blade_anchor_1";case PICKUP -> "steering_pedestal";case MOTORCYCLE -> "front_yoke";case BOAT -> "outboard_gearcase";case PLANE -> "propeller_shaft";case HELICOPTER -> "tail_rotor_gearbox";case DRONE -> "radial_boom_1_1";};
            assertTrue(parts.stream().anyMatch(p->p.name().equals(root)),t.id);assertTrue(VehicleMechanics.renderRadius(t,parts)>t.width/2);assertTrue(parts.stream().mapToInt(p->p.boxes().size()).sum()<900);
            if(t.family==VehicleType.Family.DOZER) {
                var rods=parts.stream().filter(p->p.name().equals("metal")).flatMap(p->p.boxes().stream()).filter(b->b.y()==8 && b.z()==10 && b.d()==15).toList();
                assertEquals(2,rods.size());assertTrue(rods.stream().allMatch(b->b.y()+b.h()>10.05),"Blade rods must clear the chassis top plane");
                var belts=parts.stream().filter(p->p.name().equals("rubber")).flatMap(p->p.boxes().stream()).filter(b->b.h()==3 && b.d()==36).toList();
                assertEquals(4,belts.size());assertTrue(belts.stream().allMatch(b->b.w()<5.9),"Moving track edges must clear the rubber side plane");
            }
            if(t.family==VehicleType.Family.MOTORCYCLE) for(int i=0;i<2;i++) {
                int seat=i;var bar=parts.stream().filter(p->p.name().equals("footrest_crossbar_"+seat)).findFirst().orElseThrow().boxes().getFirst();
                assertTrue(bar.y()+bar.h()<VehicleGeometry.seat(t,i).top()-3.1,"Footrest must not share the mudguard/engine top plane");
            }
        }
    }
}
