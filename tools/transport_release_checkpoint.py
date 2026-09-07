"""Persist bounded CI evidence in this public repository; never logs credentials."""
import base64,json,os,pathlib,re,urllib.request,urllib.error,urllib.parse
repo=os.environ['GITHUB_REPOSITORY'];branch=os.environ['GITHUB_REF_NAME']
assert repo=='ProkStudio/mine' and branch=='feature/vehicle-art-pass-20260907'
headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Accept':'application/vnd.github+json','Content-Type':'application/json'}
base='https://api.github.com/repos/'+repo

def put(path,content):
    assert path.startswith('docs/TRANSPORT-')
    url=base+'/contents/'+path
    payload={'message':'docs: persist transport release CI checkpoint','branch':branch,'content':base64.b64encode(content.encode()).decode()}
    try:
        request=urllib.request.Request(url+'?ref='+urllib.parse.quote(branch,safe=''),headers=headers)
        payload['sha']=json.load(urllib.request.urlopen(request,timeout=30))['sha']
    except urllib.error.HTTPError as error:
        if error.code!=404:raise
    urllib.request.urlopen(urllib.request.Request(url,data=json.dumps(payload).encode(),headers=headers,method='PUT'),timeout=30).read()

def bounded(path):
    if not path.exists():return 'No log was produced. Inspect the linked Actions run.\n'
    text=re.sub(r'\x1b\[[0-9;]*m','',path.read_text(errors='replace'));lines=text.splitlines()
    if len(lines)>230:lines=lines[:50]+['... bounded diagnostic excerpt ...']+lines[-180:]
    return '\n'.join(lines)[:26000]+'\n'

run='https://github.com/'+repo+'/actions/runs/'+os.environ['GITHUB_RUN_ID']
record={'sourceCommit':os.environ['GITHUB_SHA'],'runUrl':run,'jobOutcome':os.environ.get('RELEASE_JOB_OUTCOME','unknown'),'releaseTag':os.environ['RELEASE_TAG'],'releasePublished':os.environ.get('RELEASE_PUBLISHED')=='true','minecraftClientAcceptance':'not_run','inGameListening':'not_run'}
info=pathlib.Path('build/release/BUILD-INFO.json')
if info.exists():record['buildInfo']=json.loads(info.read_text())
if record['releasePublished']:
    request=urllib.request.Request(base+'/releases/tags/'+os.environ['RELEASE_TAG'],headers=headers)
    release=json.load(urllib.request.urlopen(request,timeout=30))
    assert not release['draft'] and release['prerelease']
    record['releaseUrl']=release['html_url'];record['assets']=[{'name':a['name'],'bytes':a['size'],'digest':a.get('digest'),'url':a['browser_download_url']} for a in release['assets']]
put('docs/TRANSPORT-RELEASE-CHECKPOINT.json',json.dumps(record,ensure_ascii=False,indent=2)+'\n')
temp=pathlib.Path(os.environ['RUNNER_TEMP'])
put('docs/TRANSPORT-CI-LAST.log','Release run: '+run+'\nSource: '+os.environ['GITHUB_SHA']+'\nOutcome: '+record['jobOutcome']+'\n'+bounded(temp/'transport-release.log'))
put('docs/TRANSPORT-CI-PR-DIAGNOSTIC.log','Historical failed PR run, NOT the release build: https://github.com/ProkStudio/mine/actions/runs/34141419192\n'+bounded(temp/'transport-pr-diagnostic.log'))
print('Saved release checkpoint and bounded diagnostics.')
