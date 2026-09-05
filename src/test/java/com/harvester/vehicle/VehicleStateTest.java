package com.harvester.vehicle;

import com.mojang.serialization.DynamicOps;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VehicleStateTest {
    private static DynamicOps<NbtElement> ops;
    @BeforeAll static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        ops=DynamicRegistryManager.of(Registries.REGISTRIES).getOps(NbtOps.INSTANCE);
    }
    @Test void fullCargoRoundtripRetainsComponentsAndEverySlot() {
        List<ItemStack> cargo=new ArrayList<>();
        for(int i=0;i<54;i++) {
            ItemStack stack=new ItemStack(i%2==0?Items.WHEAT:Items.DIAMOND,64);
            stack.set(DataComponentTypes.CUSTOM_NAME,Text.literal("Cargo "+i));
            cargo.add(stack);
        }
        VehicleState original=new VehicleState(VehicleType.COMBINE_WIDE,1731,87,14,false,4,cargo);
        NbtCompound saved=original.encode(ops);
        // Same component container used by the pickup item, with a binary NBT codec roundtrip.
        NbtComponent component=NbtComponent.of(saved);
        NbtElement encoded=NbtComponent.CODEC.encodeStart(ops,component).getOrThrow();
        NbtComponent restored=NbtComponent.CODEC.parse(ops,encoded).getOrThrow();
        VehicleState result=VehicleState.decode(restored.copyNbt(),ops);
        assertEquals(original.type(),result.type()); assertEquals(original.fuel(),result.fuel());
        assertEquals(original.condition(),result.condition()); assertEquals(original.color(),result.color());
        assertEquals(original.headerEnabled(),result.headerEnabled()); assertEquals(original.workCooldown(),result.workCooldown());
        assertEquals(54,result.cargo().size());
        for(int i=0;i<54;i++) {
            assertTrue(ItemStack.areItemsAndComponentsEqual(original.cargo().get(i),result.cargo().get(i)));
            assertEquals(64,result.cargo().get(i).getCount());
        }
        cargo.get(0).decrement(1);
        assertEquals(64,result.cargo().get(0).getCount());
    }
    @Test void emptySlotsAndBrokenConditionSurvive() {
        VehicleState original=new VehicleState(VehicleType.DRONE,0,0,0,true,0,List.of(ItemStack.EMPTY,new ItemStack(Items.WHEAT,3),ItemStack.EMPTY));
        VehicleState result=VehicleState.decode(original.encode(ops),ops);
        assertTrue(result.cargo().get(0).isEmpty()); assertTrue(result.cargo().get(2).isEmpty());
        assertEquals(3,result.cargo().get(1).getCount()); assertEquals(0,result.condition());
    }
    @Test void rejectsUnknownSchemaAndType() {
        NbtCompound n=new NbtCompound(); n.putInt("Version",999);
        assertThrows(IllegalArgumentException.class,()->VehicleState.decode(n,ops));
        n.putInt("Version",1); n.putString("Type","nonexistent");
        assertThrows(IllegalArgumentException.class,()->VehicleState.decode(n,ops));
    }
    @Test void everyFamilyHasMultipleStableIds() {
        Set<String> ids=new HashSet<>();
        for(VehicleType t:VehicleType.values()) { assertTrue(ids.add(t.id)); assertEquals(t,VehicleType.fromId(t.id)); }
        for(VehicleType.Family family:VehicleType.Family.values()) assertTrue(Arrays.stream(VehicleType.values()).filter(t->t.family==family).count()>=2);
        assertEquals(2,VehicleType.MOTORCYCLE.seats);
        assertTrue(VehicleType.MOTORCYCLE.speed>VehicleType.PICKUP.speed);
        assertTrue(VehicleType.COMBINE.speed<VehicleType.PICKUP.speed);
    }
}
