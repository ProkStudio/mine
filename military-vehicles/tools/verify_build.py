#!/usr/bin/env python3
"""Strict post-build gate for required JUnit methods and installable remapped JAR."""
from pathlib import Path
import json, zipfile, xml.etree.ElementTree as ET, hashlib, struct
root = Path(__file__).resolve().parents[1]
expected = {
'TruckPhysicsTest': {'boundsEveryInputCombination','reverseIsSlower','oppositeDirectionBrakesThroughZero','brakeOverridesThrottle','cannotPivotOrAccelerateInAir','reverseReversesYawResponse','invalidNumbersStopSafely','fuelTransferConservesBothStores'},
'ControlLatchTest': {'onlyOnePacketPerServerTick','heldEngineKeyTogglesOnlyOnce','staleKeysBrakeAndDiscardToggle','invalidBitsCannotRefreshTimeout','resetCannotLeakDriverCommands'},
'VehicleSaveTest': {'roundTripPreservesEverySlotAndFractionalFuel','unsupportedVersionAndTypeFailClosed','missingOrMalformedCargoCannotBecomeEmptyTruck','invalidNumbersRejectedRatherThanSilentlyClamped','inputAndRunningEngineAreNotPersisted','snapshotsOwnTheirCargoList'},
'TruckGeometryTest': {'allPartsHaveUniqueNamesAndKnownMaterials','sixWheelsHaveMatchingHubsAndExactlyTwoSteeringWheels','geometryFitsCollisionAtEveryYaw'},
'ResourceTest': {'languagesHaveMatchingKeysAndPlaceholders','registeredItemsHaveModernDefinitionsAndTextures','manifestPinsTargetAndDoesNotRequireOtherMods'},
'EngineFeedbackTest': {'stoppedIsSilent','startAndStopHaveBoundedRamps','loadChangesPitchAtStandstill','reversingHasSameRoadResponse','invalidInputsRemainFinite','independentLoopsDoNotShareState'},
'TruckFeedbackTest': {'demandRequiresPoweredGroundedUnopposedThrottle','priorityIsBoundedStableAndDeduplicated','ownTruckOutranksNearerLoops','invalidAndDistantCandidatesAreIgnored','interpolationRejectsTeleports','exhaustRatesAndOffGate','exhaustAnchorMatchesGeometryAtAllYaws'},
'AudioResourceTest': {'registeredLoopHasLocalizedSubtitle','generatedLoopIsMonoVorbisAndMatchesManifest','encoderAndRawPcmAreNotRuntimeResources'}}
report = root/'build/verification.json'
report.unlink(missing_ok=True)
seen = {k: set() for k in expected}
cases = 0
for path in (root/'build/test-results/test').glob('TEST-*.xml'):
    suite = ET.parse(path).getroot()
    assert not any(int(suite.get(k, '0')) for k in ['failures','errors','skipped']), path
    for c in suite.iter('testcase'):
        assert not any(c.find(k) is not None for k in ['failure','error','skipped']), path
        name = c.attrib['classname'].split('.')[-1]
        if name in seen:
            seen[name].add(c.attrib['name'].removesuffix('()'))
        cases += 1
assert seen == expected, {'expected': {k: sorted(v) for k,v in expected.items()}, 'actual': {k: sorted(v) for k,v in seen.items()}}
assert cases == sum(map(len, expected.values())), cases
version = [s.split('=',1)[1] for s in (root/'gradle.properties').read_text().splitlines() if s.startswith('version=')][0]
jar = root/'build/libs'/f'military-vehicles-{version}.jar'
with zipfile.ZipFile(jar) as a:
    assert a.testzip() is None
    names = set(a.namelist())
    m = json.loads(a.read('fabric.mod.json'))
    assert m['version'] == version and m['id'] == 'militaryvehicles'
    assert m['depends']['minecraft'] == '=1.21.11' and m['depends']['fabricloader'] == '>=0.19.5'
    for clazz in ['entity/TruckEntity', 'entity/TruckExhaust', 'client/TruckRenderer', 'client/TruckAudio', 'client/TruckEngineSound', 'core/EngineFeedback', 'core/TruckFeedback', 'init/MilitarySounds']:
        assert f'com/prokstudio/militaryvehicles/{clazz}.class' in names, clazz
    assert not any(n.startswith(('com/harvester/', 'ws/schild/')) or n.endswith(('.exe','.dll','.so','.dylib','.wav','.pcm')) for n in names)
    assert 'LICENSE_military-vehicles' in names
    assert m['license'] == 'CC0-1.0'
    for n in names:
        if n.endswith('.json'):
            json.loads(a.read(n))
    for item in ['truck_6x6','fuel_can','repair_kit']:
        assert f'assets/militaryvehicles/items/{item}.json' in names
        model = json.loads(a.read(f'assets/militaryvehicles/models/item/{item}.json'))
        for texture in model['textures'].values():
            ns, path = texture.split(':',1)
            assert ns == 'militaryvehicles' and path.startswith('item/')
            assert a.read(f'assets/{ns}/textures/{path}.png').startswith(b'\x89PNG\r\n\x1a\n')
        for e in model['elements']:
            assert all(0 <= lo < hi <= 16 for lo,hi in zip(e['from'], e['to']))
    ru = json.loads(a.read('assets/militaryvehicles/lang/ru_ru.json'))
    en = json.loads(a.read('assets/militaryvehicles/lang/en_us.json'))
    assert ru.keys() == en.keys() and all(ru[k].count('%s') == en[k].count('%s') for k in ru)
    base = 'assets/militaryvehicles/'
    sounds = json.loads(a.read(base+'sounds.json'))
    assert set(sounds) == {'truck_engine'}
    assert sounds['truck_engine']['subtitle'] in ru and sounds['truck_engine']['subtitle'] in en
    assert sounds['truck_engine']['sounds'] == [{'name':'militaryvehicles:truck_engine','attenuation_distance':32}]
    ogg = a.read(base+'sounds/truck_engine.ogg')
    assert ogg.startswith(b'OggS')
    ident = ogg.find(b'\x01vorbis', 0, 128)
    assert ident >= 0 and ogg[ident+11] == 1 and struct.unpack_from('<I', ogg, ident+12)[0] == 32000
    audio = json.loads(a.read(base+'sounds/manifest.json'))['truck_engine']
    assert audio['sha256'] == hashlib.sha256(ogg).hexdigest() and audio['bytes'] == len(ogg)
    assert audio['samples'] == 128000 and audio['channels'] == 1 and audio['sampleRate'] == 32000
    assert .07 <= audio['rms'] <= .20 and 0 < audio['peak'] < .95 and 0 <= audio['seamDelta'] <= .06 and 0 <= audio['dcOffset'] <= .002
    assert b'CC0-1.0' in a.read(base+'sounds/LICENSE.txt')
result = {'status':'PASS','junit_cases':cases,'jar':jar.name,'bytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'audio':audio,'minecraft_runtime_tested':False}
report.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2))
