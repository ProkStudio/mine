"""Strict standalone validation; no third-party Python packages required."""
import json,struct,sys
from pathlib import Path
root=Path(sys.argv[1]);base=root/'assets/arsenal'
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
manifest=read(base/'art_manifest.json')
assert manifest['weapons']==16 and manifest['ammunition']==16 and manifest['models']==352
items=list((base/'items').glob('*.json'));models=list((base/'models').rglob('*.json'))
assert len(items)==32 and len(models)==352
seen=set()
def node(n):
 if isinstance(n,dict):
  if n.get('type')=='minecraft:model':
   assert n['model'].startswith('arsenal:')
   assert (base/'models'/(n['model'].split(':',1)[1]+'.json')).is_file(),n
  for v in n.values():node(v)
 elif isinstance(n,list):
  for v in n:node(v)
for path in items:node(read(path))
required={'gui','ground','fixed','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'}
for path in models:
 model=read(path);assert required<=model['display'].keys(),path
 for e in model['elements']:
  assert all(-16<=a<b<=32 for a,b in zip(e['from'],e['to'])),(path,e)
  for face in e['faces'].values():assert face['texture'][1:] in model['textures'],(path,face)
 for texture in model['textures'].values():
  if texture.startswith('#'):continue
  assert texture.startswith('arsenal:item/material/'), ('outside vanilla item atlas',texture)
  png=base/'textures'/(texture.split(':',1)[1]+'.png');seen.add(png)
  data=png.read_bytes();assert data[:8]==b'\x89PNG\r\n\x1a\n'
  assert struct.unpack('>II',data[16:24])==(32,32)
en=read(base/'lang/en_us.json');ru=read(base/'lang/ru_ru.json')
assert en.keys()==ru.keys() and all(en.values()) and all(ru.values())
recipes=list((root/'data/arsenal/recipe').glob('*.json'));assert len(recipes)==32
fingerprints=set()
for path in recipes:
 r=read(path);node(r)
 if r['type']=='minecraft:crafting_shaped':
  fp=tuple(tuple(r['key'].get(c,' ') for c in row) for row in r['pattern'])
 else:fp=tuple(sorted(r['ingredients']))
 assert fp not in fingerprints,('ambiguous crafting recipe',path);fingerprints.add(fp)
 assert r['result']['id'].removeprefix('arsenal:') in {p.stem for p in items}
for typ in ('is_projectile','is_explosion','bypasses_armor','bypasses_cooldown'):
 for value in read(root/f'data/minecraft/tags/damage_type/{typ}.json')['values']:
  assert (root/'data/arsenal/damage_type'/(value.split(':')[1]+'.json')).is_file()
props=dict(line.split('=',1) for line in Path('gradle.properties').read_text().splitlines() if '=' in line)
mod=read(Path('src/main/resources/fabric.mod.json'))
assert props['loader_version']=='0.19.2'
assert mod['depends']['fabricloader']=='>=${loader_version}'
for lang in (en,ru):
 assert len(lang['tooltip.arsenal.controls'])<=30
 assert 'key.arsenal.help' in lang
for item in ('frag_grenade','smoke_grenade'):
 assert (base/'items'/f'{item}.json').is_file()
 assert (root/'data/arsenal/recipe'/f'{item}.json').is_file()
print(f'PASS {len(items)} item definitions, {len(models)} 3D models, {len(seen)} textures, all display contexts, 32 unique recipes and RU/EN parity')
