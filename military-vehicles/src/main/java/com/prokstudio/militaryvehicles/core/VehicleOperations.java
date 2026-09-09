package com.prokstudio.militaryvehicles.core;

/** Pure, bounded rules for crew roles, deployment, service accounting and arcade gun controls. */
public final class VehicleOperations {
    public static final int ACTION=1,DEPLOY=2,DEPLOY_TICKS=60,SERVICE_COOLDOWN=40;
    public static final int TRANSFER_LIMIT=600,FUEL_RESERVE=200,RECOVERY_COST=40;
    public static final double SERVICE_RANGE=12,RECOVERY_STEP=.75;
    public record Deployment(int ticks,boolean extending) {
        public Deployment { if(ticks<0||ticks>DEPLOY_TICKS) throw new IllegalArgumentException("Deployment bounds"); }
        public boolean locked() { return ticks>0||extending; }
        public boolean ready() { return ticks==DEPLOY_TICKS&&extending; }
        public Deployment toggle(boolean parked,boolean grounded) {
            return parked&&grounded?new Deployment(ticks,!extending):this;
        }
        public Deployment tick(boolean parked,boolean grounded,boolean functional) {
            boolean target=extending&&parked&&grounded&&functional;
            return new Deployment(Math.clamp(ticks+(target?1:-1),0,DEPLOY_TICKS),target);
        }
    }
    private VehicleOperations() {}
    public static boolean authorized(VehicleKind kind,int seat,int action) {
        if(action==DEPLOY) return kind==VehicleKind.HOWITZER&&seat==0;
        if(action!=ACTION) return false;
        return kind.armed()?seat==1:kind.support()&&seat==0;
    }
    public static boolean parked(double speedSquared) { return Double.isFinite(speedSquared)&&speedSquared>=0&&speedSquared<=.000025; }
    public static int transfer(int source,int target,int capacity) { return transfer(FleetTuning.DEFAULT,source,target,capacity); }
    public static int transfer(FleetTuning settings,int source,int target,int capacity) {
        if(!settings.supportEnabled()||source<0||source>VehicleKind.TANKER.tank||target<0||capacity<=0||target>capacity) return 0;
        return Math.min(settings.transferLimit(),Math.min(Math.max(0,source-settings.fuelReserve()),capacity-target));
    }
    public static int repair(int current,int capacity,int kits) { return repair(FleetTuning.DEFAULT,current,capacity,kits); }
    public static int repair(FleetTuning settings,int current,int capacity,int kits) {
        if(!settings.supportEnabled()||current<0||capacity<=0||current>=capacity||kits<=0) return 0;
        return Math.min(settings.workshopRepair(),capacity-current);
    }
    public static int gunCooldown(VehicleKind kind) { return gunCooldown(FleetTuning.DEFAULT,kind); }
    public static int gunCooldown(FleetTuning settings,VehicleKind kind) {
        return kind==VehicleKind.HOWITZER?settings.howitzerReloadTicks():settings.tankReloadTicks();
    }
    public static int restoredCooldown(FleetTuning settings,VehicleKind kind) {
        return Math.max(100,kind.armed()?gunCooldown(settings,kind):settings.serviceCooldownTicks());
    }
    // The autocannon trades reach and punch for elevation: it is a squad weapon, not a main gun.
    public static double gunRange(VehicleKind kind) { return kind==VehicleKind.HOWITZER?64:kind==VehicleKind.IFV?40:48; }
    public static float gunDamage(VehicleKind kind) { return kind==VehicleKind.HOWITZER?18:kind==VehicleKind.IFV?9:12; }
    public static float minPitch(VehicleKind kind) { return kind==VehicleKind.HOWITZER?-55:kind==VehicleKind.IFV?-45:-20; }
    public static float maxPitch(VehicleKind kind) { return kind==VehicleKind.HOWITZER?8:kind==VehicleKind.IFV?15:12; }
    public static float aimYaw(float current,float target) {
        if(!Float.isFinite(current)||!Float.isFinite(target)) return 0;
        return TruckPhysics.wrap(current+(float)TruckPhysics.clamp(TruckPhysics.wrap(target-current),-3,3));
    }
    public static float aimPitch(VehicleKind kind,float current,float target) {
        if(!Float.isFinite(current)||!Float.isFinite(target)) return 0;
        return (float)TruckPhysics.approach(TruckPhysics.clamp(current,minPitch(kind),maxPitch(kind)),TruckPhysics.clamp(target,minPitch(kind),maxPitch(kind)),2);
    }
    public static boolean canFire(VehicleKind kind,int cooldown,int shells,boolean functional,boolean grounded,boolean water,Deployment deployment) {
        return canFire(FleetTuning.DEFAULT,kind,cooldown,shells,functional,grounded,water,deployment);
    }
    public static boolean canFire(FleetTuning settings,VehicleKind kind,int cooldown,int shells,boolean functional,boolean grounded,boolean water,Deployment deployment) {
        return settings.weaponsEnabled()&&kind.armed()&&cooldown==0&&shells>0&&functional&&grounded&&!water&&(kind!=VehicleKind.HOWITZER||deployment.ready());
    }
}
