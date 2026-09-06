package dev.mine.arsenal.client;

import dev.mine.arsenal.*;
import dev.mine.arsenal.core.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import static dev.mine.arsenal.ArsenalPackets.*;

/** Cosmetic prediction only. The server confirms every shot and every inventory write. */
public final class ArsenalClient implements ClientModInitializer {
    private static KeyBinding reload,mode,ammo,inspect;
    private static double time,shot=-1000,hit=-1000,reloadStart=-1000,inspectStart=-1000,ads,previousAds;
    private static int reloadDuration;
    private static ItemStack previous=ItemStack.EMPTY;
    private static Object world;
    private static boolean tap,wasArmed;
    private static final MinecraftClient CLIENT=MinecraftClient.getInstance();
    @Override public void onInitializeClient() {
        var category=KeyBinding.Category.create(Arsenal.id("controls"));
        reload=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arsenal.reload",GLFW.GLFW_KEY_R,category));
        mode=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arsenal.mode",GLFW.GLFW_KEY_V,category));
        ammo=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arsenal.ammo",GLFW.GLFW_KEY_B,category));
        inspect=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arsenal.inspect",GLFW.GLFW_KEY_H,category));
        EntityRendererRegistry.register(Arsenal.PROJECTILE,ProjectileRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(Feedback.ID,(payload,context)->{
            if(context.client().player==null || payload.entity()!=context.client().player.getId()) return;
            switch(payload.event()) {
                case SHOT -> { shot=time; inspectStart=-1000; }
                case RELOADING -> { reloadStart=time; reloadDuration=Math.clamp(payload.duration(),1,200); inspectStart=-1000; }
                case INSPECTING -> inspectStart=time;
                case HIT -> hit=time;
                case STOP -> { reloadStart=-1000; reloadDuration=0; inspectStart=-1000; }
                default -> {}
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(ArsenalClient::tick);
        HudElementRegistry.addLast(Arsenal.id("weapon_hud"),(context,counter)->hud(context));
    }
    private static boolean drain(KeyBinding binding) { boolean result=false; while(binding.wasPressed()) result=true; return result; }
    private static void reset() { time=0; shot=hit=reloadStart=inspectStart=-1000; reloadDuration=0; ads=previousAds=0; previous=ItemStack.EMPTY; wasArmed=tap=false; world=null; }
    private static void tick(MinecraftClient c) {
        if(c.player==null || c.world==null) { reset(); return; }
        if(world!=c.world) { reset(); world=c.world; }
        if(c.isPaused()) return;
        time++; previousAds=ads;
        ItemStack held=c.player.getMainHandStack(); boolean armed=held.getItem() instanceof GunItem;
        // Only actual item/slot changes reset animation, not server component synchronization.
        if(held.getItem()!=previous.getItem()) { shot=hit=reloadStart=inspectStart=-1000; reloadDuration=0; ads=previousAds=0; }
        previous=held;
        boolean r=drain(reload),m=drain(mode),a=drain(ammo),i=drain(inspect);
        int keys=0;
        if(armed && c.currentScreen==null && !c.player.isSpectator() && c.player.isAlive()) {
            if(c.options.attackKey.isPressed()||tap) keys|=FIRE;
            if(c.options.useKey.isPressed()&&!c.player.isSprinting()) keys|=AIM;
            if(r) keys|=RELOAD; if(m) keys|=MODE; if(a) keys|=AMMO; if(i) keys|=INSPECT;
        }
        tap=false;
        double aim=(keys&AIM)!=0 && !GunItem.reloading(held)?1:0;
        ads+=(aim-ads)*.38;
        if((armed||wasArmed) && ClientPlayNetworking.canSend(Input.ID)) ClientPlayNetworking.send(new Input((byte)keys));
        wasArmed=armed;
    }
    public static boolean holdingGun() { return CLIENT.player!=null && CLIENT.player.getMainHandStack().getItem() instanceof GunItem && !CLIENT.player.isSpectator(); }
    public static boolean ownsMouse() { return holdingGun()&&CLIENT.currentScreen==null; }
    public static void fireTap() { tap=true; }
    public static float aim(float delta) { return (float)(previousAds+(ads-previousAds)*delta); }
    public static float zoom(float delta) {
        if(!holdingGun() || !CLIENT.options.getPerspective().isFirstPerson()) return 1;
        Weapon w=((GunItem)CLIENT.player.getMainHandStack().getItem()).weapon;
        return (float)(1+(w.zoom-1)*aim(delta));
    }
    public static void transform(MatrixStack matrices,float delta,ItemStack stack) {
        if(!(stack.getItem() instanceof GunItem gun) || CLIENT.player==null) return;
        double now=time+delta,recoil=Animation.recoil(now-shot),aim=aim(delta);
        double reload=GunItem.reloading(stack)&&reloadDuration>0?Animation.clamp((now-reloadStart)/reloadDuration,0,1):0;
        double inspect=Animation.clamp((now-inspectStart)/40,0,1);
        double hand=CLIENT.player.getMainArm()==Arm.RIGHT?1:-1;
        // No forced player yaw/pitch. Accessible visual recoil leaves input under player control.
        double lower=Math.sin(reload*Math.PI),look=Math.sin(inspect*Math.PI);
        matrices.translate(-hand*.34*aim, .055*aim-.17*lower, .09*recoil-.13*aim);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(-5*recoil+18*lower)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(hand*(22*lower+35*look))));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(hand*18*look)));
        if(aim<.2 && !GunItem.reloading(stack)) matrices.translate(Math.sin(now*.08)*.003,Math.cos(now*.12)*.002,0);
    }
    private static void hud(DrawContext g) {
        if(!holdingGun() || CLIENT.options.hudHidden || CLIENT.currentScreen!=null) return;
        ItemStack stack=CLIENT.player.getMainHandStack(); GunItem gun=(GunItem)stack.getItem(); Magazine state=gun.magazine(stack);
        int width=CLIENT.getWindow().getScaledWidth(),height=CLIENT.getWindow().getScaledHeight();
        int panelWidth=Math.min(210,width-16),x=width-panelWidth-8,y=height-89;
        g.fill(x,y,width-8,y+58,0xd51c2428); g.fill(x,y,x+2,y+58,0xff80b5b0);
        String name=CLIENT.textRenderer.trimToWidth(stack.getName().getString(),panelWidth-16);
        g.drawText(CLIENT.textRenderer,name,x+9,y+6,0xffe8e9e5,true);
        String status=state.rounds()+" / "+gun.weapon.capacity+"  ·  "+Text.translatable("mode.arsenal."+state.mode().name().toLowerCase(java.util.Locale.ROOT)).getString();
        g.drawText(CLIENT.textRenderer,status,x+9,y+18,state.rounds()==0?0xffffa28b:0xffabd5c4,true);
        String ammoName=Text.translatable("item.arsenal."+state.ammo().id).getString();
        g.drawText(CLIENT.textRenderer,CLIENT.textRenderer.trimToWidth(ammoName,panelWidth-16),x+9,y+30,0xffb8c4c9,true);
        String reserve=CLIENT.player.isCreative()?"∞":Integer.toString(WeaponService.available(CLIENT.player,state.ammo()));
        String bottom=GunItem.reloading(stack)?Text.translatable("hud.arsenal.reload").getString():Text.translatable("hud.arsenal.reserve",reserve).getString();
        g.drawText(CLIENT.textRenderer,CLIENT.textRenderer.trimToWidth(bottom,panelWidth-55),x+9,y+43,0xffacb5b9,true);
        String reloadKey=reload.getBoundKeyLocalizedText().getString();
        g.drawText(CLIENT.textRenderer,"["+reloadKey+"]",width-18-CLIENT.textRenderer.getWidth("["+reloadKey+"]"),y+43,0xffcfdcdd,true);
        if(GunItem.reloading(stack)&&reloadDuration>0) {
            int progress=(int)((panelWidth-4)*Animation.clamp((time-reloadStart)/reloadDuration,0,1));
            g.fill(x+2,y+56,x+2+progress,y+58,0xff80b5b0);
        }
        if(!CLIENT.options.getPerspective().isFirstPerson()) return;
        int cx=width/2,cy=height/2,gap=(int)(3+(1-ads)*gun.weapon.spread+Animation.recoil(time-shot)*6);
        int color=state.rounds()==0?0xffd99a85:0xffdde8de;
        g.fill(cx-gap-5,cy,cx-gap,cy+1,color); g.fill(cx+gap+1,cy,cx+gap+6,cy+1,color);
        g.fill(cx,cy-gap-5,cx+1,cy-gap,color); g.fill(cx,cy+gap+1,cx+1,cy+gap+6,color);
        if(gun.weapon.scoped()&&ads>.8) {
            for(int j=1;j<=4;j++) { int d=j*10; g.fill(cx-d,cy-2,cx-d+1,cy+3,color); g.fill(cx+d,cy-2,cx+d+1,cy+3,color); }
            for(int deg=0;deg<360;deg+=6) { double angle=Math.toRadians(deg); int sx=cx+(int)(Math.cos(angle)*55),sy=cy+(int)(Math.sin(angle)*55); g.fill(sx,sy,sx+2,sy+2,0xaaabc4c7); }
        }
        if(time-hit<6) for(int j=3;j<8;j++) {
            g.fill(cx-j,cy-j,cx-j+1,cy-j+1,0xffffd58a); g.fill(cx+j,cy-j,cx+j+1,cy-j+1,0xffffd58a);
            g.fill(cx-j,cy+j,cx-j+1,cy+j+1,0xffffd58a); g.fill(cx+j,cy+j,cx+j+1,cy+j+1,0xffffd58a);
        }
    }
}
