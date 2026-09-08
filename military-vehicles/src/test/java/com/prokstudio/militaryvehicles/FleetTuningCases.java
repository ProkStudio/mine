package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.config.FleetConfigFile;
import com.prokstudio.militaryvehicles.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Same production-code cases run under JUnit and a standalone Java launcher; no Minecraft mocks. */
public final class FleetTuningCases {
    private static long checks;
    private FleetTuningCases() {}
    private static void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    private static void eq(Object expected,Object actual) { check(Objects.equals(expected,actual),expected+" != "+actual); }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    @FunctionalInterface private interface InDirectory { void run(Path directory) throws Exception; }
    private static void rejects(Checked action) throws Exception {
        boolean rejected=false;
        try {action.run();} catch(IllegalArgumentException|IOException expected) {rejected=true;}
        check(rejected,"Invalid input must be rejected");
    }
    private static void temporary(InDirectory action) throws Exception {
        Path dir=Files.createTempDirectory("fleet-tuning-");
        try {action.run(dir);} finally {
            try(var paths=Files.walk(dir)) {for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);}
        }
    }
    private static FleetTuning from(int[] v) {return new FleetTuning(true,true,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9]);}
    public static void defaultsPreserveLegacyEconomy() {
        var d=FleetTuning.DEFAULT;
        eq(1,d.engineFuelUnits());eq(5,d.shotFuelCost());eq(60,d.tankReloadTicks());eq(100,d.howitzerReloadTicks());
        eq(VehicleOperations.SERVICE_COOLDOWN,d.serviceCooldownTicks());eq(VehicleOperations.TRANSFER_LIMIT,d.transferLimit());
        eq(VehicleOperations.FUEL_RESERVE,d.fuelReserve());eq(VehicleOperations.RECOVERY_COST,d.recoveryFuelCost());
        eq(100,d.workshopRepair());eq(TruckSpec.NEARBY_LIMIT,d.nearbyVehicleLimit());
        check(d.weaponsEnabled()&&d.supportEnabled(),"Default role switches");
        for(var k:VehicleKind.values()) {
            eq(VehicleOperations.transfer(1000,k.tank-7,k.tank),VehicleOperations.transfer(d,1000,k.tank-7,k.tank));
            eq(VehicleOperations.repair(k.condition-3,k.condition,1),VehicleOperations.repair(d,k.condition-3,k.condition,1));
            eq(VehicleOperations.gunCooldown(k),VehicleOperations.gunCooldown(d,k));
        }
    }
    public static void partialFilesInheritDocumentedDefaults() {
        eq(FleetTuning.DEFAULT,FleetTuning.parse("# defaults\n"));
        var a=FleetTuning.parse("weaponsEnabled=false\nengineFuelUnits=3\n");
        check(!a.weaponsEnabled()&&a.supportEnabled(),"Partial booleans");eq(3,a.engineFuelUnits());eq(200,a.fuelReserve());
        eq(FleetTuning.DEFAULT,FleetTuning.parse("schemaVersion=1\r\n"));
    }
    public static void rejectsUnknownAndDuplicateKeys() throws Exception {
        for(String text:List.of("typo=1", "weaponsEnabled=false\nweaponsEnabled=true", "shotFuelCost=5\nshotFuelCost:8",
            "shotFuelCost=5\nshotFuel\\u0043ost=8", "schemaVersion=1\nschemaVersion=1")) rejects(()->FleetTuning.parse(text));
    }
    public static void rejectsUnsupportedSchemaAndMalformedValues() throws Exception {
        for(String text:List.of("schemaVersion=0","schemaVersion=2","schemaVersion=-1","schemaVersion=1.0",
            "weaponsEnabled=yes","supportEnabled=TRUE","engineFuelUnits=NaN","shotFuelCost=5.5",
            "tankReloadTicks=2147483648","fuelReserve=-1","engineFuelUnits=","shotFuelCost=+5","\\uFEFFschemaVersion=1"))
            rejects(()->FleetTuning.parse(text));
        rejects(()->FleetTuning.parse("#".repeat(FleetTuning.MAX_TEXT_LENGTH+1)));
    }
    public static void integerBoundsCannotBeBypassed() throws Exception {
        String[] keys={"engineFuelUnits","shotFuelCost","tankReloadTicks","howitzerReloadTicks","serviceCooldownTicks",
            "transferLimit","fuelReserve","workshopRepair","recoveryFuelCost","nearbyVehicleLimit"};
        int[] min={1,1,20,40,10,1,0,1,1,1},max={10,50,600,1200,200,600,2000,100,200,12};
        from(min);from(max);
        for(int i=0;i<keys.length;i++) {
            eq(from(min),FleetTuning.parse(from(min).serialize()));eq(from(max),FleetTuning.parse(from(max).serialize()));
            String key=keys[i];int lo=min[i],hi=max[i];FleetTuning.parse(key+"="+lo);FleetTuning.parse(key+"="+hi);
            rejects(()->FleetTuning.parse(key+"="+(lo-1)));rejects(()->FleetTuning.parse(key+"="+(hi+1)));
            int[] under=min.clone(),over=max.clone();under[i]=lo-1;over[i]=hi+1;
            rejects(()->from(under));rejects(()->from(over));
        }
    }
    public static void canonicalRoundTripIsDeterministic() {
        for(int i=0;i<200;i++) {
            FleetTuning settings=new FleetTuning(i%2==0,i%3==0,1+i%10,1+i%50,20+i,40+i,10+i%191,
                1+i*3%600,i*10,1+i%100,1+i%200,1+i%12);
            String wire=settings.serialize();eq(settings,FleetTuning.parse(wire));eq(wire,FleetTuning.parse(wire).serialize());
            check(wire.length()<FleetTuning.MAX_TEXT_LENGTH,"Bounded wire packet");
        }
    }
    public static void createsMissingFileWithoutOverwritingExistingEdits() throws Exception {
        temporary(dir->{
            Path path=dir.resolve("sub/server.properties");var store=new FleetConfigFile(path);
            eq(FleetTuning.DEFAULT,store.reload());check(Files.isRegularFile(path),"Default created");
            String edited="# preserve my comment\nweaponsEnabled=false\n";Files.writeString(path,edited);
            check(!store.reload().weaponsEnabled(),"Edited file loaded");eq(edited,Files.readString(path));
        });
    }
    public static void invalidReloadKeepsLastGoodSnapshot() throws Exception {
        temporary(dir->{
            Path path=dir.resolve("server.properties");Files.writeString(path,"shotFuelCost=17\n");var store=new FleetConfigFile(path);
            var good=store.reload();eq(17,good.shotFuelCost());
            Files.writeString(path,"shotFuelCost=30\nunknownSetting=1\n");String bad=Files.readString(path);
            rejects(store::reload);check(store.current()==good,"No partial assignment");eq(bad,Files.readString(path));
            var fresh=new FleetConfigFile(path);rejects(fresh::reload);eq(FleetTuning.DEFAULT,fresh.current());
        });
    }
    public static void boundsBytesAndRejectsMalformedUtf8() throws Exception {
        temporary(dir->{
            Path path=dir.resolve("server.properties");var store=new FleetConfigFile(path);store.reload();
            Files.write(path,new byte[FleetConfigFile.MAX_BYTES+1]);rejects(store::reload);
            Files.write(path,new byte[]{(byte)0xc3,(byte)0x28});rejects(store::reload);
            Files.writeString(path,"\uFEFFschemaVersion=1",StandardCharsets.UTF_8);rejects(store::reload);
            Files.writeString(path,"#".repeat(FleetTuning.MAX_TEXT_LENGTH+1));rejects(store::reload);
            eq(FleetTuning.DEFAULT,store.current());
        });
    }
    public static void separateStoresDoNotLeakServerState() throws Exception {
        temporary(dir->{
            Path path=dir.resolve("server.properties");Files.writeString(path,"weaponsEnabled=false\nshotFuelCost=9\n");
            var oldServer=new FleetConfigFile(path);oldServer.reload();Files.writeString(path,"supportEnabled=false\n");
            var newServer=new FleetConfigFile(path);newServer.reload();
            check(!oldServer.current().weaponsEnabled()&&oldServer.current().supportEnabled(),"Old snapshot isolated");
            check(newServer.current().weaponsEnabled()&&!newServer.current().supportEnabled(),"New server has no stale values");
            eq(5,newServer.current().shotFuelCost());
        });
    }
    public static void configuredTransfersConserveFuelAndReserve() {
        for(int limit:new int[]{1,73,600}) for(int reserve:new int[]{0,200,2000}) {
            var c=FleetTuning.parse("transferLimit="+limit+"\nfuelReserve="+reserve);
            for(var k:VehicleKind.values()) for(int source=0;source<=9600;source+=137) for(int target=0;target<=k.tank;target+=113) {
                int n=VehicleOperations.transfer(c,source,target,k.tank);
                check(n>=0&&n<=limit,"Transfer bound");check(source-n>=Math.min(source,reserve),"Reserve held");
                check(target+n<=k.tank,"Tank capacity");eq(source+target,(source-n)+(target+n));
            }
        }
        var c=FleetTuning.DEFAULT;
        for(int bad:new int[]{-1,9601,Integer.MAX_VALUE}) eq(0,VehicleOperations.transfer(c,bad,0,2400));
        eq(0,VehicleOperations.transfer(c,1000,-1,2400));eq(0,VehicleOperations.transfer(c,1000,2401,2400));
    }
    public static void configuredRepairsRequireKitsAndRespectCapacity() {
        for(int repair:new int[]{1,37,100}) {
            var c=FleetTuning.parse("workshopRepair="+repair);
            for(var k:VehicleKind.values()) for(int condition=0;condition<=k.condition;condition++) {
                eq(0,VehicleOperations.repair(c,condition,k.condition,0));eq(0,VehicleOperations.repair(c,condition,k.condition,-1));
                eq(Math.min(repair,k.condition-condition),VehicleOperations.repair(c,condition,k.condition,1));
            }
            eq(0,VehicleOperations.repair(c,-1,200,1));eq(0,VehicleOperations.repair(c,201,200,1));
        }
    }
    public static void weaponDisableAndReloadLimitsApply() {
        var off=FleetTuning.parse("weaponsEnabled=false\ntankReloadTicks=600\nhowitzerReloadTicks=1200");
        var ready=new VehicleOperations.Deployment(60,true);
        for(var k:VehicleKind.values()) check(!VehicleOperations.canFire(off,k,0,64,true,true,false,ready),"Disabled guns cannot fire");
        eq(600,VehicleOperations.gunCooldown(off,VehicleKind.TANK));eq(1200,VehicleOperations.gunCooldown(off,VehicleKind.HOWITZER));
        var on=FleetTuning.DEFAULT;
        for(var k:VehicleKind.values()) for(boolean ground:new boolean[]{false,true}) for(boolean water:new boolean[]{false,true})
            for(int cooldown:new int[]{-1,0,1,1200}) for(int shells:new int[]{-1,0,1}) {
                boolean expected=k.armed()&&ground&&!water&&cooldown==0&&shells>0;
                eq(expected,VehicleOperations.canFire(on,k,cooldown,shells,true,ground,water,ready));
            }
    }
    public static void supportDisableStopsAccounting() {
        var c=FleetTuning.parse("supportEnabled=false");
        for(var k:VehicleKind.values()) {
            eq(0,VehicleOperations.transfer(c,9600,0,k.tank));eq(0,VehicleOperations.repair(c,0,k.condition,64));
        }
    }
    public static void engineDebitCannotUnderflow() throws Exception {
        for(int cost=1;cost<=10;cost++) {
            var c=FleetTuning.parse("engineFuelUnits="+cost);
            for(int fuel=0;fuel<=9600;fuel++) {
                int debit=c.engineDebit(fuel);eq(Math.min(fuel,cost),debit);check(fuel-debit>=0,"Never underflow");
            }
            rejects(()->c.engineDebit(-1));
        }
    }
    public static void restoringCannotShortenConfiguredCooldown() {
        var c=FleetTuning.parse("tankReloadTicks=600\nhowitzerReloadTicks=1200\nserviceCooldownTicks=200");
        eq(600,VehicleOperations.restoredCooldown(c,VehicleKind.TANK));eq(1200,VehicleOperations.restoredCooldown(c,VehicleKind.HOWITZER));
        for(var k:VehicleKind.values()) if(!k.armed()) eq(200,VehicleOperations.restoredCooldown(c,k));
        for(var k:VehicleKind.values()) eq(100,VehicleOperations.restoredCooldown(FleetTuning.DEFAULT,k));
    }
    public static void firstPhysicalHitBlocksTargetsBehindIt() {
        var target=new ServiceTargeting.Hit(2,16,true);var blocker=new ServiceTargeting.Hit(1,4,false);
        check(ServiceTargeting.select(List.of(target,blocker)).isEmpty(),"Do not skip a player, occupied truck or other blocker");
        eq(2,ServiceTargeting.select(List.of(target,new ServiceTargeting.Hit(3,25,false))).orElseThrow());
        check(ServiceTargeting.select(List.of(new ServiceTargeting.Hit(1,0,false),target)).isEmpty(),"Starting inside a blocker");
        check(ServiceTargeting.select(List.of()).isEmpty(),"No hit means no target");
    }
    public static void raySelectionIsOrderIndependentAndFinite() {
        var hits=new ArrayList<>(List.of(new ServiceTargeting.Hit(7,9,true),new ServiceTargeting.Hit(2,9,true),
            new ServiceTargeting.Hit(8,25,true),new ServiceTargeting.Hit(1,Double.NaN,true),
            new ServiceTargeting.Hit(0,Double.POSITIVE_INFINITY,true),new ServiceTargeting.Hit(0,-1,true),new ServiceTargeting.Hit(-1,0,true)));
        var random=new Random(21);
        for(int i=0;i<200;i++) {Collections.shuffle(hits,random);eq(2,ServiceTargeting.select(hits).orElseThrow());}
        hits.add(new ServiceTargeting.Hit(1,9,false));check(ServiceTargeting.select(hits).isEmpty(),"Stable tie blocker");
    }
    public static void serviceTargetsMustBeGroundedDryAndStationary() {
        for(int flags=0;flags<64;flags++) for(double speed:new double[]{0,.000025,.000026,1,-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            boolean operational=(flags&1)!=0,occupied=(flags&2)!=0,running=(flags&4)!=0,locked=(flags&8)!=0,ground=(flags&16)!=0,water=(flags&32)!=0;
            boolean expected=operational&&!occupied&&!running&&!locked&&ground&&!water&&Double.isFinite(speed)&&speed>=0&&speed<=.000025;
            eq(expected,ServiceTargeting.ready(operational,occupied,running,locked,ground,water,speed));
        }
    }
    public static long runAll() throws Exception {
        checks=0;
        defaultsPreserveLegacyEconomy();
        partialFilesInheritDocumentedDefaults();
        rejectsUnknownAndDuplicateKeys();
        rejectsUnsupportedSchemaAndMalformedValues();
        integerBoundsCannotBeBypassed();
        canonicalRoundTripIsDeterministic();
        createsMissingFileWithoutOverwritingExistingEdits();
        invalidReloadKeepsLastGoodSnapshot();
        boundsBytesAndRejectsMalformedUtf8();
        separateStoresDoNotLeakServerState();
        configuredTransfersConserveFuelAndReserve();
        configuredRepairsRequireKitsAndRespectCapacity();
        weaponDisableAndReloadLimitsApply();
        supportDisableStopsAccounting();
        engineDebitCannotUnderflow();
        restoringCannotShortenConfiguredCooldown();
        firstPhysicalHitBlocksTargetsBehindIt();
        raySelectionIsOrderIndependentAndFinite();
        serviceTargetsMustBeGroundedDryAndStationary();
        return checks;
    }
}
