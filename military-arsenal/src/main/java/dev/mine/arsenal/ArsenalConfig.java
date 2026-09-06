package dev.mine.arsenal;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;

/** Conservative server-owned balance. Existing invalid files are never overwritten. */
public final class ArsenalConfig {
    public int format=1;
    public boolean enabled=true;
    public boolean pvpDamage=false;
    public boolean explosions=true;
    public double damageMultiplier=1;
    public int smokeCloudLimit=12;
    public int projectileLimitPerPlayer=8;
    public int inputTimeoutTicks=8;
    public static ArsenalConfig load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("military-arsenal.json");
        var gson=new GsonBuilder().setPrettyPrinting().create();
        ArsenalConfig c=new ArsenalConfig();
        try {
            if(Files.exists(path)) { c=gson.fromJson(Files.readString(path),ArsenalConfig.class); if(c==null) throw new IllegalArgumentException("Empty config"); }
            else { Files.createDirectories(path.getParent()); Files.writeString(path,gson.toJson(c)+"\n"); }
        } catch(Exception ex) { Arsenal.LOG.warn("Cannot read military-arsenal.json; using safe defaults, original retained",ex); c=new ArsenalConfig(); }
        if(!Double.isFinite(c.damageMultiplier)) c.damageMultiplier=1;
        c.damageMultiplier=Math.clamp(c.damageMultiplier,0,4);
        c.smokeCloudLimit=Math.clamp(c.smokeCloudLimit,0,24);
        c.projectileLimitPerPlayer=Math.clamp(c.projectileLimitPerPlayer,1,16);
        c.inputTimeoutTicks=Math.clamp(c.inputTimeoutTicks,3,12);
        return c;
    }
}
