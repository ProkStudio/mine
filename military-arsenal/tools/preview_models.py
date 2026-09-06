"""Optional offline texture-aware orthographic mesh QA. This is NOT a Minecraft screenshot.
Requires Pillow and numpy; neither is required to build or run the mod.
Usage: python3 tools/preview_models.py build/standalone/resources build/previews
"""
import json,sys,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont
root=Path(sys.argv[1]);out=Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=True)
base=root/'assets/arsenal'
try:font=ImageFont.truetype('DejaVuSans.ttf',22);small=ImageFont.truetype('DejaVuSans.ttf',14)
except OSError:font=ImageFont.load_default(size=22);small=ImageFont.load_default(size=14)
cam=np.array([.82,.42,-.7]);cam/=np.linalg.norm(cam)
right=np.cross(np.array([0.,1.,0.]),cam);right/=np.linalg.norm(right)
up=np.cross(cam,right);matrix=np.vstack([right,up,cam])
LIGHT=np.array([-.45,.8,-.5]);LIGHT/=np.linalg.norm(LIGHT)
faces={'north':([0,1,2,3],np.array([0,0,-1.])), 'south':([5,4,7,6],np.array([0,0,1.])), 'west':([4,0,3,7],np.array([-1.,0,0])), 'east':([1,5,6,2],np.array([1.,0,0])), 'up':([3,2,6,7],np.array([0,1.,0])), 'down':([4,5,1,0],np.array([0,-1.,0]))}
def mesh(model,w=960,h=500):
 canvas=np.full((h,w,3),[238,239,235],dtype=np.uint8);depth=np.full((h,w),-np.inf)
 vertices=[]
 for e in model['elements']:
  x,y,z=e['from'];X,Y,Z=e['to'];vertices.append(np.array([[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],float))
 allv=np.concatenate(vertices)@matrix.T;mins=allv.min(axis=0);maxs=allv.max(axis=0)
 scale=min((w-90)/(maxs[0]-mins[0]),(h-90)/(maxs[1]-mins[1]));center=(mins+maxs)/2
 cache={}
 def tri(v,uv,tex,shade):
  lo=np.maximum(np.floor(v[:,:2].min(axis=0)).astype(int),[0,0]);hi=np.minimum(np.ceil(v[:,:2].max(axis=0)).astype(int),[w-1,h-1])
  if np.any(lo>hi):return
  xs,ys=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
  a,b,c=v;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-7:return
  u=((b[1]-c[1])*(xs-c[0])+(c[0]-b[0])*(ys-c[1]))/den
  vv=((c[1]-a[1])*(xs-c[0])+(a[0]-c[0])*(ys-c[1]))/den;ww=1-u-vv
  z=u*a[2]+vv*b[2]+ww*c[2]
  sl=np.s_[lo[1]:hi[1]+1,lo[0]:hi[0]+1];mask=(u>=-1e-7)&(vv>=-1e-7)&(ww>=-1e-7)&(z>depth[sl])
  tx=np.clip(((u*uv[0,0]+vv*uv[1,0]+ww*uv[2,0])*31).astype(int),0,31)
  ty=np.clip(((u*uv[0,1]+vv*uv[1,1]+ww*uv[2,1])*31).astype(int),0,31)
  color=np.clip(tex[ty,tx]*shade,0,255).astype(np.uint8);canvas[sl][mask]=color[mask];depth[sl][mask]=z[mask]
 for e,verts in zip(model['elements'],vertices):
  projected=verts@matrix.T;projected[:,0]=(projected[:,0]-center[0])*scale+w/2;projected[:,1]=h/2-(projected[:,1]-center[1])*scale
  for side,(indices,normal) in faces.items():
   if np.dot(normal,cam)<=0:continue
   t=e['faces'][side]['texture'][1:];name=model['textures'][t].split(':')[1]
   if name not in cache:cache[name]=np.asarray(Image.open(base/'textures'/(name+'.png')).convert('RGB'),dtype=float)
   shade=.57+.43*max(0,np.dot(normal,LIGHT));v=projected[indices];uv=np.array([[0,1],[1,1],[1,0],[0,0]],float)
   tri(v[[0,1,2]],uv[[0,1,2]],cache[name],shade);tri(v[[0,2,3]],uv[[0,2,3]],cache[name],shade)
 return Image.fromarray(canvas)
lang=json.loads((base/'lang/en_us.json').read_text())
for folder in sorted((base/'models/item').iterdir()):
 if not folder.is_dir():continue
 id=folder.name;model=json.loads((folder/'0.json').read_text());image=Image.new('RGB',(960,620),'#eeefeb');image.paste(mesh(model),(0,72));draw=ImageDraw.Draw(image)
 draw.text((36,22),lang['item.arsenal.'+id],font=font,fill='#27343b')
 draw.text((36,52),'MILITARY ARSENAL  /  ORIGINAL VOXEL MESH',font=small,fill='#6a777e')
 draw.line((36,578,924,578),fill='#c8cfce',width=1)
 draw.text((36,590),f'{len(model["elements"])} cuboids  ·  Textures from generated resources  ·  Offline model preview',font=small,fill='#5a6a6f')
 image.save(out/(id+'.png'))
for path in sorted((base/'models/item').glob('*.json')):
 id=path.stem;model=json.loads(path.read_text());image=Image.new('RGB',(720,460),'#eeefeb');image.paste(mesh(model,720,340),(0,62));d=ImageDraw.Draw(image);d.text((28,20),lang['item.arsenal.'+id],font=font,fill='#27343b');d.text((28,425),'Original 3D ammunition · same mesh used in flight and item views',font=small,fill='#5a6a6f');image.save(out/(id+'.png'))
for id in ('kestrel_p9','breaker_pump','atlas_rpg'):
 for frame in (2,13,17):
  model=json.loads((base/f'models/item/{id}/{frame}.json').read_text());image=mesh(model);d=ImageDraw.Draw(image);d.text((24,18),f'{id} / frame {frame}',font=font,fill='#27343b');image.save(out/f'{id}_frame_{frame}.png')
print('Rendered',len(list(out.glob('*.png'))),'individual model/state previews')
