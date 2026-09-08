package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FleetGeometryTest {
    @Test void rigsAreDistinctWithUniqueNamesAndKnownMaterials() {
        Set<List<TruckGeometry.Part>> rigs=new HashSet<>();
        for(var k:VehicleKind.values()) {
            var rig=VehicleGeometry.create(k);assertTrue(rig.size()>30);assertTrue(rigs.add(rig));
            Set<String> names=new HashSet<>();
            for(var p:rig) {assertTrue(names.add(p.name()));assertTrue(VehicleGeometry.MATERIALS.containsKey(p.material()));assertFalse(p.boxes().isEmpty());}
        }
        assertEquals(TruckGeometry.create(),VehicleGeometry.create(VehicleKind.TRUCK));
        assertEquals(13,VehicleGeometry.MATERIALS.size());
    }
    @Test void everyAxleHasMatchingHubsAndCorrectSteeringFlags() {
        for(var k:VehicleKind.values()) {
            var rig=VehicleGeometry.create(k);var tires=rig.stream().filter(p->p.wheel()&&p.material().equals("rubber")).toList();
            assertEquals(k.wheels,tires.size());assertEquals(k.steeringWheels,tires.stream().filter(TruckGeometry.Part::front).count());
            for(var tire:tires) {
                var hub=rig.stream().filter(p->p.wheel()&&p.material().equals("metal")&&p.x()==tire.x()&&p.y()==tire.y()&&p.z()==tire.z()).findFirst().orElseThrow();
                assertEquals(tire.front(),hub.front());assertEquals(k.wheelRadius,tire.y());
            }
        }
    }
    @Test void allStaticCornersFitHeightAndYawIndependentCollider() {
        for(var k:VehicleKind.values()) for(var p:VehicleGeometry.create(k)) for(var b:p.boxes()) {
            assertTrue(b.w()>0&&b.h()>0&&b.d()>0);
            for(float x:new float[]{p.x()+b.x(),p.x()+b.x()+b.w()}) for(float y:new float[]{p.y()+b.y(),p.y()+b.y()+b.h()}) for(float z:new float[]{p.z()+b.z(),p.z()+b.z()+b.d()}) {
                assertTrue(y>=-1e-5&&y/16<=k.height,k+" "+p.name());
                assertTrue(Math.hypot(x,z)/16<=k.width/2,k+" "+p.name());
            }
        }
    }
    @Test void newWheelsStayAboveGroundAndInsideBoundsWhenAnimated() {
        for(var k:List.of(VehicleKind.BUGGY,VehicleKind.CARRIER)) for(var p:VehicleGeometry.create(k)) if(p.wheel())
            for(int frame=0;frame<32;frame++) for(float steering:new float[]{-k.handling.steer(),0,k.handling.steer()}) for(var b:p.boxes())
                for(float x:new float[]{b.x(),b.x()+b.w()}) for(float y:new float[]{b.y(),b.y()+b.h()}) for(float z:new float[]{b.z(),b.z()+b.d()}) {
                    double spin=frame*Math.PI/16,angle=p.front()?-steering:0;
                    double yy=y*Math.cos(spin)-z*Math.sin(spin),zz=y*Math.sin(spin)+z*Math.cos(spin);
                    double xx=x*Math.cos(angle)+zz*Math.sin(angle),zzz=-x*Math.sin(angle)+zz*Math.cos(angle);
                    assertTrue(p.y()+yy>=-1e-5,k+" "+p.name());assertTrue(Math.hypot(p.x()+xx,p.z()+zzz)/16<=k.width/2,k+" "+p.name());
                }
    }
    @Test void newSeatCushionsAndControlsMatchActualAttachmentProfiles() {
        for(var k:List.of(VehicleKind.BUGGY,VehicleKind.CARRIER)) {
            var rig=VehicleGeometry.create(k);
            for(int i=0;i<k.seats.size();i++) {
                String name="seat_"+i;var seat=k.seats.get(i);var cushion=rig.stream().filter(p->p.name().equals(name)).findFirst().orElseThrow().boxes().getFirst();
                assertEquals(seat.x(),cushion.x()+cushion.w()/2,1e-5);assertEquals(seat.topY(),cushion.y()+cushion.h(),1e-5);assertEquals(seat.z(),cushion.z()+cushion.d()/2,1e-5);
            }
            var wheel=rig.stream().filter(p->p.name().equals("steering")).findFirst().orElseThrow();assertEquals(k.seats.getFirst().x(),wheel.x());assertTrue(wheel.z()>k.seats.getFirst().z());
        }
    }
}
