package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.EngineFeedback;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EngineFeedbackTest {
    @Test void stoppedIsSilent() {
        var e = new EngineFeedback();
        for (int i = 0; i < 100; i++) assertEquals(0, e.update(false, 1, .28).volume());
    }
    @Test void startAndStopHaveBoundedRamps() {
        var e = new EngineFeedback(); float previous = 0;
        for (int i = 0; i < 100; i++) {
            var f = e.update(true, 1, .28);
            assertTrue(f.volume() >= previous && f.volume() - previous <= .08);
            assertTrue(f.volume() <= .47001 && f.pitch() >= .78f && f.pitch() <= 1.52001); previous = f.volume();
        }
        for (int i = 0; i < 40; i++) {
            float volume = e.update(false, 1, .28).volume();
            assertTrue(volume <= previous && previous - volume <= .13); previous = volume;
        }
        assertEquals(0, previous);
    }
    @Test void loadChangesPitchAtStandstill() {
        var idle = new EngineFeedback(); var load = new EngineFeedback();
        EngineFeedback.Frame a = null, b = null;
        for (int i = 0; i < 100; i++) { a = idle.update(true, 0, 0); b = load.update(true, 1, 0); }
        assertTrue(b.pitch() > a.pitch() + .4); assertTrue(b.volume() > a.volume());
    }
    @Test void reversingHasSameRoadResponse() {
        var forward = new EngineFeedback(); var reverse = new EngineFeedback();
        for (int i = 0; i < 100; i++) assertEquals(forward.update(true, 1, .1), reverse.update(true, 1, -.1));
    }
    @Test void invalidInputsRemainFinite() {
        var e = new EngineFeedback();
        for (double load : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -10, 1e300})
            for (double speed : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 1e300}) {
                var f = e.update(true, load, speed);
                assertTrue(Float.isFinite(f.volume()) && f.volume() >= 0 && f.volume() <= .47001);
                assertTrue(Float.isFinite(f.pitch()) && f.pitch() >= .78f && f.pitch() <= 1.52001);
            }
    }
    @Test void independentLoopsDoNotShareState() {
        var a = new EngineFeedback(); var b = new EngineFeedback();
        for (int i = 0; i < 100; i++) a.update(true, 1, .28);
        assertEquals(0, b.update(false, 0, 0).volume());
        assertTrue(a.update(true, 1, .28).volume() > .4);
    }
}
