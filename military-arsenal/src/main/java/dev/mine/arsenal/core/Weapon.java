package dev.mine.arsenal.core;

import java.util.List;
import static dev.mine.arsenal.core.Ammo.*;
import static dev.mine.arsenal.core.Weapon.Mode.*;
import static dev.mine.arsenal.core.Weapon.Style.*;

/** One catalog drives registration, balancing, art, recipes and localization. */
public enum Weapon {
    KESTREL("kestrel_p9","Kestrel P9","Кестрел P9",PISTOL,12,30,5,1,64,1.4,1.2,0x566568,List.of(LIGHT),List.of(SEMI)),
    BASTION("bastion_45","Bastion .45","Бастион .45",PISTOL,8,34,7,1,70,1.8,1.25,0xa59c83,List.of(HEAVY),List.of(SEMI)),
    MARSHAL("marshal_r6","Marshal R6","Маршал R6",REVOLVER,6,16,10,1,90,2.6,1.3,0x675d54,List.of(MAGNUM),List.of(SEMI)),
    VIPER("viper_smg","Viper SM9","Вайпер SM9",SMG,30,40,2,.88,55,2.4,1.3,0x64735f,List.of(LIGHT),List.of(AUTO,SEMI)),
    WARDEN("warden_smg","Warden SD","Страж SD",SMG,24,42,3,.9,60,1.8,1.4,0x46525b,List.of(HEAVY),List.of(AUTO,SEMI)),
    LYNX("lynx_carbine","Lynx C5","Рысь C5",Style.RIFLE,30,44,3,1,120,1.7,1.6,0x8a805c,List.of(INTERMEDIATE),List.of(AUTO,BURST,SEMI)),
    BOREAL("boreal_rifle","Boreal R7","Борей R7",Style.RIFLE,20,48,4,1,145,2.2,1.7,0x58644b,List.of(Ammo.RIFLE),List.of(AUTO,SEMI)),
    TRIDENT("trident_burst","Trident B3","Трезубец B3",Style.RIFLE,27,44,3,1.1,130,1.5,1.6,0x64646c,List.of(INTERMEDIATE),List.of(BURST,SEMI)),
    BREAKER("breaker_pump","Breaker P12","Бричер P12",PUMP,8,17,18,1,40,3.5,1.25,0x775d45,List.of(BUCKSHOT,SLUG),List.of(SEMI)),
    TEMPEST("tempest_auto","Tempest A12","Шторм A12",SHOTGUN,10,60,7,.82,36,4,1.25,0x646e62,List.of(BUCKSHOT,SLUG),List.of(AUTO,SEMI)),
    SENTINEL("sentinel_dmr","Sentinel D7","Дозор D7",DMR,12,50,10,1.4,190,1.1,2.5,0x7a785e,List.of(Ammo.RIFLE),List.of(SEMI)),
    LONGWATCH("longwatch_sniper","Longwatch S1","Горизонт S1",SNIPER,5,66,27,1.35,256,.55,4,0x77847c,List.of(PRECISION),List.of(SEMI)),
    BULWARK("bulwark_lmg","Bulwark L5","Оплот L5",LMG,80,96,3,.95,130,3,1.5,0x596149,List.of(INTERMEDIATE),List.of(AUTO)),
    ARC("arc_gl","Arc G40","Дуга G40",LAUNCHER,6,20,17,1,90,2,1.35,0x8a815f,List.of(GRENADE_HE,GRENADE_SMOKE),List.of(SEMI)),
    ATLAS("atlas_rpg","Atlas RPG","Атлас РПГ",RPG,1,72,28,1,160,2.2,1.6,0x627043,List.of(ROCKET_HE,ROCKET_AP,ROCKET_SMOKE,ROCKET_PRACTICE),List.of(SEMI)),
    TITAN("titan_at","Titan AT","Титан ПТ",RPG,1,90,36,1.15,210,1,2.5,0x777967,List.of(ROCKET_AP,ROCKET_PRACTICE),List.of(SEMI));

    public enum Mode { SEMI, BURST, AUTO }
    public enum Style { PISTOL, REVOLVER, SMG, RIFLE, PUMP, SHOTGUN, DMR, SNIPER, LMG, LAUNCHER, RPG }
    public final String id,en,ru;
    public final Style style;
    public final int capacity,reloadTicks,interval,color;
    public final double damageScale,range,spread,zoom;
    public final List<Ammo> ammunition;
    public final List<Mode> modes;
    Weapon(String id,String en,String ru,Style style,int capacity,int reloadTicks,int interval,double damageScale,double range,double spread,double zoom,int color,List<Ammo> ammunition,List<Mode> modes) {
        this.id=id; this.en=en; this.ru=ru; this.style=style; this.capacity=capacity; this.reloadTicks=reloadTicks; this.interval=interval;
        this.damageScale=damageScale; this.range=range; this.spread=spread; this.zoom=zoom; this.color=color;
        this.ammunition=List.copyOf(ammunition); this.modes=List.copyOf(modes);
    }
    public boolean shellReload() { return style==PUMP || style==REVOLVER || style==LAUNCHER; }
    public boolean sidearm() { return style==PISTOL || style==REVOLVER; }
    public boolean scoped() { return style==DMR || style==SNIPER || this==TITAN; }
    public String sound() { return switch(style) {
        case PISTOL,REVOLVER -> "pistol"; case SMG -> "smg"; case PUMP,SHOTGUN -> "shotgun";
        case SNIPER,DMR -> "precision"; case RPG -> "rocket"; case LAUNCHER -> "grenade"; default -> "rifle";
    }; }
}
