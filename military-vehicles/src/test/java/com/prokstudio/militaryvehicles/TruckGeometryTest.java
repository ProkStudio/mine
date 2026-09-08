package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TruckGeometryTest {
    @Test void allPartsHaveUniqueNamesAndKnownMaterials() { Set<String> names=new HashSet<>();for(var p:TruckGeometry.create()) { assertTrue(names.add(p.name()),p.name());assertTrue(TruckGeometry.MATERIALS.containsKey(p.material())); } }
    @Test void sixWheelsHaveMatchingHubsAndExactlyTwoSteeringWheels() {
        var parts=TruckGeometry.create();var wheels=parts.stream().filter(p->p.wheel()&&!p.name().startsWith("wheel_hub")).toList();assertEquals(6,wheels.size());assertEquals(2,wheels.stream().filter(TruckGeometry.Part::front).count());
        for(var w:wheels) { var h=parts.stream().filter(p->p.name().equals(w.name().replace("wheel_","wheel_hub_"))).findFirst().orElseThrow();assertEquals(w.x(),h.x());assertEquals(w.y(),h.y());assertEquals(w.z(),h.z());assertEquals(w.front(),h.front()); }
    }
    @Test void geometryFitsCollisionAtEveryYaw() {
        for(var p:TruckGeometry.create()) for(var b:p.boxes()) {
            for(float x:new float[]{p.x()+b.x(),p.x()+b.x()+b.w()}) for(float z:new float[]{p.z()+b.z(),p.z()+b.z()+b.d()}) assertTrue(Math.hypot(x,z)/16<=TruckSpec.WIDTH/2.0+.001,p.name());
            assertTrue(p.y()+b.y()>=0,p.name());assertTrue((p.y()+b.y()+b.h())/16<=TruckSpec.HEIGHT,p.name());
        }
    }
}
