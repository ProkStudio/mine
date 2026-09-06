package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.client.ArsenalPose;
import dev.mine.arsenal.core.Weapon;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public abstract class WeaponPoseStateMixin implements ArsenalPose {
    @Unique private Weapon arsenal$weapon;
    @Unique private boolean arsenal$left,arsenal$aim;
    @Unique private int arsenal$frame;
    public Weapon arsenal$weapon() { return arsenal$weapon; }
    public boolean arsenal$left() { return arsenal$left; }
    public boolean arsenal$aim() { return arsenal$aim; }
    public int arsenal$frame() { return arsenal$frame; }
    public void arsenal$set(Weapon weapon,boolean left,boolean aim,int frame) {
        arsenal$weapon=weapon; arsenal$left=left; arsenal$aim=aim; arsenal$frame=frame;
    }
}
