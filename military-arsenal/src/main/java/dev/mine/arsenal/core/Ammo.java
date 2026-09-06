package dev.mine.arsenal.core;

/** Fictional, deliberately game-scaled ammunition. No real-world ballistics. */
public enum Ammo {
    LIGHT("light_round", "Light rounds", "Лёгкие патроны", 5, 1, 0, 0, 0, false, false),
    HEAVY("heavy_round", "Heavy pistol rounds", "Тяжёлые пистолетные патроны", 7, 1, 0, 0, 0, false, false),
    MAGNUM("magnum_round", "Magnum rounds", "Патроны «Магнум»", 11, 1, 0, 0, 0, false, false),
    INTERMEDIATE("intermediate_round", "Carbine rounds", "Патроны для карабина", 6, 1, 0, 0, 0, false, false),
    RIFLE("rifle_round", "Rifle rounds", "Винтовочные патроны", 9, 1, 0, 0, 0, false, false),
    PRECISION("precision_round", "Precision rounds", "Снайперские патроны", 19, 1, 0, 0, 0, false, false),
    BUCKSHOT("buckshot", "Buckshot shells", "Дробовые патроны", 2.5, 8, 5.5, 0, 0, false, false),
    SLUG("slug", "Slug shells", "Пулевые патроны", 13, 1, 0, 0, 0, false, false),
    GRENADE_HE("grenade_he", "HE grenades", "Осколочные гранаты", 12, 1, 0, 1.1, 4, false, false),
    GRENADE_SMOKE("grenade_smoke", "Smoke grenades", "Дымовые гранаты", 0, 1, 0, 1, 0, false, true),
    ROCKET_HE("rocket_he", "HE rockets", "Осколочно-фугасные ракеты", 15, 1, 0, 1.8, 5, false, false),
    ROCKET_AP("rocket_ap", "Anti-armour rockets", "Противотанковые ракеты", 42, 1, 0, 2.2, 2, true, false),
    ROCKET_SMOKE("rocket_smoke", "Smoke rockets", "Дымовые ракеты", 0, 1, 0, 1.7, 0, false, true),
    ROCKET_PRACTICE("rocket_practice", "Inert training rockets", "Инертные учебные ракеты", 0, 1, 0, 1.8, 0, false, false);

    public final String id, en, ru;
    public final double damage, spread, velocity, radius;
    public final int pellets;
    public final boolean piercing, smoke;
    Ammo(String id,String en,String ru,double damage,int pellets,double spread,double velocity,double radius,boolean piercing,boolean smoke) {
        this.id=id; this.en=en; this.ru=ru; this.damage=damage; this.pellets=pellets; this.spread=spread;
        this.velocity=velocity; this.radius=radius; this.piercing=piercing; this.smoke=smoke;
    }
    public boolean projectile() { return velocity>0; }
    public boolean grenade() { return this==GRENADE_HE || this==GRENADE_SMOKE; }
    public double gravity() { return grenade()?.045:.003; }
    public static Ammo find(String id,Ammo fallback) {
        for(Ammo a:values()) if(a.id.equals(id)) return a;
        return fallback;
    }
}
