package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.*;
import java.util.*;

/** Dependency-free executable assertions, not JUnit cases or Minecraft runtime testing. */
public final class FeedbackSmoke {
    private static int checks;
    private static void check(boolean value, String context) {
        checks++; if (!value) throw new AssertionError(context + " (assertion " + checks + ")");
    }
    public static void main(String[] args) {
        for (int keys = 0; keys <= 255; keys++) for (boolean powered : new boolean[]{false, true})
            for (boolean ground : new boolean[]{false, true}) {
                int load = TruckFeedback.driveLoad(keys, powered, ground);
                boolean expected = keys <= 63 && powered && ground && (keys & 16) == 0 && ((keys & 1) != 0 ^ (keys & 2) != 0);
                check(load == (expected ? 100 : 0), "server-derived load");
            }
        for (double load : new double[]{0, .5, 1, -1, Double.NaN, Double.POSITIVE_INFINITY})
            for (double speed : new double[]{0, -.1, .28, Double.NaN, 1e300}) {
                var envelope = new EngineFeedback(); float previous = 0;
                for (int tick = 0; tick < 150; tick++) {
                    boolean on = tick < 100; var f = envelope.update(on, load, speed);
                    check(Float.isFinite(f.volume()) && f.volume() >= 0 && f.volume() <= .47001, "volume bound");
                    check(Float.isFinite(f.pitch()) && f.pitch() >= .7799 && f.pitch() <= 1.52001, "pitch bound");
                    check(Math.abs(f.volume() - previous) <= .13, "smooth envelope"); previous = f.volume();
                }
                check(previous == 0, "loop stops after fade");
            }
        for (int yaw = -720; yaw <= 720; yaw++) {
            var p = TruckFeedback.exhaustOffset(yaw); double a = Math.toRadians(yaw);
            check(Math.abs(p.x() * Math.cos(a) + p.z() * Math.sin(a) - TruckFeedback.EXHAUST_X) < 1e-6, "outlet x");
            check(Math.abs(-p.x() * Math.sin(a) + p.z() * Math.cos(a) - TruckFeedback.EXHAUST_Z) < 1e-6, "outlet z");
            check(p.y() == TruckFeedback.EXHAUST_Y, "outlet y");
        }
        for (int id : new int[]{Integer.MIN_VALUE, -1, 0, 100, Integer.MAX_VALUE}) {
            int idle = 0, load = 0;
            for (int tick = 0; tick < 200; tick++) {
                check(!TruckFeedback.emitExhaust(false, 1, tick, id), "no stopped exhaust");
                if (TruckFeedback.emitExhaust(true, 0, tick, id)) idle++;
                if (TruckFeedback.emitExhaust(true, 1, tick, id)) load++;
            }
            check(idle == 25 && load == 50, "particle budget");
        }
        for (int population = 0; population <= 200; population++) {
            var candidates = new ArrayList<TruckFeedback.Candidate>();
            for (int id = 0; id < population; id++) candidates.add(new TruckFeedback.Candidate(id, id, id == population - 1, false));
            if (population > 0) candidates.add(candidates.getFirst());
            var selected = TruckFeedback.selectVoices(candidates);
            check(selected.size() == Math.min(population, 8), "voice cap including duplicate IDs");
            check(new HashSet<>(selected).size() == selected.size(), "one loop per ID");
            if (population > 0) check(selected.getFirst() == population - 1, "own engine priority");
        }
        check(TruckFeedback.selectVoices(List.of(new TruckFeedback.Candidate(1, Double.NaN, true, true))).isEmpty(), "invalid distance");
        check(TruckFeedback.observedSpeed(100, 0) == 0, "teleport filter");
        check(Math.abs(TruckFeedback.observedSpeed(.3, .4) - .5) < 1e-9, "horizontal speed");
        System.out.println("FEEDBACK_SMOKE_PASS assertions=" + checks);
    }
}
