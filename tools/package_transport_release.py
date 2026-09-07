"""Package only a freshly tested, remapped transport RC. Never publishes anything."""
from pathlib import Path
import hashlib,json,os,re,shutil,struct,subprocess,zipfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
VERSION='1.3.0-rc.1'
PROFILES=('engine','motorcycle','boat','plane','helicopter','drone')
REQUIRED_CLASSES=('com/harvester/entity/CombineEntity.class','com/harvester/client/entity/renderer/CombineRenderer.class','com/harvester/vehicle/VehicleAssembly.class','com/harvester/vehicle/VehicleMechanics.class','com/harvester/vehicle/VehicleSoundEnvelope.class')
def digest(data):return hashlib.sha256(data).hexdigest()
def require(ok,message):
    if not ok:raise RuntimeError(message)
def validate_jar(path):
    require(path.is_file(),'Missing remapped release JAR: '+str(path))
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None,'Corrupt JAR entry')
        names=z.namelist();require(len(names)==len(set(names)),'Duplicate JAR entries')
        mod=json.loads(z.read('fabric.mod.json'))
        require(mod['id']=='harvester' and mod['version']==VERSION,'Wrong mod identity/version')
        require(mod['license']=='CC0-1.0','Metadata must match the repository CC0 license')
        require(mod['depends']['minecraft']=='~1.21.11','Unsupported Minecraft target')
        require('harvester.client.mixins.json' in names,'Missing client mixins')
        require(not any(n.startswith('ws/schild/') or n.lower().endswith(('.exe','.dll','.so','.dylib')) for n in names),'Build-only/native dependency leaked into the mod')
        for name in REQUIRED_CLASSES:
            data=z.read(name);require(data[:4]==b'\xca\xfe\xba\xbe' and struct.unpack('>H',data[6:8])[0]==65,'Wrong or missing Java 21 class: '+name)
        base='assets/harvester/sounds/'
        audio=json.loads(z.read(base+'manifest.json'));require(set(audio)==set(PROFILES),'Wrong audio profile set')
        for name in PROFILES:
            data=z.read(base+name+'.ogg');m=audio[name]
            require(data[:4]==b'OggS' and digest(data)==m['sha256'] and len(data)==m['bytes'],'Packaged audio differs from validated audio: '+name)
            require((m['sampleRate'],m['samples'],m['channels'])==(32000,128000,1),'Wrong audio format: '+name)
            require(-24<m['rmsDb']<-13 and m['peak']<.95 and m['seamDelta']<=.06 and abs(m['dcOffset'])<=.002,'Audio quality contract failed: '+name)
        require('CC0' in z.read(base+'LICENSE.txt').decode(),'Missing original-audio licensing')
    return mod,audio

def main():
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    require(re.fullmatch('[0-9a-f]{40}',commit) is not None,'Invalid source commit')
    require(os.environ.get('GITHUB_SHA',commit)==commit,'Verification and packaging source commits differ')
    require(not subprocess.check_output(['git','status','--porcelain','--untracked-files=no'],cwd=ROOT,text=True).strip(),'Tracked source changed after testing')
    props=dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
    require(props['version']==VERSION and props['minecraft_version']=='1.21.11','Unexpected build configuration')
    jar=ROOT/'build/libs'/('harvester-mod-'+VERSION+'.jar');mod,audio=validate_jar(jar)
    cases=[];xml_files=sorted((ROOT/'build/test-results/test').glob('TEST-*.xml'))
    for path in xml_files:
        suite=ET.parse(path).getroot();cases.extend(suite.iter('testcase'))
    require(len(cases)>=59,'Missing JUnit evidence; run strict verifier first')
    require(not any(c.find(kind) is not None for c in cases for kind in ('failure','error','skipped')),'Failed or skipped JUnit case')
    reports=sorted((ROOT/'build/verification').glob('*-tests.json'))
    require(len(reports)>=10,'Missing strict-verifier reports')
    expected=(ROOT/'tests/expected-polish-meshes.sha256').read_text().splitlines()
    require(len(expected)==16,'Expected all 16 model fingerprints')
    for line in expected:
        sha,name=line.split();require(Path(name).name==name,'Invalid model fingerprint path')
        require(digest((ROOT/'build/polish-meshes'/name).read_bytes())==sha,'Unreviewed geometry change: '+name)
    out=ROOT/'build/release'
    if out.exists():shutil.rmtree(out)
    out.mkdir(parents=True)
    shutil.copyfile(jar,out/jar.name)
    for source,target in [('LICENSE','LICENSE.txt'),('docs/INSTALL-TRANSPORT-RU.md','INSTALL_RU.md'),('docs/TRANSPORT-QA-1.3.md','QA.md')]:shutil.copyfile(ROOT/source,out/target)
    run='https://github.com/'+os.environ.get('GITHUB_REPOSITORY','ProkStudio/mine')+'/actions/runs/'+os.environ.get('GITHUB_RUN_ID','local')
    manifest={'version':VERSION,'sourceCommit':commit,'workflowRun':run,'minecraft':props['minecraft_version'],'java':21,'fabricLoaderBuiltWith':props['loader_version'],'fabricApiBuiltWith':props['fabric_api_version'],'requiredJUnitMethods':59,'observedJUnitCases':len(cases),'geometryVariants':16,'jar':{'file':jar.name,'sha256':digest(jar.read_bytes()),'bytes':jar.stat().st_size},'audio':audio,'minecraftClientAcceptance':'not_run','inGameListening':'not_run','textureVisualAcceptance':'not_run','license':mod['license']}
    (out/'BUILD-INFO.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    notes=(ROOT/'docs/RELEASE-1.3.0-rc.1.md').read_text()+'\n## Сборка этого выпуска\n\n- Исходники: `'+commit+'`\n- CI: '+run+'\n- Обязательных JUnit-методов: 59; реально выполненных test cases: '+str(len(cases))+'.\n- Контрольные суммы JAR/ZIP/документации — в `SHA256SUMS`.\n'
    (out/'RELEASE-NOTES.md').write_text(notes)
    contents={('mods/'+jar.name):jar.read_bytes()}
    for p in out.iterdir():
        if p.suffix!='.jar':contents[p.name]=p.read_bytes()
    for p in reports:contents['verification/'+p.name]=p.read_bytes()
    contents['verification/expected-polish-meshes.sha256']=(ROOT/'tests/expected-polish-meshes.sha256').read_bytes()
    contents['CONTENTS.sha256']=''.join(digest(data)+'  '+name+'\n' for name,data in sorted(contents.items())).encode()
    archive=out/('harvester-transport-'+VERSION+'-mc1.21.11.zip')
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name,data in sorted(contents.items()):
            info=zipfile.ZipInfo('harvester-transport-'+VERSION+'/'+name,(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
            z.writestr(info,data,compresslevel=9)
    with zipfile.ZipFile(archive) as z:
        require(z.testzip() is None,'Corrupt distribution ZIP')
        require(z.read('harvester-transport-'+VERSION+'/mods/'+jar.name)==jar.read_bytes(),'ZIP changed the JAR')
    (out/'SHA256SUMS').write_text(''.join(digest(p.read_bytes())+'  '+p.name+'\n' for p in sorted(out.iterdir()) if p.is_file() and p.name!='SHA256SUMS'))
    print(json.dumps({'releaseFiles':sorted(p.name for p in out.iterdir()),'sourceCommit':commit,'junitCases':len(cases),'jarSha256':manifest['jar']['sha256']},indent=2))

if __name__=='__main__':main()
