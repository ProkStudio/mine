package dev.mine.arsenal.core;

/** Tick-based server cadence. Packet count never changes rate of fire. */
public final class Trigger {
    private long nextShot;
    private boolean wasDown;
    private int burst;
    public boolean tick(long now,boolean down,boolean allowed,Weapon.Mode mode,int interval) {
        boolean edge=down&&!wasDown; wasDown=down;
        if(!allowed) { burst=0; return false; }
        if(mode==Weapon.Mode.BURST && edge && now>=nextShot) burst=3;
        boolean wanted=switch(mode) { case SEMI -> edge; case AUTO -> down; case BURST -> burst>0; };
        if(!wanted || now<nextShot) return false;
        nextShot=now+Math.max(1,interval);
        if(mode==Weapon.Mode.BURST) { burst--; if(burst==0) nextShot+=Math.max(4,interval); }
        return true;
    }
    public void interrupt() { burst=0; wasDown=true; }
    public void equip(long now) { interrupt(); nextShot=Math.max(nextShot,now+6); }
    public long nextShot() { return nextShot; }
}
