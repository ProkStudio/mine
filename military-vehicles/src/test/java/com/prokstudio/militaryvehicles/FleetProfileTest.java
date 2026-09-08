package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FleetProfileTest {
    @Test void stableIdsAndCapacitiesCoverThreeDistinctRoles() {
        // Historical method name retained; exact registry coverage now includes all eight roles.
        assertEquals(Set.of("truck_6x6","scout_buggy","carrier_8x8","warden_tank","fuel_tanker","field_workshop","recovery_vehicle","bastion_howitzer"),new HashSet<>(Arrays.stream(VehicleKind.values()).map(k->k.id).toList()));
        assertEquals(5,Arrays.stream(VehicleKind.values()).map(k->k.soundId).distinct().count());
        assertEquals(2,VehicleKind.TRUCK.seats.size());assertEquals(2,VehicleKind.BUGGY.seats.size());assertEquals(6,VehicleKind.CARRIER.seats.size());
        assertEquals(27,VehicleKind.TRUCK.cargoSlots());assertEquals(9,VehicleKind.BUGGY.cargoSlots());assertEquals(18,VehicleKind.CARRIER.cargoSlots());
        assertEquals(1200,VehicleKind.BUGGY.tank);assertEquals(3200,VehicleKind.CARRIER.tank);
        assertEquals(120,VehicleKind.BUGGY.condition);assertEquals(360,VehicleKind.CARRIER.condition);
        assertTrue(VehicleKind.find("tank").isEmpty());assertThrows(IllegalArgumentException.class,()->VehicleKind.require("unknown"));
    }
    @Test void legacyTruckProfileMatchesExistingConstants() {
        var k=VehicleKind.TRUCK;
        assertEquals(TruckSpec.ID,k.id);assertEquals(TruckSpec.SLOTS,k.cargoSlots());assertEquals(TruckSpec.SEATS,k.seats.size());
        assertEquals(TruckSpec.TANK,k.tank);assertEquals(TruckSpec.CONDITION,k.condition);assertEquals(TruckSpec.WIDTH,k.width);assertEquals(TruckSpec.HEIGHT,k.height);
        assertEquals(TruckSpec.MAX_FORWARD,k.handling.forward());assertEquals(TruckSpec.MAX_REVERSE,k.handling.reverse());
        assertEquals(.010,k.handling.acceleration());assertEquals(.024,k.handling.counterBrake());assertEquals(.035,k.handling.brake());assertEquals(.006,k.handling.coast());
        assertEquals(.48f,k.handling.steer());assertEquals(2.1f,k.handling.yawRate());
        assertEquals(new VehicleKind.Seat(9,24,16),k.seats.getFirst());
    }
    @Test void handlingAndDurabilityExpressDifferentVehicleRoles() {
        assertTrue(VehicleKind.BUGGY.handling.forward()>VehicleKind.TRUCK.handling.forward());
        assertTrue(VehicleKind.TRUCK.handling.forward()>VehicleKind.CARRIER.handling.forward());
        assertTrue(VehicleKind.BUGGY.handling.acceleration()>VehicleKind.CARRIER.handling.acceleration());
        assertTrue(VehicleKind.BUGGY.handling.yawRate()>VehicleKind.CARRIER.handling.yawRate());
        for(var k:VehicleKind.values()) {
            assertTrue(k.handling.reverse()>0&&k.handling.reverse()<k.handling.forward());
            assertTrue(k.cargoRows>=1&&k.cargoRows<=3);assertEquals(k.cargoRows*9,k.cargoSlots());
            assertTrue(k.width>0&&k.height>0&&k.wheelRadius>0);
        }
    }
    @Test void seatLayoutsAreImmutableUniqueAndInsideBodyBounds() {
        for(var k:VehicleKind.values()) {
            assertEquals(k.seats.size(),new HashSet<>(k.seats).size());
            assertThrows(UnsupportedOperationException.class,()->k.seats.add(new VehicleKind.Seat(0,1,0)));
            for(var s:k.seats) { assertTrue(s.topY()>0&&s.topY()/16<k.height);assertTrue(Math.hypot(s.x(),s.z())/16<k.width/2); }
        }
    }
    @Test void exhaustAnchorsMatchTheirOwnRigAndRotateAroundYaw() {
        for(var k:VehicleKind.values()) {
            var stack=VehicleGeometry.create(k).stream().filter(p->p.name().equals("exhaust_stack")).findFirst().orElseThrow();
            var outlet=stack.boxes().stream().min(Comparator.comparingDouble(TruckGeometry.Box::z)).orElseThrow();
            assertEquals(stack.x()+outlet.x()+outlet.w()/2,k.exhaust.x(),1e-5);
            assertEquals(stack.y()+outlet.y()+outlet.h()/2,k.exhaust.y(),1e-5);
            assertEquals(stack.z()+outlet.z()-.5,k.exhaust.z(),1e-5);
            for(int yaw=-180;yaw<=180;yaw+=15) {
                var offset=TruckFeedback.exhaustOffset(k,yaw);double a=Math.toRadians(yaw);
                assertEquals((k.exhaust.x()*Math.cos(a)-k.exhaust.z()*Math.sin(a))/16,offset.x(),1e-7);
                assertEquals(k.exhaust.y()/16,offset.y(),1e-7);
                assertEquals((k.exhaust.x()*Math.sin(a)+k.exhaust.z()*Math.cos(a))/16,offset.z(),1e-7);
            }
        }
    }
}
