import dev.mine.arsenal.core.*;
import java.util.*;

/** Standalone assertions execute without Minecraft bootstrap or third-party dependencies. */
public final class ArsenalCoreChecks {
    private static int checks;
    private static void check(boolean ok,String description) { checks++; if(!ok) throw new AssertionError(description); }
    public static void catalog() {
        check(Weapon.values().length==16,"weapon count"); check(Ammo.values().length==14,"ammo count");
        Set<String> ids=new HashSet<>();
        for(Weapon w:Weapon.values()) {
            check(ids.add(w.id),"unique weapon id"); check(w.capacity>0&&w.reloadTicks>0&&w.interval>0,"positive timings");
            check(!w.ammunition.isEmpty()&&!w.modes.isEmpty(),"usable weapon");
            check(WeaponGeometry.create(w).size()>=20,"model is not a placeholder");
            for(var p:WeaponGeometry.create(w)) {
                check(p.w()>0&&p.h()>0&&p.d()>0,"positive geometry");
                check(p.x()>=-16&&p.y()>=-16&&p.z()>=-16&&p.x()+p.w()<=32&&p.y()+p.h()<=32&&p.z()+p.d()<=32,"model envelope");
            }
        }
        for(Ammo a:Ammo.values()) check(ids.add(a.id),"unique ammo id");
        check(Ammo.ROCKET_PRACTICE.damage==0&&Ammo.ROCKET_PRACTICE.radius==0,"inert training round");
        check(Ammo.ROCKET_AP.piercing&&Ammo.ROCKET_AP.radius<Ammo.ROCKET_HE.radius,"AP differs from HE");
    }
    public static void reload() {
        for(Weapon w:Weapon.values()) {
            Magazine empty=Magazine.read(w,-5,"broken","broken");
            check(empty.rounds()==0&&w.ammunition.contains(empty.ammo())&&w.modes.contains(empty.mode()),"sanitized empty");
            check(Magazine.read(w,999,"broken","broken").rounds()==w.capacity,"clamped capacity");
            Magazine partial=empty.reload(w,2);
            check(partial.rounds()==Math.min(w.shellReload()?1:2,w.capacity),"partial reload");
            check(partial.cycleAmmo(w).ammo()==partial.ammo(),"no loaded ammo conversion");
            check(empty.reload(w,-3).rounds()==0,"negative inventory");
            check(partial.shot().rounds()==Math.max(0,partial.rounds()-1),"one shot consumes one");
            int inventory=137,shots=0;
            Magazine state=empty;
            for(int i=0;i<1000;i++) {
                if(state.rounds()==0) { Magazine next=state.reload(w,inventory); inventory-=next.rounds()-state.rounds();state=next; }
                if(state.rounds()>0) { state=state.shot(); shots++; }
                check(inventory+state.rounds()+shots==137,"ammunition conservation");
            }
            check(shots==137&&inventory==0&&state.rounds()==0,"no duplication");
        }
        var rocket=Magazine.read(Weapon.ATLAS,0,Ammo.ROCKET_HE.id,"SEMI").cycleAmmo(Weapon.ATLAS);
        check(rocket.ammo()==Ammo.ROCKET_AP,"cycle to AP");
    }
    public static void trigger() {
        Trigger auto=new Trigger(); int shots=0;
        for(int t=0;t<100;t++) if(auto.tick(t,true,true,Weapon.Mode.AUTO,3)) shots++;
        check(shots==34,"automatic cadence");
        Trigger semi=new Trigger();shots=0;
        for(int t=0;t<100;t++) if(semi.tick(t,true,true,Weapon.Mode.SEMI,3)) shots++;
        check(shots==1,"semi requires release");
        Trigger burst=new Trigger();shots=0;
        for(int t=0;t<100;t++) if(burst.tick(t,true,true,Weapon.Mode.BURST,3)) shots++;
        check(shots==3,"exact three-round burst");
        Trigger spam=new Trigger();shots=0;
        for(int i=0;i<10000;i++) if(spam.tick(0,true,true,Weapon.Mode.AUTO,3)) shots++;
        check(shots==1,"same tick packet spam cannot speed up fire");
        Trigger switcher=new Trigger();check(switcher.tick(0,true,true,Weapon.Mode.SEMI,27),"initial shot");
        switcher.equip(1); switcher.tick(2,false,true,Weapon.Mode.SEMI,27);
        check(!switcher.tick(8,true,true,Weapon.Mode.SEMI,27),"switch keeps cooldown");
        switcher.tick(26,false,true,Weapon.Mode.SEMI,27); check(switcher.tick(27,true,true,Weapon.Mode.SEMI,27),"cooldown ends on time");
        Trigger interrupted=new Trigger(); interrupted.tick(0,true,true,Weapon.Mode.BURST,3);
        interrupted.tick(1,false,false,Weapon.Mode.BURST,3); check(!interrupted.tick(10,false,true,Weapon.Mode.BURST,3),"cancelled burst stays cancelled");
    }
    public static void animation() {
        check(Animation.shotFrame(-1)==0&&Animation.shotFrame(4)==0,"shot boundaries");
        check(Animation.reloadFrame(0,40)==10&&Animation.reloadFrame(40,40)==17,"reload frames");
        check(Math.abs(Animation.magazineTravel(10))<1e-8&&Math.abs(Animation.magazineTravel(17))<1e-8,"magazine returns home");
        check(Animation.magazineTravel(13)>.9,"magazine actually moves");
        check(Animation.recoil(-1)==0&&Animation.recoil(15)==0,"recoil settles");
        check(Animation.blast(6,5)==0&&Animation.blast(0,5)==1,"blast falloff bounds");
        check(Animation.falloff(0,64)==1&&Animation.falloff(1000,64)==.45,"damage falloff bounds");
    }
    public static void main(String[] args) { catalog();reload();trigger();animation();System.out.println("PASS "+checks+" standalone assertions (catalog, geometry, reload conservation, cadence, animation)"); }
}
