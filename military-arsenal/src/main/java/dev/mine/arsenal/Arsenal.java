package dev.mine.arsenal;

import dev.mine.arsenal.core.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.*;
import java.util.*;

public final class Arsenal implements ModInitializer {
    public static final String MOD_ID="arsenal";
    public static final Logger LOG=LoggerFactory.getLogger("Military Arsenal");
    public static ArsenalConfig CONFIG;
    public static final Map<Weapon,GunItem> GUNS=new EnumMap<>(Weapon.class);
    public static final Map<Ammo,Item> AMMO=new EnumMap<>(Ammo.class);
    public static final Map<String,SoundEvent> SOUNDS=new HashMap<>();
    public static EntityType<ArsenalProjectile> PROJECTILE;
    public static final WeaponService SERVICE=new WeaponService();
    public static Identifier id(String path) { return Identifier.of(MOD_ID,path); }
    private static Item.Settings settings(String id) {
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM,id(id))).component(DataComponentTypes.ITEM_MODEL,id(id));
    }
    @Override public void onInitialize() {
        CONFIG=ArsenalConfig.load();
        for(Weapon w:Weapon.values()) GUNS.put(w,Registry.register(Registries.ITEM,id(w.id),new GunItem(settings(w.id).maxCount(1),w)));
        for(Ammo a:Ammo.values()) AMMO.put(a,Registry.register(Registries.ITEM,id(a.id),new Item(settings(a.id).maxCount(a.projectile()?16:64))));
        var entityKey=RegistryKey.of(RegistryKeys.ENTITY_TYPE,id("projectile"));
        PROJECTILE=Registry.register(Registries.ENTITY_TYPE,entityKey,EntityType.Builder.<ArsenalProjectile>create(ArsenalProjectile::new,SpawnGroup.MISC)
            .dimensions(.22f,.22f).maxTrackingRange(8).trackingTickInterval(1).disableSaving().build(entityKey));
        for(String name:List.of("pistol","smg","rifle","shotgun","precision","rocket","grenade","reload_out","reload_in","bolt","dry","impact"))
            SOUNDS.put(name,Registry.register(Registries.SOUND_EVENT,id(name),SoundEvent.of(id(name))));
        Registry.register(Registries.ITEM_GROUP,id("armory"),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.arsenal"))
            .icon(()->new ItemStack(GUNS.get(Weapon.LYNX))).entries((context,entries)->{
                for(Weapon w:Weapon.values()) entries.add(GUNS.get(w));
                for(Ammo a:Ammo.values()) entries.add(AMMO.get(a));
            }).build());
        PayloadTypeRegistry.playC2S().register(ArsenalPackets.Input.ID,ArsenalPackets.Input.CODEC);
        PayloadTypeRegistry.playS2C().register(ArsenalPackets.Feedback.ID,ArsenalPackets.Feedback.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ArsenalPackets.Input.ID,(payload,context)->SERVICE.accept(context.player(),payload.keys()));
        ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->SERVICE.remove(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->SERVICE.clear());
        LOG.info("Military Arsenal: {} weapons / {} ammunition types",GUNS.size(),AMMO.size());
    }
    public static Ammo ammo(ItemStack stack) {
        for(var entry:AMMO.entrySet()) if(stack.isOf(entry.getValue())) return entry.getKey();
        return Ammo.ROCKET_PRACTICE;
    }
}
