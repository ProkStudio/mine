package com.harvester.vehicle;

import java.util.*;

/** Deterministic box-net atlas shared by the runtime baker and offline asset generator.
 * Every cuboid owns an island with a two-texel gutter. No dependency on a block/item atlas,
 * shader offsets, texture binding order or renderer internals. Units are model pixels.
 */
public final class VehicleAtlas {
    public static final int DENSITY=2, GUTTER=2, MAX_SIZE=1024;
    public record Key(String part,int box) {}
    public record Island(int u,int v,int width,int height) {}
    public record Layout(int size,Map<Key,Island> islands) {
        public Island island(String part,int box) {
            Island island=islands.get(new Key(part,box));
            if(island==null) throw new IllegalArgumentException("Unmapped cuboid: "+part+":"+box);
            return island;
        }
    }
    private record Request(Key key,int width,int height) {}
    private VehicleAtlas() {}
    public static Layout layout(List<VehicleGeometry.Part> parts) {
        List<Request> requests=new ArrayList<>(); Set<String> names=new HashSet<>();
        for(var part:parts) {
            if(!names.add(part.name())) throw new IllegalArgumentException("Duplicate part: "+part.name());
            for(int i=0;i<part.boxes().size();i++) {
                var b=part.boxes().get(i);
                if(!Float.isFinite(b.w()+b.h()+b.d()) || b.w()<=0 || b.h()<=0 || b.d()<=0)
                    throw new IllegalArgumentException("Invalid cuboid: "+part.name());
                requests.add(new Request(new Key(part.name(),i),(int)Math.ceil(2*(b.w()+b.d())*DENSITY)+2*GUTTER,
                    (int)Math.ceil((b.h()+b.d())*DENSITY)+2*GUTTER));
            }
        }
        requests.sort(Comparator.comparingInt(Request::height).reversed()
            .thenComparing(Comparator.comparingInt(Request::width).reversed())
            .thenComparing(r->r.key().part()).thenComparingInt(r->r.key().box()));
        for(int size=128;size<=MAX_SIZE;size*=2) {
            Map<Key,Island> islands=new LinkedHashMap<>(); int x=0,y=0,row=0; boolean fits=true;
            for(var request:requests) {
                if(request.width()>size || request.height()>size) { fits=false; break; }
                if(x+request.width()>size) { x=0; y+=row; row=0; }
                if(y+request.height()>size) { fits=false; break; }
                islands.put(request.key(),new Island(x+GUTTER,y+GUTTER,request.width()-2*GUTTER,request.height()-2*GUTTER));
                x+=request.width(); row=Math.max(row,request.height());
            }
            if(fits) return new Layout(size,Collections.unmodifiableMap(islands));
        }
        throw new IllegalArgumentException("Vehicle exceeds the "+MAX_SIZE+"px atlas budget");
    }
    /** Vanilla ModelPart box UV convention; up is deliberately V-flipped. */
    public static double[] faceUv(Island i,VehicleGeometry.Cuboid b,String face) {
        double u=i.u(),v=i.v(),w=b.w()*DENSITY,h=b.h()*DENSITY,d=b.d()*DENSITY;
        return switch(face) {
            case "down" -> new double[]{u+d,v,u+d+w,v+d};
            case "up" -> new double[]{u+d+w,v+d,u+d+2*w,v};
            case "west" -> new double[]{u,v+d,u+d,v+d+h};
            case "north" -> new double[]{u+d,v+d,u+d+w,v+d+h};
            case "east" -> new double[]{u+d+w,v+d,u+2*d+w,v+d+h};
            case "south" -> new double[]{u+2*d+w,v+d,u+2*d+2*w,v+d+h};
            default -> throw new IllegalArgumentException("Unknown face: "+face);
        };
    }
}
