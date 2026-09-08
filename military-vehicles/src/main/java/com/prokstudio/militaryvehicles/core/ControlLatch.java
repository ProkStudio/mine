package com.prokstudio.militaryvehicles.core;
/** Rate limit and dead-man timeout. No client position, velocity or hit result. */
public final class ControlLatch {
    public static final int FORWARD=1,BACK=2,LEFT=4,RIGHT=8,BRAKE=16,ENGINE=32,MASK=63,TIMEOUT=10;
    private long lastTick=Long.MIN_VALUE;
    private int keys;
    private boolean toggle;
    public boolean accept(long tick,int rawKeys) {
        if ((rawKeys & ~MASK)!=0 || tick<0 || tick<=lastTick) return false;
        if (!fresh(tick)) { keys=0; toggle=false; }
        toggle |= (rawKeys & ENGINE)!=0 && (keys & ENGINE)==0;
        keys=rawKeys; lastTick=tick; return true;
    }
    public boolean fresh(long tick) { return lastTick!=Long.MIN_VALUE && tick>=lastTick && tick-lastTick<=TIMEOUT; }
    public int keys(long tick) { return fresh(tick)?keys:BRAKE; }
    public boolean consumeToggle(long tick) { boolean result=fresh(tick)&&toggle;toggle=false;return result; }
    public void reset() { keys=0;toggle=false;lastTick=Long.MIN_VALUE; }
}
