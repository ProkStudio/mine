import com.harvester.vehicle.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.*;

/** Dependency-free original art pipeline. Runtime, item models and preview meshes share geometry/UVs. */
public final class VehicleAssetGenerator {
    private static final List<String> FACES=List.of("down","up","west","north","east","south");
    private final Path root,previews;
    private final Map<String,Object> files=new LinkedHashMap<>();
    private VehicleAssetGenerator(Path output,Path previews) { root=output.resolve("assets/harvester"); this.previews=previews; }
    public static void main(String[] args) throws Exception {
        if(args.length<1 || args.length>2) throw new IllegalArgumentException("Expected resource output and optional preview mesh output");
        new VehicleAssetGenerator(Path.of(args[0]),args.length==2?Path.of(args[1]):null).run();
    }
    private void run() throws Exception {
        // Keep the old material paths for external packs, but the entity now uses one atlas per type.
        for(String material:VehicleGeometry.MATERIALS) {
            BufferedImage legacy=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
            paintFace(legacy,0,0,32,32,material,VehicleType.COMBINE,material.hashCode(),false);
            png("textures/vehicle/"+material+".png",legacy);
        }
        Set<String> aliases=new HashSet<>(); List<Object> manifest=new ArrayList<>();
        for(VehicleType type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type); var layout=VehicleAtlas.layout(parts);
            int cubes=parts.stream().mapToInt(p->p.boxes().size()).sum();
            if(cubes<20 || cubes>900) throw new IllegalStateException("Geometry budget: "+type+" = "+cubes);
            atlas(type,parts,layout,false); atlas(type,parts,layout,true);
            model(type,parts,layout); icon(type,parts); definition(type.id,type.id);
            if(aliases.add(type.modelId()) && !type.modelId().equals(type.id)) {
                definition(type.modelId(),type.id);
                json("models/item/"+type.modelId()+"_hand.json",Map.of("parent","harvester:item/"+type.id+"_hand"));
                json("models/item/"+type.modelId()+"_icon.json",Map.of("parent","harvester:item/"+type.id+"_icon"));
            }
            manifest.add(Map.of("id",type.id,"cuboids",cubes,"parts",parts.size(),"atlasSize",layout.size(),"texelsPerModelPixel",VehicleAtlas.DENSITY));
            if(previews!=null) preview(type,parts,layout);
            System.out.println(type.id+": "+cubes+" cuboids, "+parts.size()+" parts, "+layout.size()+"px atlas");
        }
        json("models/item/combine_spawn_egg.json",Map.of("parent","harvester:item/combine_spawn_egg_hand"));
        json("models/item/vehicle_hand.json",Map.of("parent","harvester:item/pickup_hand"));
        for(String id:List.of("fuel_can_small","fuel_can_medium","fuel_can_large","repair_kit","paint")) {
            serviceIcon(id);
            json("models/item/"+id+".json",Map.of("parent","minecraft:item/generated","textures",Map.of("layer0","harvester:item/"+id)));
            json("items/"+id+".json",Map.of("model",Map.of("type","minecraft:model","model","harvester:item/"+id)));
        }
        json("items/fuel_can.json",Map.of("model",Map.of("type","minecraft:model","model","harvester:item/fuel_can_large")));
        json("vehicle-art-manifest.json",Map.of("schema",1,"targetMinecraft","1.21.11","materials",VehicleGeometry.MATERIALS,"vehicles",manifest));
        for(var entry:files.entrySet()) write(root.resolve(entry.getKey()),stringify(entry.getValue())+"\n");
    }
    private void definition(String id,String target) {
        json("items/"+id+".json",Map.of("model",Map.of("type","minecraft:select","property","minecraft:display_context",
            "cases",List.of(Map.of("when","gui","model",Map.of("type","minecraft:model","model","harvester:item/"+target+"_icon"))),
            "fallback",Map.of("type","minecraft:model","model","harvester:item/"+target+"_hand"))));
    }
    private void atlas(VehicleType type,List<VehicleGeometry.Part> parts,VehicleAtlas.Layout layout,boolean colored) throws Exception {
        BufferedImage image=new BufferedImage(layout.size(),layout.size(),BufferedImage.TYPE_INT_ARGB);
        for(var p:parts) for(int index=0;index<p.boxes().size();index++) {
            var box=p.boxes().get(index); var island=layout.island(p.name(),index);
            int base=p.material().equals("paint")?(colored?VehicleGeometry.paintColor(type):0xefefeb):VehicleGeometry.materialColor(p.material(),type);
            for(int y=island.v()-VehicleAtlas.GUTTER;y<island.v()+island.height()+VehicleAtlas.GUTTER;y++)
                for(int x=island.u()-VehicleAtlas.GUTTER;x<island.u()+island.width()+VehicleAtlas.GUTTER;x++)
                    image.setRGB(x,y,p.material().equals("glass")?0:0xff000000|base);
            for(String face:FACES) {
                double[] uv=VehicleAtlas.faceUv(island,box,face);
                int x=(int)Math.floor(Math.min(uv[0],uv[2])),y=(int)Math.floor(Math.min(uv[1],uv[3]));
                int w=Math.max(1,(int)Math.ceil(Math.max(uv[0],uv[2]))-x),h=Math.max(1,(int)Math.ceil(Math.max(uv[1],uv[3]))-y);
                paintFace(image,x,y,w,h,p.material(),type,Objects.hash(p.name(),index,face),colored);
            }
        }
        png("textures/vehicle/atlas_"+type.id+(colored?"_item":"")+".png",image);
    }
    private static void paintFace(BufferedImage image,int x,int y,int w,int h,String material,VehicleType type,int seed,boolean colored) {
        int base=material.equals("paint")?(colored?VehicleGeometry.paintColor(type):0xefefeb):VehicleGeometry.materialColor(material,type);
        for(int yy=0;yy<h;yy++) for(int xx=0;xx<w;xx++) {
            if(x+xx>=image.getWidth() || y+yy>=image.getHeight()) continue;
            int variation=Math.floorMod(seed+xx*13+yy*23,5)-2,alpha=255;
            boolean edge=xx==0 || yy==0 || xx==w-1 || yy==h-1;
            switch(material) {
                case "paint" -> {
                    if(w>=6 && h>=6) { if(edge) variation-=28; else if(yy==1) variation+=9; }
                    if(w>=12 && h>=8 && (yy==3 || yy==h-4) && (xx==3 || xx==w-4 || xx>5 && xx%18==0)) variation-=55;
                }
                case "metal", "brass" -> { variation+=(yy%5==0?9:0); if(edge && w>4 && h>4) variation-=18; }
                case "rubber" -> { if((xx+yy/2)%8<2) variation-=11; if(edge) variation+=5; }
                case "seat" -> { if(w>8 && h>6 && yy%9==2) variation-=16; if((xx==2 || xx==w-3) && yy%3==0) variation+=28; }
                case "wood" -> variation+=(yy%6<2?-17:5);
                case "gauge" -> { if(edge) variation+=70; if(yy==2 && xx%3==0) variation+=120; }
                case "glass" -> {
                    // Binary cutout: the cockpit stays genuinely see-through on the standard entity layer.
                    alpha=edge || w>=8 && h>=8 && Math.abs(xx-yy-w/3)<1?255:0;
                    variation=edge?-12:50;
                }
                case "lamp", "red" -> { if(!edge) variation+=18; if(yy%4==0) variation-=12; }
                case "trim" -> { if(edge && w>4 && h>4) variation-=13; }
                default -> { if(edge && w>5 && h>5) variation-=13; }
            }
            image.setRGB(x+xx,y+yy,(alpha<<24)|adjust(base,variation));
        }
    }
    private static int adjust(int rgb,int n) {
        return Math.clamp((rgb>>16&255)+n,0,255)<<16|Math.clamp((rgb>>8&255)+n,0,255)<<8|Math.clamp((rgb&255)+n,0,255);
    }
    private static double[] rotate(double x,double y,double z,VehicleGeometry.Part p) {
        double a=Math.toRadians(p.restPitch()),b=Math.toRadians(p.restYaw()),c=Math.toRadians(p.restRoll());
        double yy=y*Math.cos(a)-z*Math.sin(a),zz=y*Math.sin(a)+z*Math.cos(a);
        double xx=x*Math.cos(b)+zz*Math.sin(b); zz=-x*Math.sin(b)+zz*Math.cos(b);
        return new double[]{p.px()+xx*Math.cos(c)-yy*Math.sin(c),p.py()+xx*Math.sin(c)+yy*Math.cos(c),p.pz()+zz};
    }
    private static double[][] vertices(VehicleGeometry.Part p,VehicleGeometry.Cuboid b) {
        double[][] result=new double[8][];
        for(int i=0;i<8;i++) result[i]=rotate(b.x()+((i&1)==0?0:b.w()),b.y()+((i&2)==0?0:b.h()),b.z()+((i&4)==0?0:b.d()),p);
        return result;
    }
    private static double[] bounds(List<VehicleGeometry.Part> parts) {
        double[] r={1e9,1e9,1e9,-1e9,-1e9,-1e9};
        for(var p:parts) for(var b:p.boxes()) for(var v:vertices(p,b)) for(int k=0;k<3;k++) { r[k]=Math.min(r[k],v[k]);r[k+3]=Math.max(r[k+3],v[k]); }
        return r;
    }
    private void model(VehicleType type,List<VehicleGeometry.Part> parts,VehicleAtlas.Layout layout) {
        double[] bounds=bounds(parts); double scale=14/Math.max(bounds[3]-bounds[0],Math.max(bounds[4]-bounds[1],bounds[5]-bounds[2]));
        double cx=(bounds[0]+bounds[3])/2,cz=(bounds[2]+bounds[5])/2;
        List<Object> elements=new ArrayList<>();
        for(var p:parts) for(int i=0;i<p.boxes().size();i++) {
            var b=p.boxes().get(i); Map<String,Object> faces=new LinkedHashMap<>();
            for(String face:FACES) {
                double[] uv=VehicleAtlas.faceUv(layout.island(p.name(),i),b,face);
                faces.put(face,Map.of("texture","#atlas","uv",List.of(uv[0]*16/layout.size(),uv[1]*16/layout.size(),uv[2]*16/layout.size(),uv[3]*16/layout.size())));
            }
            Map<String,Object> element=new LinkedHashMap<>();
            element.put("from",List.of(8+(b.x()+p.px()-cx)*scale,1+(b.y()+p.py()-bounds[1])*scale,8+(b.z()+p.pz()-cz)*scale));
            element.put("to",List.of(8+(b.x()+b.w()+p.px()-cx)*scale,1+(b.y()+b.h()+p.py()-bounds[1])*scale,8+(b.z()+b.d()+p.pz()-cz)*scale));
            int axes=(p.restPitch()!=0?1:0)+(p.restYaw()!=0?1:0)+(p.restRoll()!=0?1:0);
            if(axes>1) throw new IllegalStateException("Item model requires a single rest axis: "+p.name());
            if(axes==1) {
                double angle=p.restPitch()!=0?p.restPitch():p.restYaw()!=0?p.restYaw():p.restRoll();
                if(!Set.of(-45.0,-22.5,0.0,22.5,45.0).contains(angle)) throw new IllegalStateException("Nonportable item angle: "+p.name());
                element.put("rotation",Map.of("origin",List.of(8+(p.px()-cx)*scale,1+(p.py()-bounds[1])*scale,8+(p.pz()-cz)*scale),
                    "axis",p.restPitch()!=0?"x":p.restYaw()!=0?"y":"z","angle",angle,"rescale",false));
            }
            element.put("faces",faces); elements.add(element);
        }
        Map<String,Object> display=new LinkedHashMap<>();
        display.put("firstperson_righthand",transform(List.of(12,140,0),List.of(0,1,0),.6));
        display.put("firstperson_lefthand",transform(List.of(12,220,0),List.of(0,1,0),.6));
        display.put("thirdperson_righthand",transform(List.of(60,0,0),List.of(0,2,0),.45));
        display.put("thirdperson_lefthand",transform(List.of(60,0,0),List.of(0,2,0),.45));
        display.put("ground",transform(List.of(0,0,0),List.of(0,1,0),.5));
        display.put("fixed",transform(List.of(0,180,0),List.of(0,0,0),.8));
        json("models/item/"+type.id+"_hand.json",Map.of("textures",Map.of("atlas","harvester:vehicle/atlas_"+type.id+"_item","particle","harvester:vehicle/atlas_"+type.id+"_item"),"elements",elements,"display",display));
        json("models/item/"+type.id+"_icon.json",Map.of("parent","minecraft:item/generated","textures",Map.of("layer0","harvester:item/"+type.id)));
    }
    private Map<String,Object> transform(List<Integer> rotation,List<Integer> translation,double scale) { return Map.of("rotation",rotation,"translation",translation,"scale",List.of(scale,scale,scale)); }
    private record Face(double[][] v,int rgb,double depth) {}
    private void icon(VehicleType type,List<VehicleGeometry.Part> parts) throws Exception {
        List<Face> faces=new ArrayList<>();
        // Geometry-derived inventory miniatures, including the static rest rotations.
        int[][] indices={{2,3,7,6},{1,5,7,3},{4,6,7,5}};
        for(var p:parts) if(!p.material().equals("glass")) for(var box:p.boxes()) {
            var v=vertices(p,box); int base=VehicleGeometry.materialColor(p.material(),type),side=0;
            for(var ids:indices) {
                double[][] corners=new double[4][]; double depth=0;
                for(int k=0;k<4;k++) { corners[k]=v[ids[k]]; depth+=corners[k][0]+corners[k][1]+corners[k][2]; }
                faces.add(new Face(corners,adjust(base,side++==0?18:side==2?-28:0),depth/4));
            }
        }
        faces.sort(Comparator.comparingDouble(Face::depth));
        double x0=1e9,y0=1e9,x1=-1e9,y1=-1e9;
        for(var f:faces) for(var v:f.v()) { var p=project(v); x0=Math.min(x0,p[0]);x1=Math.max(x1,p[0]);y0=Math.min(y0,p[1]);y1=Math.max(y1,p[1]); }
        int size=128; double scale=112/Math.max(x1-x0,y1-y0),ox=(size-(x1-x0)*scale)/2,oy=(size-(y1-y0)*scale)/2;
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics();
        for(var f:faces) {
            Polygon polygon=new Polygon(); for(var v:f.v()) { var p=project(v); polygon.addPoint((int)Math.round(ox+(p[0]-x0)*scale),(int)Math.round(oy+(p[1]-y0)*scale)); }
            g.setColor(new Color(f.rgb()));g.fillPolygon(polygon);
        }
        g.dispose(); png("textures/item/"+type.id+".png",image);
    }
    private static double[] project(double[] p) { return new double[]{(p[0]-p[2])*.866,(p[0]+p[2])*.5-p[1]}; }
    private void preview(VehicleType type,List<VehicleGeometry.Part> parts,VehicleAtlas.Layout layout) throws Exception {
        List<Object> exported=new ArrayList<>();
        for(var p:parts) {
            List<Object> boxes=new ArrayList<>();
            for(int i=0;i<p.boxes().size();i++) {
                var b=p.boxes().get(i); Map<String,Object> uv=new LinkedHashMap<>();
                for(String f:FACES) { double[] v=VehicleAtlas.faceUv(layout.island(p.name(),i),b,f);uv.put(f,List.of(v[0],v[1],v[2],v[3])); }
                boxes.add(Map.of("from",List.of(b.x(),b.y(),b.z()),"size",List.of(b.w(),b.h(),b.d()),"uv",uv));
            }
            exported.add(Map.of("name",p.name(),"material",p.material(),"pivot",List.of(p.px(),p.py(),p.pz()),
                "rest",List.of(p.restPitch(),p.restYaw(),p.restRoll()),"axis",String.valueOf(p.axis()),"boxes",boxes));
        }
        write(previews.resolve(type.id+".json"),stringify(Map.of("id",type.id,"title",type.displayName,"atlasSize",layout.size(),"parts",exported))+"\n");
    }
    private void serviceIcon(String id) throws Exception {
        BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics();g.setColor(new Color(0x20272c));
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
        g.dispose(); png("textures/item/"+id+".png",image);
    }
    private void png(String relative,BufferedImage image) throws Exception { Path target=root.resolve(relative);Files.createDirectories(target.getParent());if(!ImageIO.write(image,"png",target.toFile())) throw new IllegalStateException("PNG writer missing"); }
    private void json(String path,Object value) { if(files.putIfAbsent(path,value)!=null) throw new IllegalStateException("Duplicate path: "+path); }
    private static void write(Path path,String text) throws Exception { Files.createDirectories(path.getParent()); Files.writeString(path,text,StandardCharsets.UTF_8); }
    private static String stringify(Object value) {
        if(value instanceof String s) return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        if(value instanceof Map<?,?> map) { Map<String,Object> sorted=new TreeMap<>();map.forEach((k,v)->sorted.put(k.toString(),v));List<String> parts=new ArrayList<>();for(var e:sorted.entrySet()) parts.add(stringify(e.getKey())+":"+stringify(e.getValue()));return "{"+String.join(",",parts)+"}"; }
        if(value instanceof List<?> list) return "["+String.join(",",list.stream().map(VehicleAssetGenerator::stringify).toList())+"]";
        throw new IllegalArgumentException("Unsupported JSON value: "+value);
    }
}
