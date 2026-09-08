#!/usr/bin/env python3
"""Generate an offline geometry preview, not a Minecraft screenshot or runtime test."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
boxes=json.loads((root/'build/generated/truck-geometry.json').read_text(encoding='utf-8'))
html='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Military Vehicles / offline geometry</title>
<style>body{margin:0;background:#f2f3ee;color:#303a31;font-family:Arial,sans-serif}header{margin:24px 36px;padding-bottom:16px;border-bottom:1px solid #ccd2c3}h1{font-size:26px;margin:0 0 8px}p{margin:6px 0;font-size:16px;color:#5d6856}button{padding:10px 18px;min-height:44px;background:#303a31;color:#fff;border:0;border-radius:6px;margin:8px 8px 0 0;cursor:pointer}canvas{display:block;max-width:100%;height:auto;margin:0 auto}footer{margin:8px 36px 24px;font-size:15px;color:#56634e}</style>
<header><h1>Military Vehicles — 6x6 logistics truck</h1><p>0.1.0-alpha.1 / offline geometry preview</p><button onclick="render([1,.7,1.25])">Front</button><button onclick="render([-1,.9,-1.35])">Rear</button></header>
<canvas id="view" width="1100" height="600" aria-label="Offline rendered truck geometry"></canvas>
<footer>Not a Minecraft screenshot. Flat materials; no player, texture filtering or runtime animation.</footer>
<script>
const boxes=BOX_DATA;
const palette={olive:0x68754d,dark:0x303a31,metal:0x596369,rubber:0x252a2d,glass:0x8faeb1,seat:0x394538,light:0xf1d9a1,tail:0xba5946,canvas:0x8a8662,accent:0xc4b68e};
const dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],norm=a=>a.map(x=>x/Math.hypot(...a));
function render(camera){
 const canvas=document.getElementById('view'),W=canvas.width,H=canvas.height,ctx=canvas.getContext('2d');
 const im=ctx.createImageData(W,H),depth=new Float32Array(W*H).fill(-1e9);for(let i=0;i<W*H;i++){im.data.set([242,243,238,255],i*4);}
 const dir=norm(camera),right=norm(cross([0,1,0],dir)),up=cross(dir,right),light=norm([.4,.85,.65]);
 const proj=v=>{let q=[v[0],v[1]-20,v[2]];return [W/2+dot(q,right)*9.2,290-dot(q,up)*9.2,dot(q,dir)];};
 const faces=[[[0,3,2,1],[0,0,-1]],[[4,5,6,7],[0,0,1]],[[0,4,7,3],[-1,0,0]],[[1,2,6,5],[1,0,0]],[[0,1,5,4],[0,-1,0]],[[3,7,6,2],[0,1,0]]];
 const polys=[];
 for(const obj of boxes){const [x,y,z,w,h,d]=obj.box,v=[[x,y,z],[x+w,y,z],[x+w,y+h,z],[x,y+h,z],[x,y,z+d],[x+w,y,z+d],[x+w,y+h,z+d],[x,y+h,z+d]];
 for(const [ids,n] of faces){if(dot(n,dir)<=0)continue;const rgb=palette[obj.material],shade=.57+.43*Math.max(0,dot(n,light));polys.push({v:ids.map(i=>proj(v[i])),c:[rgb>>16&255,rgb>>8&255,rgb&255].map(c=>c*shade),glass:obj.material==='glass'});}}
 polys.sort((a,b)=>a.glass-b.glass);
 for(const poly of polys)for(const ids of [[0,1,2],[0,2,3]]){const [a,b,c]=ids.map(i=>poly.v[i]);
 const minX=Math.max(0,Math.floor(Math.min(a[0],b[0],c[0]))),maxX=Math.min(W-1,Math.ceil(Math.max(a[0],b[0],c[0]))),minY=Math.max(0,Math.floor(Math.min(a[1],b[1],c[1]))),maxY=Math.min(H-1,Math.ceil(Math.max(a[1],b[1],c[1])));
 const den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-9)continue;
 for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){let px=x+.5,py=y+.5,u=((b[1]-c[1])*(px-c[0])+(c[0]-b[0])*(py-c[1]))/den,v=((c[1]-a[1])*(px-c[0])+(a[0]-c[0])*(py-c[1]))/den,w=1-u-v;
 if(u<0||v<0||w<0)continue;let z=u*a[2]+v*b[2]+w*c[2],i=y*W+x;if(z<=depth[i]+1e-6)continue;
 for(let j=0;j<3;j++)im.data[i*4+j]=poly.glass?im.data[i*4+j]*.8+poly.c[j]*.2:poly.c[j];if(!poly.glass)depth[i]=z;}}
 ctx.putImageData(im,0,0);document.body.dataset.view=camera[2]>0?'front':'rear';return boxes.length;
}
render([1,.7,1.25]);</script></html>'''
html=html.replace('BOX_DATA',json.dumps(boxes,separators=(',',':')))
out=root/'build/previews/preview.html';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(html,encoding='utf-8')
print(out)
