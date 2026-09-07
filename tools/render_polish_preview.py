"""Geometry-only orthographic review. These are NOT Minecraft screenshots or texture QA."""
import json,math,sys,subprocess
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/polish-previews';OUT.mkdir(parents=True,exist_ok=True)
FONT=subprocess.check_output(['fc-match','-f','%{file}','DejaVu Sans'],text=True)
def font(size):return ImageFont.truetype(FONT,size)
def rotation(axis,deg):
 a=math.radians(deg);c=math.cos(a);s=math.sin(a)
 return np.array([[1,0,0],[0,c,-s],[0,s,c]]) if axis==0 else np.array([[c,0,s],[0,1,0],[-s,0,c]]) if axis==1 else np.array([[c,-s,0],[s,c,0],[0,0,1]])
FACES=((0,2,3,1),(4,5,7,6),(0,4,6,2),(1,3,7,5),(0,1,5,4),(2,6,7,3))
PROJ=np.array([[.866,0,-.866],[.37,-1,.37],[.6,.65,.6]])
LIGHT=np.array([.25,1,.45]);LIGHT/=np.linalg.norm(LIGHT)
def render(path,raised=False):
 doc=json.loads(path.read_text());faces=[]
 for p in doc['parts']:
  if p['material']=='glass':continue
  rx,ry,rz=p['rest'];R=rotation(2,rz)@rotation(1,ry)@rotation(0,rx);pivot=np.array(p['pivot'],float)
  if p['axis']=='w':R=R@rotation(2,(-1 if p['name'].endswith('_-1') else 1)*math.degrees(1.15))
  if p['axis']=='t':
   distance=int(p['name'].split('_')[2])*(56+12*math.pi)/32
   angle=0 if distance<28 else (distance-28)/6 if distance<28+6*math.pi else math.pi if distance<56+6*math.pi else math.pi+(distance-56-6*math.pi)/6
   R=R@rotation(0,math.degrees(angle))
  scalez=1;offset=0
  if raised and (p['axis']=='h' or p['name'].startswith('reel')):pivot[1]+=4
  if raised and p['name'].startswith('hydraulic_header'):
   R=rotation(0,math.degrees(math.atan2(4,8)))
   if '_piston_' in p['name']:offset=math.sqrt(128)*.5;scalez=(math.hypot(4,8)-offset)/(math.sqrt(128)-offset)
  for b in p['boxes']:
   x,y,z,w,h,d=b
   v=np.array([[x+(w if i&1 else 0),y+(h if i&2 else 0),z+(d if i&4 else 0)] for i in range(8)],float)
   v[:,2]=offset+(v[:,2]-offset)*scalez
   v=(v@R.T+pivot)*doc['scale']
   color=np.array([(p['color']>>16)&255,(p['color']>>8)&255,p['color']&255])
   for idx in FACES:
    poly=v[list(idx)];normal=np.cross(poly[1]-poly[0],poly[2]-poly[0]);normal/=max(1e-8,np.linalg.norm(normal))
    shade=.64+.36*abs(normal@LIGHT);faces.append((poly,color*shade))
 points=np.concatenate([v for v,c in faces]);coords=points@PROJ.T
 lo=coords[:,:2].min(0);hi=coords[:,:2].max(0);factor=min(840/(hi[0]-lo[0]),505/(hi[1]-lo[1]));origin=np.array([500-(hi[0]+lo[0])*factor/2,405-(hi[1]+lo[1])*factor/2])
 W,H=1000,730;img=np.full((H,W,3),248,np.uint8);depth=np.full((H,W),-np.inf)
 for v,color in faces:
  vv=v@PROJ.T;vv[:,:2]=vv[:,:2]*factor+origin
  for idx in ((0,1,2),(0,2,3)):
   t=vv[list(idx)];x0=max(0,int(t[:,0].min()));x1=min(W-1,int(t[:,0].max()+1));y0=max(0,int(t[:,1].min()));y1=min(H-1,int(t[:,1].max()+1))
   if x1<x0 or y1<y0:continue
   xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5)
   a,b,c=t;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
   if abs(den)<1e-9:continue
   u=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;vvv=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;ww=1-u-vvv
   z=u*a[2]+vvv*b[2]+ww*c[2];dest=depth[y0:y1+1,x0:x1+1];mask=(u>=0)&(vvv>=0)&(ww>=0)&(z>dest);dest[mask]=z[mask];img[y0:y1+1,x0:x1+1][mask]=np.clip(color,0,255).astype(np.uint8)
 image=Image.fromarray(img);draw=ImageDraw.Draw(image)
 draw.text((40,26),doc['title']+(' — жатка поднята' if raised else ''),font=font(29),fill='#2c2c2b')
 draw.text((40,69),'TRANSPORT 1.3 · GEOMETRY REVIEW · SCALE ×1.125',font=font(15),fill='#666666')
 draw.line((40,105,960,105),fill='#dddddd',width=1)
 draw.text((40,681),'Офлайн-модель из исходников. Не игровой кадр; текстуры и игрок здесь не проверяются.',font=font(16),fill='#666666')
 target=OUT/(path.stem+('-raised' if raised else '')+'.png');image.save(target);print(target.name,flush=True)
if __name__=='__main__':
 paths=[ROOT/'build/polish-meshes'/(name+'.json') for name in sys.argv[1:]] if len(sys.argv)>1 else sorted((ROOT/'build/polish-meshes').glob('*.json'))
 for p in paths:render(p)
 if len(sys.argv)==1:render(ROOT/'build/polish-meshes/combine_spawn_egg.json',True)
