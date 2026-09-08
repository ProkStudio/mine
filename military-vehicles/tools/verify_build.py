#!/usr/bin/env python3
"""Strict post-build gate for required JUnit methods and installable remapped JAR."""
from pathlib import Path
import json,zipfile,xml.etree.ElementTree as ET,hashlib
root=Path(__file__).resolve().parents[1]
expected={
'TruckPhysicsTest':{'boundsEveryInputCombination','reverseIsSlower','oppositeDirectionBrakesThroughZero','brakeOverridesThrottle','cannotPivotOrAccelerateInAir','reverseReversesYawResponse','invalidNumbersStopSafely','fuelTransferConservesBothStores'},
'ControlLatchTest':{'onlyOnePacketPerServerTick','heldEngineKeyTogglesOnlyOnce','staleKeysBrakeAndDiscardToggle','invalidBitsCannotRefreshTimeout','resetCannotLeakDriverCommands'},
'VehicleSaveTest':{'roundTripPreservesEverySlotAndFractionalFuel','unsupportedVersionAndTypeFailClosed','missingOrMalformedCargoCannotBecomeEmptyTruck','invalidNumbersRejectedRatherThanSilentlyClamped','inputAndRunningEngineAreNotPersisted','snapshotsOwnTheirCargoList'},
'TruckGeometryTest':{'allPartsHaveUniqueNamesAndKnownMaterials','sixWheelsHaveMatchingHubsAndExactlyTwoSteeringWheels','geometryFitsCollisionAtEveryYaw'},
'ResourceTest':{'languagesHaveMatchingKeysAndPlaceholders','registeredItemsHaveModernDefinitionsAndTextures','manifestPinsTargetAndDoesNotRequireOtherMods'}}
report=root/'build/verification.json';report.unlink(missing_ok=True)
seen={k:set() for k in expected};cases=0
for path in (root/'build/test-results/test').glob('TEST-*.xml'):
 suite=ET.parse(path).getroot()
 assert not any(int(suite.get(k,'0')) for k in ['failures','errors','skipped']),path
 for c in suite.iter('testcase'):
  assert not any(c.find(k) is not None for k in ['failure','error','skipped']),path
  name=c.attrib['classname'].split('.')[-1]
  if name in seen: seen[name].add(c.attrib['name'].removesuffix('()'))
  cases+=1
assert seen==expected,{'expected':{k:sorted(v) for k,v in expected.items()},'actual':{k:sorted(v) for k,v in seen.items()}}
assert cases==sum(map(len,expected.values())),cases
version=[s.split('=',1)[1] for s in (root/'gradle.properties').read_text().splitlines() if s.startswith('version=')][0]
jar=root/'build/libs'/f'military-vehicles-{version}.jar'
with zipfile.ZipFile(jar) as a:
 assert a.testzip() is None
 names=set(a.namelist());m=json.loads(a.read('fabric.mod.json'))
 assert m['version']==version and m['id']=='militaryvehicles'
 assert m['depends']['minecraft']=='=1.21.11' and m['depends']['fabricloader']=='>=0.19.5'
 assert 'com/prokstudio/militaryvehicles/entity/TruckEntity.class' in names
 assert 'com/prokstudio/militaryvehicles/client/TruckRenderer.class' in names
 assert not any(n.startswith('com/harvester/') for n in names)
 assert 'LICENSE_military-vehicles' in names
 for n in names:
  if n.endswith('.json'):json.loads(a.read(n))
 for item in ['truck_6x6','fuel_can','repair_kit']:
  assert f'assets/militaryvehicles/items/{item}.json' in names
  model=json.loads(a.read(f'assets/militaryvehicles/models/item/{item}.json'))
  for texture in model['textures'].values():
   ns,path=texture.split(':',1);assert ns=='militaryvehicles' and path.startswith('item/')
   assert a.read(f'assets/{ns}/textures/{path}.png').startswith(b'\x89PNG\r\n\x1a\n')
  for e in model['elements']:assert all(0<=lo<hi<=16 for lo,hi in zip(e['from'],e['to']))
 ru=json.loads(a.read('assets/militaryvehicles/lang/ru_ru.json'));en=json.loads(a.read('assets/militaryvehicles/lang/en_us.json'))
 assert ru.keys()==en.keys() and all(ru[k].count('%s')==en[k].count('%s') for k in ru)
result={'status':'PASS','junit_cases':cases,'jar':jar.name,'bytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'minecraft_runtime_tested':False}
report.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(json.dumps(result,indent=2))
