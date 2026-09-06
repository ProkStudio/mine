import dev.mine.arsenal.core.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Original deterministic art and data; generated resources are included in the distributable jar. */
public final class ArsenalAssets {
    private static Path root;
    private static final Map<String,Integer> COLORS=Map.ofEntries(
        Map.entry("steel",0x808b91),Map.entry("dark",0x252e34),Map.entry("rubber",0x353a37),
        Map.entry("glass",0x59a9ba),Map.entry("wood",0x856045),Map.entry("brass",0xb29858),
        Map.entry("copper",0xb4754c),Map.entry("olive",0x727b4c),Map.entry("red",0xb3594a),
        Map.entry("blue",0x64849d),Map.entry("orange",0xd4974b));
    private static final String[] DYES={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    public static void main(String[] args) throws Exception {
        root=Path.of(args[0]); Files.createDirectories(root);
        Map<String,Object> en=new TreeMap<>(),ru=new TreeMap<>();
        for(var entry:COLORS.entrySet()) texture(entry.getKey(),entry.getValue());
        int models=0,parts=0;
        for(Weapon w:Weapon.values()) {
            texture(w.id,w.color);
            List<WeaponGeometry.Part> geometry=WeaponGeometry.create(w); parts+=geometry.size();
            List<Integer> frames=new ArrayList<>(List.of(0,1,2,3,4));
            for(int i=10;i<=17;i++) frames.add(i);
            for(int i=20;i<=27;i++) frames.add(i);
            List<Object> dispatch=new ArrayList<>();
            for(int frame:frames) {
                String id="item/"+w.id+"/"+frame;
                write("assets/arsenal/models/"+id+".json",model(geometry,w,frame,false)); models++;
                if(frame>0) dispatch.add(Map.of("threshold",frame,"model",reference(id)));
            }
            var resting=reference("item/"+w.id+"/0");
            var animated=Map.of("type","minecraft:range_dispatch","property","minecraft:custom_model_data","index",0,"fallback",resting,"entries",dispatch);
            var definition=Map.of("type","minecraft:select","property","minecraft:display_context","cases",List.of(Map.of("when",List.of("firstperson_righthand","firstperson_lefthand","thirdperson_righthand","thirdperson_lefthand"),"model",animated)),"fallback",resting);
            write("assets/arsenal/items/"+w.id+".json",Map.of("model",definition,"hand_animation_on_swap",false));
            en.put("item.arsenal."+w.id,w.en); ru.put("item.arsenal."+w.id,w.ru);
            String metal=w.sidearm()?"minecraft:iron_ingot":w.style==Weapon.Style.RPG||w.style==Weapon.Style.LMG?"minecraft:iron_block":"minecraft:iron_ingot";
            write("data/arsenal/recipe/"+w.id+".json",Map.of("type","minecraft:crafting_shaped","category","equipment","pattern",List.of("IBI","DRS"," I "),
                "key",Map.of("I",metal,"B",w.scoped()?"minecraft:spyglass":"minecraft:copper_ingot","D","minecraft:"+DYES[w.ordinal()]+"_dye","R","minecraft:redstone","S","minecraft:stick"),"result",Map.of("id","arsenal:"+w.id,"count",1)));
            unlock(w.id,"minecraft:iron_ingot");
        }
        for(Ammo a:Ammo.values()) {
            write("assets/arsenal/models/item/"+a.id+".json",model(GrenadeGeometry.ammunition(a),null,0,true)); models++;
            write("assets/arsenal/items/"+a.id+".json",Map.of("model",reference("item/"+a.id)));
            en.put("item.arsenal."+a.id,a.en); ru.put("item.arsenal."+a.id,a.ru);
            write("data/arsenal/recipe/"+a.id+".json",Map.of("type","minecraft:crafting_shapeless","category","misc",
                "ingredients",List.of("minecraft:copper_ingot",a==Ammo.ROCKET_PRACTICE?"minecraft:sand":"minecraft:gunpowder","minecraft:"+DYES[a.ordinal()]+"_dye",a.projectile()?"minecraft:iron_ingot":"minecraft:iron_nugget"),
                "result",Map.of("id","arsenal:"+a.id,"count",a.projectile()?2:16)));
            unlock(a.id,"minecraft:copper_ingot");
        }
        localize(en,ru);
        write("assets/arsenal/lang/en_us.json",en); write("assets/arsenal/lang/ru_ru.json",ru);
        Map<String,Object> sounds=new TreeMap<>();
        for(String name:List.of("pistol","smg","rifle","shotgun","precision","rocket","grenade","reload_out","reload_in","bolt","dry","impact")) {
            List<Object> variants=new ArrayList<>();
            variants.add(Map.of("name","arsenal:"+name,"stream",false,"attenuation_distance",name.equals("impact")?64:40));
            if(List.of("pistol","smg","rifle","shotgun","precision","rocket","grenade").contains(name))
                variants.add(Map.of("name","arsenal:"+name+"_2","stream",false,"attenuation_distance",40));
            sounds.put(name,Map.of("subtitle","subtitles.arsenal."+name,"sounds",variants));
        }
        write("assets/arsenal/sounds.json",sounds);
        for(String type:List.of("kinetic","piercing","blast")) write("data/arsenal/damage_type/"+type+".json",Map.of("message_id","arsenal."+type,"scaling","when_caused_by_living_non_player","exhaustion",.1));
        tag("is_projectile",List.of("arsenal:kinetic","arsenal:piercing"));
        tag("bypasses_armor",List.of("arsenal:piercing")); tag("is_explosion",List.of("arsenal:blast"));
        // Vanilla immunity would otherwise silently throttle fast automatic weapons.
        tag("bypasses_cooldown",List.of("arsenal:kinetic","arsenal:piercing"));
        write("assets/arsenal/art_manifest.json",Map.of("weapons",Weapon.values().length,"ammunition",Ammo.values().length,"models",models,"restingWeaponCuboids",parts,"animationFrames",21,"license","CC0-1.0","generator","ArsenalAssets"));
        System.out.println("Generated "+models+" models, "+parts+" original weapon cuboids, 32 recipes and both localizations");
    }
    private static Map<String,Object> reference(String id) { return Map.of("type","minecraft:model","model","arsenal:"+id); }
    private static Map<String,Object> transform(List<Double> rotation,List<Double> translation,double scale) { return Map.of("rotation",rotation,"translation",translation,"scale",List.of(scale,scale,scale)); }
    private static Map<String,Object> model(List<WeaponGeometry.Part> parts,Weapon w,int frame,boolean ammo) {
        Map<String,Object> textures=new TreeMap<>();
        for(String key:COLORS.keySet()) textures.put(key,"arsenal:item/material/"+key);
        textures.put("paint","arsenal:item/material/"+(w==null?"olive":w.id)); textures.put("particle","#steel");
        List<Object> elements=new ArrayList<>();
        for(var part:parts) {
            double x=part.x(),y=part.y(),z=part.z();
            double reload=Animation.magazineTravel(frame),bolt=Animation.boltTravel(frame);
            if(part.name().equals("mag")) y-=7*reload;
            if(part.name().equals("cylinder")||part.name().equals("drum")) x-=3*reload;
            if(part.name().equals("rocket")||part.name().equals("warhead")) { y-=6*reload; z+=reload; }
            if(part.name().equals("bolt")||part.name().equals("charging_handle")||part.name().startsWith("slide_")) z+=bolt*1.6;
            if(part.name().equals("pump")) z+=bolt*2.5;
            Map<String,Object> faces=new TreeMap<>();
            for(String face:List.of("north","south","east","west","up","down")) faces.put(face,Map.of("texture","#"+part.material(),"uv",List.of(0,0,16,16)));
            elements.add(Map.of("name",part.name(),"from",List.of(x,y,z),"to",List.of(x+part.w(),y+part.h(),z+part.d()),"faces",faces));
        }
        double gui=ammo?.9:w.sidearm()?.7:w.style==Weapon.Style.RPG?.43:.46;
        Map<String,Object> display=new TreeMap<>();
        display.put("gui",transform(List.of(25.,140.,0.),List.of(0.,0.,0.),gui));
        display.put("ground",transform(List.of(0.,0.,90.),List.of(0.,3.5,0.),ammo?.55:.4));
        display.put("fixed",transform(List.of(0.,90.,0.),List.of(0.,0.,0.),ammo?.75:.45));
        display.put("head",transform(List.of(0.,0.,0.),List.of(0.,0.,0.),.35));
        display.put("firstperson_righthand",transform(List.of(0.,0.,0.),List.of(0.,-1.7,-1.5),ammo?.65:w!=null&&w.style==Weapon.Style.RPG?.58:.65));
        display.put("firstperson_lefthand",transform(List.of(0.,0.,0.),List.of(0.,-1.7,-1.5),ammo?.65:w!=null&&w.style==Weapon.Style.RPG?.58:.65));
        display.put("thirdperson_righthand",transform(List.of(0.,0.,0.),List.of(0.,2.5,1.),ammo?.55:.65));
        display.put("thirdperson_lefthand",transform(List.of(0.,0.,0.),List.of(0.,2.5,1.),ammo?.55:.65));
        return Map.of("ambientocclusion",true,"gui_light","side","textures",textures,"display",display,"elements",elements);
    }
    private static void texture(String name,int base) throws Exception {
        BufferedImage image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB); Random rng=new Random(name.hashCode());
        for(int y=0;y<32;y++) for(int x=0;x<32;x++) {
            int noise=rng.nextInt(11)-5;
            if(name.equals("wood")) noise+=(int)(8*Math.sin(y*.7+x*.09));
            if(name.equals("glass")) noise+=(int)((x+y)*.3);
            if((x==1||y==1)&&rng.nextInt(4)!=0) noise+=18;
            int r=Math.clamp(((base>>16)&255)+noise,0,255),g=Math.clamp(((base>>8)&255)+noise,0,255),b=Math.clamp((base&255)+noise,0,255);
            image.setRGB(x,y,0xff000000|(r<<16)|(g<<8)|b);
        }
        Path file=root.resolve("assets/arsenal/textures/item/material/"+name+".png"); Files.createDirectories(file.getParent()); ImageIO.write(image,"PNG",file.toFile());
    }
    private static void unlock(String id,String item) throws Exception {
        write("data/arsenal/advancement/recipes/"+id+".json",Map.of("parent","minecraft:recipes/root","criteria",Map.of("has_material",Map.of("trigger","minecraft:inventory_changed","conditions",Map.of("items",List.of(Map.of("items",item))))),"requirements",List.of(List.of("has_material")),"rewards",Map.of("recipes",List.of("arsenal:"+id))));
    }
    private static void tag(String id,List<String> values) throws Exception { write("data/minecraft/tags/damage_type/"+id+".json",Map.of("replace",false,"values",values)); }
    private static void localize(Map<String,Object> en,Map<String,Object> ru) {
        String[][] lines={
            {"key.arsenal.help","Weapon controls / help","Справка по оружию"},
            {"hud.arsenal.help","[%s] Controls","[%s] Управление"},
            {"help.arsenal.title","%s — close help","%s — закрыть справку"},
            {"help.arsenal.fire","%s — fire","%s — огонь"},
            {"help.arsenal.aim","%s — hold to aim","%s — держать для прицела"},
            {"help.arsenal.reload","%s — reload selected ammo","%s — зарядить выбранный тип"},
            {"help.arsenal.ammo","%s — next ammunition type","%s — следующий тип снаряда"},
            {"help.arsenal.returned","Old rounds return to inventory; overflow drops nearby.","Старые патроны вернутся в инвентарь; лишние выпадут рядом."},
            {"help.arsenal.mode","%s — fire mode","%s — режим огня"},
            {"help.arsenal.inspect","%s — cosmetic inspection","%s — осмотр (только анимация)"},
            {"help.arsenal.throw","%s with a hand grenade — throw","%s с ручной гранатой — бросок"},
            {"tooltip.arsenal.throw","Use — throw grenade","Использовать — бросить"},
            {"tooltip.arsenal.contact","Contact fuse / 2 s in flight","Контактная / 2 с в полёте"},
            {"message.arsenal.single_ammo","This weapon takes one ammo type","У этого оружия один тип патронов"},
            {"message.arsenal.ammo_selected","Selected: %s. Reload to load.","Выбрано: %s. Нажмите перезарядку."},
            {"itemGroup.arsenal","Military Arsenal","Военный арсенал"},
            {"key.category.arsenal.controls","Military Arsenal","Военный арсенал"},
            {"key.arsenal.reload","Reload","Перезарядка"},{"key.arsenal.mode","Cycle fire mode","Режим огня"},
            {"key.arsenal.ammo","Change ammunition","Сменить боеприпасы"},{"key.arsenal.inspect","Inspect weapon","Осмотр оружия"},
            {"mode.arsenal.semi","SEMI","ОДИН"},{"mode.arsenal.auto","AUTO","АВТО"},{"mode.arsenal.burst","BURST ×3","ОЧЕРЕДЬ ×3"},
            {"tooltip.arsenal.magazine","Magazine: %s / %s","Магазин: %s / %s"},
            {"tooltip.arsenal.controls","Controls: see HUD help","Управление: справка в HUD"},
            {"tooltip.arsenal.ammo_cycle","Change ammo; then reload","Смените тип и перезарядите"},
            {"message.arsenal.empty_before_switch","Empty the weapon before changing ammunition","Для смены боеприпасов оружие должно быть пустым"},
            {"message.arsenal.no_ammo","No matching ammunition in inventory","В инвентаре нет подходящих боеприпасов"},
            {"message.arsenal.projectile_limit","Active projectile limit reached","Достигнут лимит активных снарядов"},
            {"hud.arsenal.reload","RELOADING","ПЕРЕЗАРЯДКА"},{"hud.arsenal.empty","EMPTY","ПУСТО"},
            {"hud.arsenal.reserve","Reserve: %s","Запас: %s"},
            {"hud.arsenal.keys","%s Reload · %s Mode · %s Ammo · %s Inspect","%s Зарядить · %s Режим · %s Патроны · %s Осмотр"}
        };
        for(String[] line:lines) { en.put(line[0],line[1]); ru.put(line[0],line[2]); }
        for(String type:List.of("kinetic","piercing","blast")) {
            en.put("death.attack.arsenal."+type,"%1$s was hit by %2$s"); ru.put("death.attack.arsenal."+type,"%1$s попал под огонь %2$s");
            en.put("death.attack.arsenal."+type+".item","%1$s was hit by %2$s using %3$s"); ru.put("death.attack.arsenal."+type+".item","%1$s попал под огонь %2$s: %3$s");
        }
        for(String name:List.of("pistol","smg","rifle","shotgun","precision","rocket","grenade","reload_out","reload_in","bolt","dry","impact")) {
            boolean reload=name.startsWith("reload")||name.equals("bolt");
            en.put("subtitles.arsenal."+name,reload?"Weapon reloading":name.equals("dry")?"Empty weapon clicks":name.equals("impact")?"Ordnance impact":"Weapon fires");
            ru.put("subtitles.arsenal."+name,reload?"Перезарядка оружия":name.equals("dry")?"Щелчок пустого оружия":name.equals("impact")?"Разрыв снаряда":"Выстрел");
        }
    }
    private static void write(String path,Object value) throws Exception {
        Path file=root.resolve(path); Files.createDirectories(file.getParent()); Files.writeString(file,json(value)+"\n",StandardCharsets.UTF_8);
    }
    private static String json(Object value) {
        if(value instanceof String s) return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
        if(value instanceof Map<?,?> m) { List<String> out=new ArrayList<>(); m.entrySet().stream().sorted(Comparator.comparing(e->e.getKey().toString())).forEach(e->out.add(json(e.getKey().toString())+":"+json(e.getValue()))); return "{"+String.join(",",out)+"}"; }
        if(value instanceof Collection<?> c) return "["+String.join(",",c.stream().map(ArsenalAssets::json).toList())+"]";
        return String.valueOf(value);
    }
}
