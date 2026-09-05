package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class VehicleGeometryTest {
    @Test void everyVariantHasOwnDetailedFiniteGeometry() {
        for(VehicleType type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type);
            assertTrue(parts.stream().mapToInt(p->p.boxes().size()).sum()>=20,type.id);
            var names=new HashSet<String>();
            for(var part:parts) {
                assertTrue(names.add(part.name()),type.id+":"+part.name());
                assertTrue(VehicleGeometry.MATERIALS.contains(part.material()));
                for(var box:part.boxes()) {
                    assertTrue(Float.isFinite(box.x())&&Float.isFinite(box.y())&&Float.isFinite(box.z()));
                    assertTrue(box.w()>0&&box.h()>0&&box.d()>0);
                }
            }
        }
    }
    @Test void noVanillaGeometryOrTextureIdentifiers() {
        for(VehicleType type:VehicleType.values()) for(var part:VehicleGeometry.create(type)) {
            assertFalse(part.material().contains(":"));
            assertFalse(part.boxes().isEmpty());
        }
    }
}
