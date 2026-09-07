package com.harvester.vehicle;

import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VehicleAtlasTest {
    @Test void islandsAreDeterministicUniqueAndInsideBudget() {
        for(var type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type);var layout=VehicleAtlas.layout(parts);
            assertEquals(layout,VehicleAtlas.layout(parts));
            assertTrue(layout.size()<=VehicleAtlas.MAX_SIZE);
            assertEquals(0,layout.size()&(layout.size()-1));
            assertEquals(parts.stream().mapToInt(p->p.boxes().size()).sum(),layout.islands().size());
            for(var island:layout.islands().values()) {
                assertTrue(island.u()>=VehicleAtlas.GUTTER && island.v()>=VehicleAtlas.GUTTER);
                assertTrue(island.u()+island.width()+VehicleAtlas.GUTTER<=layout.size());
                assertTrue(island.v()+island.height()+VehicleAtlas.GUTTER<=layout.size());
            }
        }
    }
    @Test void cuboidIslandsNeverOverlapIncludingTheirGutters() {
        for(var type:VehicleType.values()) {
            var islands=new ArrayList<>(VehicleAtlas.layout(VehicleGeometry.create(type)).islands().values());
            int g=VehicleAtlas.GUTTER;
            for(int i=0;i<islands.size();i++) for(int j=i+1;j<islands.size();j++) {
                var a=islands.get(i);var b=islands.get(j);
                boolean overlap=a.u()-g<b.u()+b.width()+g && a.u()+a.width()+g>b.u()-g
                    && a.v()-g<b.v()+b.height()+g && a.v()+a.height()+g>b.v()-g;
                assertFalse(overlap,type.id+" overlapping islands "+i+","+j);
            }
        }
    }
    @Test void allSixFaceUvsStayInTheirOwnIsland() {
        for(var type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type);var layout=VehicleAtlas.layout(parts);
            for(var part:parts) for(int i=0;i<part.boxes().size();i++) {
                var island=layout.island(part.name(),i);
                for(String face:List.of("down","up","west","north","east","south")) {
                    var uv=VehicleAtlas.faceUv(island,part.boxes().get(i),face);
                    for(int k=0;k<4;k++) {
                        assertTrue(Double.isFinite(uv[k]));
                        assertTrue(uv[k]>=(k%2==0?island.u():island.v())-1e-5);
                        assertTrue(uv[k]<=(k%2==0?island.u()+island.width():island.v()+island.height())+1e-5);
                    }
                }
            }
        }
    }
    @Test void packagedAtlasesMatchTheRuntimeLayoutAndContainCutoutGlass() throws Exception {
        for(var type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type);var layout=VehicleAtlas.layout(parts);
            for(String suffix:List.of("","_item")) {
                try(var in=getClass().getResourceAsStream("/assets/harvester/textures/vehicle/atlas_"+type.id+suffix+".png")) {
                    assertNotNull(in);var image=ImageIO.read(in);assertNotNull(image);
                    assertEquals(layout.size(),image.getWidth());assertEquals(layout.size(),image.getHeight());
                    boolean transparentGlass=false;
                    for(var part:parts) if(part.material().equals("glass")) for(int i=0;i<part.boxes().size();i++) {
                        var island=layout.island(part.name(),i);
                        for(int y=island.v();y<island.v()+island.height();y++) for(int x=island.u();x<island.u()+island.width();x++)
                            if((image.getRGB(x,y)>>>24)==0) transparentGlass=true;
                    }
                    if(parts.stream().anyMatch(p->p.material().equals("glass")))
                        assertTrue(transparentGlass,type.id+" glass must not be opaque");
                }
            }
        }
    }
    @Test void duplicatePartNamesCannotSilentlyAliasUvs() {
        var part=VehicleGeometry.create(VehicleType.PLANE).get(0);
        assertThrows(IllegalArgumentException.class,()->VehicleAtlas.layout(List.of(part,part)));
    }
}
