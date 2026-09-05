import com.harvester.vehicle.VehicleGeometry;
import com.harvester.vehicle.VehicleType;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.*;

/** Dependency-free original asset generator. Uses no Minecraft or third-party art. */
public final class VehicleAssetGenerator {
    private final Path root;
    private final Map<String,Object> jsonFiles=new LinkedHashMap<>();
    private VehicleAssetGenerator(Path output) { root=output.resolve("assets/harvester"); }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Expected generated-resource directory");
        new VehicleAssetGenerator(Path.of(args[0])).run();
    }
    private void run() throws Exception {
        for(String material:VehicleGeometry.MATERIALS) texture(material,material.equals("paint")?0xe3e4e0:VehicleGeometry.materialColor(material,VehicleType.COMBINE));
        Set<String> aliases=new HashSet<>();
        for(VehicleType type:VehicleType.values()) {
            texture("paint_"+type.id,VehicleGeometry.paintColor(type));
            List<VehicleGeometry.Part> parts=VehicleGeometry.create(type);
            if(parts.stream().mapToInt(p->p.boxes().size()).sum()<20) throw new IllegalStateException("Undetailed model: "+type);
            model(type,parts); icon(type,parts); definition(type.id,type.id);
            if(aliases.add(type.modelId()) && !type.modelId().equals(type.id)) {
                definition(type.modelId(),type.id);
                json("models/item/"+type.modelId()+"_hand.json",Map.of("parent","harvester:item/"+type.id+"_hand"));
                json("models/item/"+type.modelId()+"_icon.json",Map.of("parent","harvester:item/"+type.id+"_icon"));
            }
        }
        json("models/item/combine_spawn_egg.json",Map.of("parent","harvester:item/combine_spawn_egg_hand"));
        json("models/item/vehicle_hand.json",Map.of("parent","harvester:item/pickup_hand"));
        for(String id:List.of("fuel_can_small","fuel_can_medium","fuel_can_large","repair_kit","paint")) {
            serviceIcon(id);
            json("models/item/"+id+".json",Map.of("parent","minecraft:item/generated","textures",Map.of("layer0","harvester:item/"+id)));
            json("items/"+id+".json",Map.of("model",Map.of("type","minecraft:model","model","harvester:item/"+id)));
        }
        json("items/fuel_can.json",Map.of("model",Map.of("type","minecraft:model","model","harvester:item/fuel_can_large")));
        for(var entry:jsonFiles.entrySet()) write(entry.getKey(),stringify(entry.getValue())+"\n");
        for(VehicleType type:VehicleType.values()) {
            require("items/"+type.id+".json"); require("models/item/"+type.id+"_hand.json"); require("textures/item/"+type.id+".png");
        }
        System.out.println("Generated original assets for "+VehicleType.values().length+" vehicles and five service items");
    }
    private void definition(String item,String target) {
        json("items/"+item+".json",Map.of("model",Map.of("type","minecraft:select","property","minecraft:display_context",
            "cases",List.of(Map.of("when","gui","model",Map.of("type","minecraft:model","model","harvester:item/"+target+"_icon"))),
            "fallback",Map.of("type","minecraft:model","model","harvester:item/"+target+"_hand"))));
    }
    private record Solid(double x,double y,double z,double w,double h,double d,String material) {}
    private List<Solid> solids(List<VehicleGeometry.Part> parts) {
        List<Solid> result=new ArrayList<>();
        for(var p:parts) for(var b:p.boxes()) result.add(new Solid(b.x()+p.px(),b.y()+p.py(),b.z()+p.pz(),b.w(),b.h(),b.d(),p.material()));
        return result;
    }
    private double[] bounds(List<Solid> solids) {
        double[] v={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(Solid b:solids) { v[0]=Math.min(v[0],b.x);v[1]=Math.min(v[1],b.y);v[2]=Math.min(v[2],b.z);v[3]=Math.max(v[3],b.x+b.w);v[4]=Math.max(v[4],b.y+b.h);v[5]=Math.max(v[5],b.z+b.d); }
        return v;
    }
    private void model(VehicleType type,List<VehicleGeometry.Part> parts) {
        List<Solid> solids=solids(parts); double[] b=bounds(solids);
        double scale=14/Math.max(b[3]-b[0],Math.max(b[4]-b[1],b[5]-b[2]));
        double cx=(b[0]+b[3])/2,cz=(b[2]+b[5])/2;
        Map<String,String> textures=new LinkedHashMap<>();
        for(String material:VehicleGeometry.MATERIALS) textures.put(material,"harvester:vehicle/"+(material.equals("paint")?"paint_"+type.id:material));
        textures.put("particle","harvester:vehicle/paint_"+type.id);
        List<Object> elements=new ArrayList<>();
        for(Solid s:solids) {
            Map<String,Object> faces=new LinkedHashMap<>();
            for(String face:List.of("north","south","east","west","up","down")) faces.put(face,Map.of("texture","#"+s.material,"uv",List.of(0,0,16,16)));
            elements.add(Map.of("from",List.of(8+(s.x-cx)*scale,1+(s.y-b[1])*scale,8+(s.z-cz)*scale),
                "to",List.of(8+(s.x+s.w-cx)*scale,1+(s.y+s.h-b[1])*scale,8+(s.z+s.d-cz)*scale),"faces",faces));
        }
        Map<String,Object> display=new LinkedHashMap<>();
        display.put("firstperson_righthand",transform(List.of(12,140,0),List.of(0,1,0),.6));
        display.put("firstperson_lefthand",transform(List.of(12,220,0),List.of(0,1,0),.6));
        display.put("thirdperson_righthand",transform(List.of(60,0,0),List.of(0,2,0),.45));
        display.put("thirdperson_lefthand",transform(List.of(60,0,0),List.of(0,2,0),.45));
        display.put("ground",transform(List.of(0,0,0),List.of(0,1,0),.5));
        display.put("fixed",transform(List.of(0,180,0),List.of(0,0,0),.8));
        json("models/item/"+type.id+"_hand.json",Map.of("textures",textures,"elements",elements,"display",display));
        json("models/item/"+type.id+"_icon.json",Map.of("parent","minecraft:item/generated","textures",Map.of("layer0","harvester:item/"+type.id)));
    }
    private Map<String,Object> transform(List<Integer> rotation,List<Integer> translation,double scale) { return Map.of("rotation",rotation,"translation",translation,"scale",List.of(scale,scale,scale)); }
    private void texture(String name,int rgb) throws IOException {
        BufferedImage image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
            int variation=((x*17+y*31)%7)-3;
            if(name.startsWith("paint")) { if(x%32==0||y%32==0) variation-=20; if(x%32==1||y%32==1) variation+=12; }
            else if(name.equals("metal")) variation+=(y%4==0?7:0);
            else if(name.equals("rubber")) variation+=((x+y)%12<3?-12:3);
            else if(name.equals("glass")) variation+=((x+y)%64<7?30:0);
            image.setRGB(x,y,0xff000000|adjust(rgb,variation));
        }
        png("textures/vehicle/"+name+".png",image);
    }
    private static int adjust(int rgb,int n) { return Math.clamp((rgb>>16&255)+n,0,255)<<16|Math.clamp((rgb>>8&255)+n,0,255)<<8|Math.clamp((rgb&255)+n,0,255); }
    private record Face(double[][] vertices,int color,double depth) {}
    private void icon(VehicleType type,List<VehicleGeometry.Part> parts) throws IOException {
        List<Face> faces=new ArrayList<>();
        for(Solid b:solids(parts)) {
            double x=b.x,y=b.y,z=b.z,X=x+b.w,Y=y+b.h,Z=z+b.d;
            int rgb=VehicleGeometry.materialColor(b.material,type);
            face(faces,new double[][]{{x,Y,z},{X,Y,z},{X,Y,Z},{x,Y,Z}},adjust(rgb,24));
            face(faces,new double[][]{{X,y,z},{X,y,Z},{X,Y,Z},{X,Y,z}},adjust(rgb,-28));
            face(faces,new double[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},rgb);
        }
        faces.sort(Comparator.comparingDouble(Face::depth));
        double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=-minX,maxY=-minX;
        for(Face f:faces) for(double[] v:f.vertices) { double[] p=project(v); minX=Math.min(minX,p[0]);maxX=Math.max(maxX,p[0]);minY=Math.min(minY,p[1]);maxY=Math.max(maxY,p[1]); }
        int size=96; double scale=84/Math.max(maxX-minX,maxY-minY),ox=(size-(maxX-minX)*scale)/2,oy=(size-(maxY-minY)*scale)/2;
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        for(Face f:faces) {
            Polygon polygon=new Polygon();
            for(double[] v:f.vertices) { double[] p=project(v);polygon.addPoint((int)Math.round(ox+(p[0]-minX)*scale),(int)Math.round(oy+(p[1]-minY)*scale)); }
            g.setColor(new Color(f.color));g.fillPolygon(polygon);g.setColor(new Color(adjust(f.color,-18)));g.drawPolygon(polygon);
        }
        g.dispose(); png("textures/item/"+type.id+".png",image);
    }
    private void face(List<Face> faces,double[][] vertices,int rgb) { double depth=0; for(double[] p:vertices) depth+=p[0]+p[1]+p[2];faces.add(new Face(vertices,rgb,depth/vertices.length)); }
    private double[] project(double[] p) { return new double[]{(p[0]-p[2])*.866,(p[0]+p[2])*.5-p[1]}; }
    private void serviceIcon(String id) throws IOException {
        BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setColor(new Color(0x20272c));
        if(id.startsWith("fuel_can")) {
            g.fillRoundRect(16,15,33,44,5,5);g.fillRoundRect(18,6,23,14,4,4);
            g.setComposite(AlphaComposite.Clear);g.fillRect(23,10,13,5);g.setComposite(AlphaComposite.SrcOver);
            int color=id.endsWith("small")?0x627d48:id.endsWith("medium")?0xbb9435:0xa64b38;
            g.setColor(new Color(color));g.fillRect(19,20,27,35);g.setColor(new Color(adjust(color,30)));g.drawLine(22,24,43,49);g.drawLine(43,24,22,49);
            g.setColor(new Color(0x343b40));g.fillRect(40,12,10,6);g.setColor(new Color(0xf0e9ce));g.fillRect(26,30,14,13);
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,10));g.setColor(new Color(0x242b30));g.drawString(id.endsWith("small")?"S":id.endsWith("medium")?"M":"L",29,40);
        } else if(id.equals("repair_kit")) {
            g.fillRoundRect(8,20,48,34,4,4);g.fillRect(23,12,19,10);g.setColor(new Color(0x9f4538));g.fillRect(11,24,42,26);
            g.setColor(new Color(0xd4d9d9));g.fillRect(28,28,7,18);g.fillRect(22,34,19,6);g.fillRect(15,22,4,5);g.fillRect(45,22,4,5);
        } else {
            g.fillRect(15,18,34,35);g.setColor(new Color(0x9ba7ab));g.fillOval(15,11,34,14);g.fillRect(18,20,28,30);
            g.setColor(new Color(0x497c99));g.fillRect(18,30,28,14);g.setColor(new Color(0xd4e5ec));g.drawArc(11,12,42,40,0,180);
            g.setColor(new Color(0xf1ede0));g.fillRect(27,31,10,10);
        }
        g.dispose();png("textures/item/"+id+".png",image);
    }
    private void png(String path,BufferedImage image) throws IOException {
        Path target=root.resolve(path);Files.createDirectories(target.getParent());
        if(!ImageIO.write(image,"png",target.toFile())) throw new IOException("PNG writer unavailable");
    }
    private void json(String path,Object value) {
        if(jsonFiles.containsKey(path)) throw new IllegalStateException("Duplicate generated JSON path: "+path);
        jsonFiles.put(path,value);
    }
    private void write(String path,String text) throws IOException { Path target=root.resolve(path);Files.createDirectories(target.getParent());Files.writeString(target,text,StandardCharsets.UTF_8); }
    private void require(String path) { if(!Files.isRegularFile(root.resolve(path))) throw new IllegalStateException("Missing generated asset: "+path); }
    private static String stringify(Object value) {
        if(value instanceof String s) return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        if(value instanceof Map<?,?> m) { List<String> parts=new ArrayList<>();for(var e:m.entrySet()) parts.add(stringify(e.getKey().toString())+":"+stringify(e.getValue()));return "{"+String.join(",",parts)+"}"; }
        if(value instanceof List<?> list) return "["+String.join(",",list.stream().map(VehicleAssetGenerator::stringify).toList())+"]";
        throw new IllegalArgumentException("Unsupported JSON value: "+value);
    }
}
