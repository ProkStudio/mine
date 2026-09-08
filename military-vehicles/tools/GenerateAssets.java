import com.prokstudio.militaryvehicles.core.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Deterministic original CC0 vehicle/item assets and an auditable fleet manifest. */
public final class GenerateAssets {
    private static Path root;
    private static void text(String path,String content) throws Exception {
        Path file=root.resolve(path);Files.createDirectories(file.getParent());Files.writeString(file,content,StandardCharsets.UTF_8);
    }
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);root=Path.of(args[0]).resolve("assets/militaryvehicles");
        for(var entry:VehicleGeometry.MATERIALS.entrySet()) {
            BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
            int rgb=entry.getValue();Random random=new Random(12417L+entry.getKey().hashCode());
            for(int y=0;y<128;y++) for(int x=0;x<128;x++) {
                int delta=random.nextInt(9)-4;
                int r=Math.clamp((rgb>>16&255)+delta,0,255),g=Math.clamp((rgb>>8&255)+delta,0,255),b=Math.clamp((rgb&255)+delta,0,255);
                int alpha=entry.getKey().equals("glass")&&(x+y)%4!=0?0:255;
                image.setRGB(x,y,alpha<<24|r<<16|g<<8|b);
            }
            Path file=root.resolve("textures/item/material/"+entry.getKey()+".png");Files.createDirectories(file.getParent());ImageIO.write(image,"png",file.toFile());
        }
        StringBuilder textures=new StringBuilder("{");
        for(String material:VehicleGeometry.MATERIALS.keySet()) {
            if(textures.length()>1) textures.append(',');
            textures.append('"').append(material).append("\":\"militaryvehicles:item/material/").append(material).append('"');
        }
        textures.append(",\"particle\":\"militaryvehicles:item/material/olive\"}");
        String display="\"display\":{\"gui\":{\"rotation\":[25,225,0],\"translation\":[0,0,0],\"scale\":[1.05,1.05,1.05]},\"ground\":{\"translation\":[0,3,0],\"scale\":[0.5,0.5,0.5]},\"thirdperson_righthand\":{\"rotation\":[75,0,0],\"translation\":[0,2.5,0],\"scale\":[0.45,0.45,0.45]},\"firstperson_righthand\":{\"rotation\":[0,-35,0],\"translation\":[0,1,0],\"scale\":[0.5,0.5,0.5]}}";
        Path diagnostics=root.getParent().getParent().getParent();
        List<String> fleet=new ArrayList<>();
        for(var kind:VehicleKind.values()) {
            List<String> elements=new ArrayList<>(),preview=new ArrayList<>();
            var parts=VehicleGeometry.create(kind);double scale=kind.itemScale();int boxCount=0;
            for(var part:parts) for(var b:part.boxes()) {
                double x=part.x()+b.x(),y=part.y()+b.y(),z=part.z()+b.z();boxCount++;
                elements.add(element(8+x*scale,1+y*scale,8+z*scale,b.w()*scale,b.h()*scale,b.d()*scale,part.material()));
                preview.add(String.format("{\"name\":\"%s\",\"material\":\"%s\",\"pivot\":[%.4f,%.4f,%.4f],\"wheel\":%s,\"front\":%s,\"box\":[%.4f,%.4f,%.4f,%.4f,%.4f,%.4f]}",
                    part.name(),part.material(),part.x(),part.y(),part.z(),part.wheel(),part.front(),x,y,z,b.w(),b.h(),b.d()));
            }
            model(kind.id,textures.toString(),elements,display);
            String geometry="["+String.join(",",preview)+"]";
            Files.writeString(diagnostics.resolve(kind.id+"-geometry.json"),geometry,StandardCharsets.UTF_8);
            if(kind==VehicleKind.TRUCK) Files.writeString(diagnostics.resolve("truck-geometry.json"),geometry,StandardCharsets.UTF_8);
            fleet.add(String.format("{\"id\":\"%s\",\"seats\":%d,\"cargoSlots\":%d,\"tank\":%d,\"condition\":%d,\"forward\":%.5f,\"reverse\":%.5f,\"width\":%.5f,\"height\":%.5f,\"wheels\":%d,\"steeringWheels\":%d,\"parts\":%d,\"boxes\":%d,\"sound\":\"%s\",\"tracked\":%s,\"armed\":%s,\"support\":%s}",
                kind.id,kind.seats.size(),kind.cargoSlots(),kind.tank,kind.condition,kind.handling.forward(),kind.handling.reverse(),kind.width,kind.height,kind.wheels,kind.steeringWheels,parts.size(),boxCount,kind.soundId,kind.tracked(),kind.armed(),kind.support()));
        }
        model("fuel_can",textures.toString(),List.of(element(3,1,5,10,11,6,"olive"),element(4,12,6,2,3,4,"metal"),element(10,12,6,2,3,4,"metal"),element(6,14,6,4,1,4,"metal"),element(4,5,4.8,8,1,6.4,"accent")),display);
        model("repair_kit",textures.toString(),List.of(element(2,2,4,12,8,8,"dark"),element(2,10,4,12,1,8,"metal"),element(5,11,7,1,2,2,"metal"),element(10,11,7,1,2,2,"metal"),element(6,12,7,4,1,2,"metal"),element(7,4,3.8,2,4,.3,"accent"),element(6,5,3.8,4,2,.3,"accent")),display);
        model("vehicle_frame",textures.toString(),List.of(element(3,5,1,2,3,14,"metal"),element(11,5,1,2,3,14,"metal"),element(5,5,3,6,2,2,"dark"),element(5,5,11,6,2,2,"dark"),element(6,7,6,4,3,4,"olive")),display);
        model("vehicle_shell",textures.toString(),List.of(element(5,1,5,6,2,6,"dark"),element(5.5,3,5.5,5,8,5,"accent"),element(6,11,6,4,2,4,"metal"),element(7,13,7,2,2,2,"metal"),element(5.3,5,5.3,5.4,.7,5.4,"olive")),display);
        text("fleet.json","{\"schemaVersion\":1,\"vehicles\":["+String.join(",",fleet)+"]}\n");
        GenerateRecipes.generate(Path.of(args[0]));
        System.out.println("Generated "+VehicleKind.values().length+" vehicle rigs, "+(VehicleKind.values().length+4)+" item models/definitions and "+VehicleGeometry.MATERIALS.size()+" original textures");
    }
    private static void model(String name,String textures,List<String> elements,String display) throws Exception {
        text("models/item/"+name+".json","{\"textures\":"+textures+",\"elements\":["+String.join(",",elements)+"],"+display+"}");
        text("items/"+name+".json","{\"model\":{\"type\":\"minecraft:model\",\"model\":\"militaryvehicles:item/"+name+"\"}}");
    }
    private static String element(double x,double y,double z,double w,double h,double d,String material) {
        if(x<0||y<0||z<0||x+w>16||y+h>16||z+d>16) throw new IllegalArgumentException("Item model exceeds 0..16 bounds");
        StringBuilder faces=new StringBuilder("{");
        for(String face:List.of("north","south","east","west","up","down")) {
            if(faces.length()>1) faces.append(',');
            faces.append('"').append(face).append("\":{\"texture\":\"#").append(material).append("\",\"uv\":[0,0,8,8]}");
        }
        return String.format("{\"from\":[%.4f,%.4f,%.4f],\"to\":[%.4f,%.4f,%.4f],\"faces\":%s}}",x,y,z,x+w,y+h,z+d,faces);
    }
}
