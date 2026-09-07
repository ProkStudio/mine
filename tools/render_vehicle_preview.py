#!/usr/bin/env python3
"""Textured orthographic QA of generated mesh/atlas data, NOT a Minecraft screenshot.
Usage: python3 tools/render_vehicle_preview.py [vehicle_id ...] [--rear]
Requires Python 3, numpy, Pillow and a Unicode font. Run the asset generator first.
"""
import json, math, sys, subprocess
from pathlib import Path
from functools import lru_cache
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
ROOT=Path(__file__).resolve().parents[1]
MESH=ROOT/'build/preview-meshes'
TEXTURES=ROOT/'build/preview-assets/assets/harvester/textures/vehicle'
OUT=ROOT/'build/previews'
FACES={'down':[0,1,5,4],'up':[2,6,7,3],'west':[0,4,6,2],'north':[0,2,3,1],'east':[1,3,7,5],'south':[4,5,7,6]}
W,H=1400,1000

def rotation(rest):
    a,b,c=np.deg2rad(rest); ca,sa,cb,sb,cc,sc=np.cos(a),np.sin(a),np.cos(b),np.sin(b),np.cos(c),np.sin(c)
    rx=np.array([[1,0,0],[0,ca,-sa],[0,sa,ca]])
    ry=np.array([[cb,0,sb],[0,1,0],[-sb,0,cb]])
    rz=np.array([[cc,-sc,0],[sc,cc,0],[0,0,1]])
    return rz@ry@rx

def track_angle(index):
    t=(index*(56+12*math.pi)/32)%(56+12*math.pi)
    if t<28:return 0
    t-=28
    if t<6*math.pi:return t/6
    t-=6*math.pi
    if t<28:return math.pi
    return math.pi+(t-28)/6

@lru_cache(maxsize=8)
def load_font(size,bold=False):
    try:
        matched=subprocess.check_output(['fc-match','-f','%{file}', 'Arial:style='+('Bold' if bold else 'Regular')],text=True).strip()
    except (FileNotFoundError,subprocess.CalledProcessError):
        matched=''
    candidates=[matched, '/usr/share/fonts/truetype/dejavu/DejaVuSans'+('-Bold' if bold else '')+'.ttf',
                '/usr/share/fonts/dejavu-sans-fonts/DejaVuSans'+('-Bold' if bold else '')+'.ttf',
                '/usr/share/fonts/truetype/liberation2/LiberationSans'+('-Bold' if bold else '-Regular')+'.ttf']
    for p in candidates:
        if p and Path(p).is_file():return ImageFont.truetype(p,size)
    raise RuntimeError('Install fontconfig and a Unicode font such as DejaVu Sans or Liberation Sans.')

def render(path,rear=False):
    mesh=json.loads(path.read_text()); tex=np.asarray(Image.open(TEXTURES/('atlas_'+mesh['id']+'_item.png')).convert('RGBA'))
    camera=np.array([1.15,.80,-1.45 if rear else 1.45]);camera/=np.linalg.norm(camera)
    right=np.cross([0,1,0],camera);right/=np.linalg.norm(right);up=np.cross(camera,right)
    view=np.stack([right,up,camera],axis=1)
    light=np.array([-.35,.86,.46]);light/=np.linalg.norm(light)
    quads=[]; all_points=[]
    for part in mesh['parts']:
        rest=list(part['rest'])
        if part['axis']=='t':rest[0]=math.degrees(track_angle(int(part['name'].split('_')[2])))
        if part['axis']=='n':rest[2]=-120
        rot=rotation(rest);pivot=np.array(part['pivot'])
        for box in part['boxes']:
            start=np.array(box['from']);size=np.array(box['size'])
            verts=np.array([start+size*np.array([i&1,(i>>1)&1,(i>>2)&1]) for i in range(8)])@rot.T+pivot
            all_points.extend(verts@view)
            for face,ids in FACES.items():
                points=verts[ids]; normal=np.cross(points[1]-points[0],points[2]-points[0]);normal/=max(np.linalg.norm(normal),1e-9)
                if np.dot(normal,camera)<=0:continue
                uv=box['uv'][face];uvs=np.array([[uv[0],uv[3]],[uv[2],uv[3]],[uv[2],uv[1]],[uv[0],uv[1]]])
                shade=.64+.36*max(0,np.dot(normal,light))
                quads.append((points@view,uvs,shade))
    all_points=np.array(all_points);xmin,ymin=all_points[:,:2].min(axis=0);xmax,ymax=all_points[:,:2].max(axis=0)
    scale=min((W-170)/(xmax-xmin),(H-300)/(ymax-ymin));cx=(xmin+xmax)/2;cy=(ymin+ymax)/2
    pixels=np.zeros((H,W,4),dtype=np.uint8);depth=np.full((H,W),-1e20,dtype=np.float64)
    for points,uvs,shade in quads:
        screen=np.column_stack([(points[:,0]-cx)*scale+W/2, -(points[:,1]-cy)*scale+H*.54, points[:,2]])
        for idx in ([0,1,2],[0,2,3]):
            p=screen[idx];uv=uvs[idx]
            x0=max(0,int(np.floor(p[:,0].min())));x1=min(W-1,int(np.ceil(p[:,0].max())))
            y0=max(0,int(np.floor(p[:,1].min())));y1=min(H-1,int(np.ceil(p[:,1].max())))
            if x1<x0 or y1<y0:continue
            xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5)
            den=(p[1,1]-p[2,1])*(p[0,0]-p[2,0])+(p[2,0]-p[1,0])*(p[0,1]-p[2,1])
            if abs(den)<1e-8:continue
            a=((p[1,1]-p[2,1])*(xx-p[2,0])+(p[2,0]-p[1,0])*(yy-p[2,1]))/den
            b=((p[2,1]-p[0,1])*(xx-p[2,0])+(p[0,0]-p[2,0])*(yy-p[2,1]))/den;c=1-a-b
            z=a*p[0,2]+b*p[1,2]+c*p[2,2]
            uu=np.clip(np.floor(a*uv[0,0]+b*uv[1,0]+c*uv[2,0]).astype(int),0,tex.shape[1]-1)
            vv=np.clip(np.floor(a*uv[0,1]+b*uv[1,1]+c*uv[2,1]).astype(int),0,tex.shape[0]-1)
            sample=tex[vv,uu];region=depth[y0:y1+1,x0:x1+1]
            mask=(a>=-1e-7)&(b>=-1e-7)&(c>=-1e-7)&(z>region)&(sample[:,:,3]>=128)
            dest=pixels[y0:y1+1,x0:x1+1];dest[mask,:3]=np.clip(sample[mask,:3]*shade,0,255).astype(np.uint8);dest[mask,3]=255;region[mask]=z[mask]
    canvas=Image.new('RGBA',(W,H),'#edf0ed')
    shadow=Image.new('RGBA',(W,H));d=ImageDraw.Draw(shadow)
    d.ellipse((W*.23,H*.75,W*.79,H*.87),fill=(20,38,43,40));shadow=shadow.filter(ImageFilter.GaussianBlur(28));canvas.alpha_composite(shadow)
    canvas.alpha_composite(Image.fromarray(pixels))
    d=ImageDraw.Draw(canvas)
    d.text((56,40),'HARVESTER / TRANSPORT WORKSHOP',font=load_font(17,True),fill='#50616a')
    d.text((52,70),mesh['title']+(' / вид сзади' if rear else ''),font=load_font(40,True),fill='#213440')
    d.line((56,135,W-56,135),fill='#cdd5d3',width=1)
    d.text((56,H-68),'Геометрия и текстуры из исходников • офлайн-превью, не скриншот Minecraft',font=load_font(19),fill='#52616a')
    target=OUT/(mesh['id']+('_rear' if rear else '')+'.png');canvas.convert('RGB').save(target)
    print(target)

if __name__=='__main__':
    if any(x.startswith('--') and x!='--rear' for x in sys.argv[1:]):
        raise SystemExit('Usage: render_vehicle_preview.py [vehicle_id ...] [--rear]')
    rear='--rear' in sys.argv;ids=[x for x in sys.argv[1:] if not x.startswith('--')]
    paths=[MESH/(x+'.json') for x in ids] if ids else sorted(MESH.glob('*.json'))
    if not paths:raise SystemExit('No generated preview meshes. Run bash tests/run-visual-smoke.sh --previews.')
    OUT.mkdir(parents=True,exist_ok=True)
    for path in paths:render(path,rear)
