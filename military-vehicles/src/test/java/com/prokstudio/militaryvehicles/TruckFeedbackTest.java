package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TruckFeedbackTest {
    @Test void demandRequiresPoweredGroundedUnopposedThrottle() {
        for (int keys = 0; keys <= 63; keys++) {
            assertEquals(0, TruckFeedback.driveLoad(keys, false, true));
            assertEquals(0, TruckFeedback.driveLoad(keys, true, false));
            int expected = (keys & 16) == 0 && ((keys & 1) != 0 ^ (keys & 2) != 0) ? 100 : 0;
            assertEquals(expected, TruckFeedback.driveLoad(keys, true, true));
        }
        assertEquals(0, TruckFeedback.driveLoad(65, true, true));
    }
    @Test void priorityIsBoundedStableAndDeduplicated() {
        var candidates = new ArrayList<TruckFeedback.Candidate>();
        for (int id = 20; id >= 0; id--) candidates.add(new TruckFeedback.Candidate(id, 10, false, false));
        candidates.add(candidates.getLast());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), TruckFeedback.selectVoices(candidates));
        assertEquals(List.of(1, 2), TruckFeedback.selectVoices(List.of(
            new TruckFeedback.Candidate(2, 9, false, false), new TruckFeedback.Candidate(1, 12, false, true))));
    }
    @Test void ownTruckOutranksNearerLoops() {
        var candidates = new ArrayList<TruckFeedback.Candidate>();
        for (int id = 0; id < 20; id++) candidates.add(new TruckFeedback.Candidate(id, 1, false, true));
        candidates.add(new TruckFeedback.Candidate(100, 100, true, false));
        var selected = TruckFeedback.selectVoices(candidates);
        assertEquals(8, selected.size()); assertEquals(100, selected.getFirst().intValue());
    }
    @Test void invalidAndDistantCandidatesAreIgnored() {
        var candidates = new ArrayList<TruckFeedback.Candidate>(); int id = 0;
        for (double distance : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, 1024.01})
            candidates.add(new TruckFeedback.Candidate(id++, distance, true, true));
        candidates.add(new TruckFeedback.Candidate(99, 1024, false, false));
        assertEquals(List.of(99), TruckFeedback.selectVoices(candidates));
        assertTrue(TruckFeedback.selectVoices(List.of()).isEmpty());
    }
    @Test void interpolationRejectsTeleports() {
        assertEquals(.5, TruckFeedback.observedSpeed(.3, .4), 1e-9);
        for (double dx : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 2, -2, 1e300})
            assertEquals(0, TruckFeedback.observedSpeed(dx, 0));
    }
    @Test void exhaustRatesAndOffGate() {
        for (int id : new int[]{Integer.MIN_VALUE, -1, 0, 7, Integer.MAX_VALUE}) {
            int idle = 0, load = 0;
            for (int tick = 0; tick < 200; tick++) {
                assertFalse(TruckFeedback.emitExhaust(false, 1, tick, id));
                if (TruckFeedback.emitExhaust(true, 0, tick, id)) idle++;
                if (TruckFeedback.emitExhaust(true, 1, tick, id)) load++;
            }
            assertEquals(25, idle); assertEquals(50, load);
        }
        assertFalse(TruckFeedback.emitExhaust(true, 1, -1, 0));
        assertEquals(TruckFeedback.emitExhaust(true, 0, Long.MAX_VALUE, -1), TruckFeedback.emitExhaust(true, Double.NaN, Long.MAX_VALUE, -1));
    }
    @Test void exhaustAnchorMatchesGeometryAtAllYaws() {
        var outlet = TruckGeometry.create().stream().filter(p -> p.name().equals("exhaust_stack")).findFirst().orElseThrow().boxes().getLast();
        assertTrue(TruckFeedback.EXHAUST_X * 16 > outlet.x() && TruckFeedback.EXHAUST_X * 16 < outlet.x() + outlet.w());
        assertTrue(TruckFeedback.EXHAUST_Y * 16 > outlet.y() && TruckFeedback.EXHAUST_Y * 16 < outlet.y() + outlet.h());
        assertEquals(outlet.z() - .5, TruckFeedback.EXHAUST_Z * 16, 1e-9);
        for (int yaw = -720; yaw <= 720; yaw++) {
            var p = TruckFeedback.exhaustOffset(yaw); double a = Math.toRadians(yaw);
            assertEquals(TruckFeedback.EXHAUST_X, p.x() * Math.cos(a) + p.z() * Math.sin(a), 1e-6);
            assertEquals(TruckFeedback.EXHAUST_Z, -p.x() * Math.sin(a) + p.z() * Math.cos(a), 1e-6);
            assertEquals(TruckFeedback.EXHAUST_Y, p.y(), 1e-9);
        }
        assertThrows(IllegalArgumentException.class, () -> TruckFeedback.exhaustOffset(Float.NaN));
    }
}
