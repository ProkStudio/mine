package dev.mine.arsenal.core;

public record Magazine(int rounds, Ammo ammo, Weapon.Mode mode) {
    public static Magazine read(Weapon weapon,int rounds,String ammoId,String modeId) {
        Ammo ammo=Ammo.find(ammoId,weapon.ammunition.getFirst());
        if(!weapon.ammunition.contains(ammo)) ammo=weapon.ammunition.getFirst();
        Weapon.Mode mode=weapon.modes.getFirst();
        for(var candidate:weapon.modes) if(candidate.name().equals(modeId)) mode=candidate;
        return new Magazine(Math.max(0,Math.min(weapon.capacity,rounds)),ammo,mode);
    }
    public Magazine shot() { return new Magazine(Math.max(0,rounds-1),ammo,mode); }
    public Magazine reload(Weapon weapon,int available) {
        int quantity=Math.min(Math.max(0,available),Math.max(0,weapon.capacity-rounds));
        if(weapon.shellReload()) quantity=Math.min(1,quantity);
        return new Magazine(rounds+quantity,ammo,mode);
    }
    public Magazine cycleMode(Weapon weapon) {
        return new Magazine(rounds,ammo,weapon.modes.get((weapon.modes.indexOf(mode)+1)%weapon.modes.size()));
    }
    /** Loaded rounds can never be silently transmuted into a different ammunition. */
    public Magazine cycleAmmo(Weapon weapon) {
        if(rounds!=0) return this;
        return new Magazine(0,weapon.ammunition.get((weapon.ammunition.indexOf(ammo)+1)%weapon.ammunition.size()),mode);
    }
}
