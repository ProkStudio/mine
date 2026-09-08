#!/usr/bin/env python3
"""Optional offline geometry QA: pip install numpy Pillow. Not Minecraft or a texture/lighting acceptance test."""
from pathlib import Path
import argparse, json, math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
PALETTE={'olive':0x68754d,'dark':0x303a31,'metal':0x596369,'rubber':0x252a2d,'glass':0x8faeb1,'seat':0x394538,'light':0xf1d9a1,'tail':0xba5946,'canvas':0x8a8662,'accent':0xc4b68e,'sand':0xb09c74,'armor':0x4e6252,'marking':0xdbd6bd}
FACES=[([0,3,2,1],[0,0,-1]),([4,5,6,7],[0,0,1]),([0,4,7,3],[-1,0,0]),([1,2,6,5],[1,0,0]),([0,1,5,4],[0,-1,0]),([3,7,6,2],[0,1,0])]
def norm(v):
 v=np.array(v,dtype=float);return v/np.linalg.norm(v)
def rotation(x,y):
 a,b=math.cos(x),math.sin(x);c,d=math.cos(y),math.sin(y)
 return np.array([[c,0,d],[0,1,0],[-d,0,c]])@np.array([[1,0,0],[0,a,-b],[0,b,a]])
def font(size):
 for name in ['/usr/share/fonts/msttcore/arial.ttf','/usr/share/fonts/truetype/msttcorefonts/Arial.ttf','/usr/share/fonts/liberation-sans/LiberationSans-Regular.ttf']:
  if Path(name).exists():return ImageFont.truetype(name,size)
 return ImageFont.load_default(size=size)
def render(kind,view,wheel=0,steer=0):
 boxes=json.loads((ROOT/f'build/generated/{kind}-geometry.json').read_text())
 W,H=1100,700;bg=np.array([242,243,238],dtype=np.float64)
 pixels=np.broadcast_to(bg,(H,W,3)).copy();depth=np.full((H,W),-np.inf)
 direction=norm([1,.68,1.3] if view=='front' else [-1,.7,-1.25])
 right=norm(np.cross([0,1,0],direction));up=np.cross(direction,right);light=norm([.4,.85,.65])
 meshes=[];projected=[]
 for obj in boxes:
  x,y,z,w,h,d=obj['box'];v=np.array([[x,y,z],[x+w,y,z],[x+w,y+h,z],[x,y+h,z],[x,y,z+d],[x+w,y,z+d],[x+w,y+h,z+d],[x,y+h,z+d]],dtype=float)
  pivot=np.array(obj['pivot']);rot=rotation(wheel,-steer if obj['front'] else 0) if obj['wheel'] else rotation(0,-steer*1.7 if obj['name']=='steering' else 0)
  v=(v-pivot)@rot.T+pivot
  proj=np.stack([v@right,-v@up,v@direction],axis=1)
  projected.append(proj);meshes.append((obj,v,proj,rot))
 allp=np.concatenate(projected);lo=allp.min(axis=0);hi=allp.max(axis=0)
 scale=min((W-160)/(hi[0]-lo[0]),(H-210)/(hi[1]-lo[1]));center=(lo+hi)/2
 polys=[]
 for obj,v,proj,rot in meshes:
  proj=proj.copy();proj[:,0]=(proj[:,0]-center[0])*scale+W/2;proj[:,1]=(proj[:,1]-center[1])*scale+H/2+10
  for ids,n in FACES:
   normal=np.array(n)@rot.T
   if np.dot(normal,direction)<=0:continue
   rgb=PALETTE[obj['material']];color=np.array([rgb>>16&255,rgb>>8&255,rgb&255])*(.61+.39*max(0,np.dot(normal,light)))
   polys.append((obj['material']=='glass',proj[ids],color))
 polys.sort(key=lambda p:p[0])
 for glass,quad,color in polys:
  for ids in [(0,1,2),(0,2,3)]:
   a,b,c=quad[list(ids)];x0=max(0,int(np.floor(min(a[0],b[0],c[0]))));x1=min(W-1,int(np.ceil(max(a[0],b[0],c[0]))));y0=max(0,int(np.floor(min(a[1],b[1],c[1]))));y1=min(H-1,int(np.ceil(max(a[1],b[1],c[1]))))
   if x0>x1 or y0>y1:continue
   den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
   if abs(den)<1e-10:continue
   yy,xx=np.mgrid[y0:y1+1,x0:x1+1];xx=xx+.5;yy=yy+.5
   u=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;v=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;t=1-u-v
   z=u*a[2]+v*b[2]+t*c[2];d=depth[y0:y1+1,x0:x1+1];mask=(u>=0)&(v>=0)&(t>=0)&(z>d+1e-6)
   region=pixels[y0:y1+1,x0:x1+1]
   if glass:region[mask]=region[mask]*.76+color*.24
   else:region[mask]=color;d[mask]=z[mask]
 image=Image.fromarray(np.uint8(np.clip(pixels,0,255)));draw=ImageDraw.Draw(image)
 labels={'truck_6x6':'6x6 logistics truck','scout_buggy':'Scout buggy','carrier_8x8':'8x8 armoured personnel carrier'}
 draw.text((40,25),labels[kind],fill='#303a31',font=font(28));draw.text((40,66),f'0.2.0-alpha.1 / {view} / wheel {wheel:.2f} / steer {steer:.2f}',fill='#5d6856',font=font(16))
 draw.text((40,H-38),'Offline geometry only. Not Minecraft; no player, production textures or runtime lighting.',fill='#5d6856',font=font(16))
 suffix='-motion' if wheel or steer else ''
 out=ROOT/f'build/previews/{kind}-{view}{suffix}.png';out.parent.mkdir(parents=True,exist_ok=True);image.save(out);print(out)
 return out
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--kind',choices=['truck_6x6','scout_buggy','carrier_8x8']);p.add_argument('--view',choices=['front','rear'],default='front');p.add_argument('--wheel',type=float,default=0);p.add_argument('--steer',type=float,default=0);a=p.parse_args()
 if a.kind:render(a.kind,a.view,a.wheel,a.steer)
 else:
  for k in ['truck_6x6','scout_buggy','carrier_8x8']:
   for v in ['front','rear']:render(k,v)
  render('scout_buggy','front',math.pi/4,.58);render('carrier_8x8','front',math.pi/4,.38)
