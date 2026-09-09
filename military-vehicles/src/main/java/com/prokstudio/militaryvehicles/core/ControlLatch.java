package com.prokstudio.militaryvehicles.core;
/** Rate limit, dead-man timeout and press-edge intent. No client position, velocity or hit result. */
public final class ControlLatch {
    public static final int FORWARD=1,BACK=2,LEFT=4,RIGHT=8,BRAKE=16,ENGINE=32,MASK=63,TIMEOUT=10;
    private long lastTick=Long.MIN_VALUE;
    private int keys;
    private boolean toggle;
    private boolean awaitRelease;
    public boolean accept(long tick,int rawKeys) {
        if ((rawKeys & ~MASK)!=0 || tick<0 || tick<=lastTick) return false;
        // A gap or a cleared seat drops the old key snapshot, so a key that is still down is not a new press.
        if (!fresh(tick)) { awaitRelease|=lastTick!=Long.MIN_VALUE; keys=0; toggle=false; }
        if ((rawKeys & ENGINE)==0) awaitRelease=false;
        else if (!awaitRelease && (keys & ENGINE)==0) toggle=true;
        keys=rawKeys; lastTick=tick; return true;
    }
    public boolean fresh(long tick) { return lastTick!=Long.MIN_VALUE && tick>=lastTick && tick-lastTick<=TIMEOUT; }
    public int keys(long tick) { return fresh(tick)?keys:BRAKE; }
    public boolean consumeToggle(long tick) { boolean result=fresh(tick)&&toggle;toggle=false;return result; }
    public void reset() { keys=0;toggle=false;lastTick=Long.MIN_VALUE;awaitRelease=true; }
    /** True while a held engine key must be released before it can count as a new press. */
    public boolean awaitingRelease() { return awaitRelease; }
}
