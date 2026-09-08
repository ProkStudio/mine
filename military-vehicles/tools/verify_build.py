#!/usr/bin/env python3
"""Strict post-build gate for required JUnit methods and installable remapped JAR."""
from pathlib import Path
import json, zipfile, xml.etree.ElementTree as ET, hashlib, struct
root = Path(__file__).resolve().parents[1]
expected = {
'TruckPhysicsTest': {'boundsEveryInputCombination', 'brakeOverridesThrottle', 'cannotPivotOrAccelerateInAir', 'fuelTransferConservesBothStores', 'invalidNumbersStopSafely', 'oppositeDirectionBrakesThroughZero', 'reverseIsSlower', 'reverseReversesYawResponse'},
'ControlLatchTest': {'heldEngineKeyTogglesOnlyOnce', 'invalidBitsCannotRefreshTimeout', 'onlyOnePacketPerServerTick', 'resetCannotLeakDriverCommands', 'staleKeysBrakeAndDiscardToggle'},
'VehicleSaveTest': {'inputAndRunningEngineAreNotPersisted', 'invalidNumbersRejectedRatherThanSilentlyClamped', 'missingOrMalformedCargoCannotBecomeEmptyTruck', 'roundTripPreservesEverySlotAndFractionalFuel', 'snapshotsOwnTheirCargoList', 'unsupportedVersionAndTypeFailClosed'},
'TruckGeometryTest': {'allPartsHaveUniqueNamesAndKnownMaterials', 'geometryFitsCollisionAtEveryYaw', 'sixWheelsHaveMatchingHubsAndExactlyTwoSteeringWheels'},
'ResourceTest': {'languagesHaveMatchingKeysAndPlaceholders', 'manifestPinsTargetAndDoesNotRequireOtherMods', 'registeredItemsHaveModernDefinitionsAndTextures'},
'EngineFeedbackTest': {'independentLoopsDoNotShareState', 'invalidInputsRemainFinite', 'loadChangesPitchAtStandstill', 'reversingHasSameRoadResponse', 'startAndStopHaveBoundedRamps', 'stoppedIsSilent'},
'TruckFeedbackTest': {'demandRequiresPoweredGroundedUnopposedThrottle', 'exhaustAnchorMatchesGeometryAtAllYaws', 'exhaustRatesAndOffGate', 'interpolationRejectsTeleports', 'invalidAndDistantCandidatesAreIgnored', 'ownTruckOutranksNearerLoops', 'priorityIsBoundedStableAndDeduplicated'},
'AudioResourceTest': {'encoderAndRawPcmAreNotRuntimeResources', 'generatedLoopIsMonoVorbisAndMatchesManifest', 'registeredLoopHasLocalizedSubtitle'},
'FleetProfileTest': {'exhaustAnchorsMatchTheirOwnRigAndRotateAroundYaw', 'handlingAndDurabilityExpressDifferentVehicleRoles', 'legacyTruckProfileMatchesExistingConstants', 'seatLayoutsAreImmutableUniqueAndInsideBodyBounds', 'stableIdsAndCapacitiesCoverThreeDistinctRoles'},
'FleetPhysicsTest': {'brakingAndReverseSteeringWorkForEveryProfile', 'eachVehicleReachesItsOwnForwardAndReverseLimit', 'fuelTransferAndAudioNormalizationFollowSelectedProfile', 'noPowerAirAndInvalidNumbersCannotCreateAcceleration'},
'FleetSaveTest': {'allThreeTypesRoundTripWithoutChangingCargoOrFuelRemainder', 'crossTypeGuardRejectsBeforeStateCanBeApplied', 'directSnapshotsOwnTheirListsAndValidateTheirProfile', 'legacyTruckVersionOneShapeRemainsReadableAndIdentical', 'perTypeBoundsAndWrongCargoSizesReturnCodecErrors', 'unsupportedAndMalformedFieldsNeverBecomeAnEmptyVehicle'},
'FleetGeometryTest': {'allStaticCornersFitHeightAndYawIndependentCollider', 'everyAxleHasMatchingHubsAndCorrectSteeringFlags', 'newSeatCushionsAndControlsMatchActualAttachmentProfiles', 'newWheelsStayAboveGroundAndInsideBoundsWhenAnimated', 'rigsAreDistinctWithUniqueNamesAndKnownMaterials'},
'FleetResourceTest': {'allFiveItemsHaveModernDefinitionsAndDistinctBoundedModels', 'bothLanguagesNameEveryRegisteredVehicleAndCapacityTooltip', 'fleetManifestMatchesTheRuntimeProfilesAndMeshCounts', 'generatedManifestKeepsOriginalTruckAndNewTypesSeparate'}}

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
    for clazz in ['entity/TruckEntity', 'entity/TruckExhaust', 'client/TruckRenderer', 'client/TruckAudio', 'client/TruckEngineSound', 'core/EngineFeedback', 'core/TruckFeedback', 'core/VehicleKind', 'core/VehicleGeometry', 'core/VehicleSaveCodec', 'entity/CargoScreenHandler', 'init/MilitarySounds']:
        assert f'com/prokstudio/militaryvehicles/{clazz}.class' in names, clazz
    assert not any(n.startswith(('com/harvester/', 'ws/schild/')) or n.endswith(('.exe','.dll','.so','.dylib','.wav','.pcm')) for n in names)
    assert 'LICENSE_military-vehicles' in names
    assert m['license'] == 'CC0-1.0'
    for n in names:
        if n.endswith('.json'):
            json.loads(a.read(n))
    expected_items = {'truck_6x6','scout_buggy','carrier_8x8','fuel_can','repair_kit'}
    assert {n.removeprefix('assets/militaryvehicles/items/').removesuffix('.json') for n in names if n.startswith('assets/militaryvehicles/items/') and n.endswith('.json')} == expected_items
    for item in sorted(expected_items):
        assert f'assets/militaryvehicles/items/{item}.json' in names
        definition = json.loads(a.read(f'assets/militaryvehicles/items/{item}.json'))
        assert definition == {'model': {'type': 'minecraft:model', 'model': f'militaryvehicles:item/{item}'}}
        model = json.loads(a.read(f'assets/militaryvehicles/models/item/{item}.json'))
        for texture in model['textures'].values():
            ns, path = texture.split(':',1)
            assert ns == 'militaryvehicles' and path.startswith('item/')
            png = a.read(f'assets/{ns}/textures/{path}.png')
            assert png.startswith(b'\x89PNG\r\n\x1a\n') and struct.unpack_from('>II', png, 16) == (128, 128)
        for e in model['elements']:
            assert all(0 <= lo < hi <= 16 for lo,hi in zip(e['from'], e['to']))
    ru = json.loads(a.read('assets/militaryvehicles/lang/ru_ru.json'))
    en = json.loads(a.read('assets/militaryvehicles/lang/en_us.json'))
    assert ru.keys() == en.keys() and all(ru[k].count('%s') == en[k].count('%s') for k in ru)
    base = 'assets/militaryvehicles/'
    sounds = json.loads(a.read(base+'sounds.json'))
    expected_sounds = {'truck_engine','buggy_engine','carrier_engine'}
    assert set(sounds) == expected_sounds
    audio = json.loads(a.read(base+'sounds/manifest.json'))
    assert set(audio) == expected_sounds
    assert {n.removeprefix(base+'sounds/') for n in names if n.startswith(base+'sounds/') and not n.endswith('/')} == {s+'.ogg' for s in expected_sounds} | {'manifest.json','LICENSE.txt'}
    hashes = set()
    for sound in sorted(expected_sounds):
        assert sounds[sound]['subtitle'] in ru and sounds[sound]['subtitle'] in en
        assert sounds[sound]['sounds'] == [{'name':'militaryvehicles:'+sound,'attenuation_distance':32}]
        ogg = a.read(base+'sounds/'+sound+'.ogg')
        assert ogg.startswith(b'OggS')
        ident = ogg.find(b'\x01vorbis', 0, 128)
        assert ident >= 0 and ogg[ident+11] == 1 and struct.unpack_from('<I', ogg, ident+12)[0] == 32000
        clip = audio[sound]
        assert clip['sha256'] == hashlib.sha256(ogg).hexdigest() and clip['bytes'] == len(ogg)
        assert clip['sha256'] not in hashes
        hashes.add(clip['sha256'])
        assert clip['samples'] == 128000 and clip['channels'] == 1 and clip['sampleRate'] == 32000
        assert .07 <= clip['rms'] <= .20 and 0 < clip['peak'] < .95 and 0 <= clip['seamDelta'] <= .06 and 0 <= clip['dcOffset'] <= .002
    fleet = json.loads(a.read(base+'fleet.json'))
    assert fleet['schemaVersion'] == 1
    vehicles = {v['id']: v for v in fleet['vehicles']}
    assert len(fleet['vehicles']) == len(vehicles) == 3
    expected_fleet = {'truck_6x6': (2,27,2400,200,6,2,'truck_engine'), 'scout_buggy': (2,9,1200,120,4,2,'buggy_engine'), 'carrier_8x8': (6,18,3200,360,8,4,'carrier_engine')}
    assert set(vehicles) == set(expected_fleet)
    for key, spec in expected_fleet.items():
        assert tuple(vehicles[key][field] for field in ('seats','cargoSlots','tank','condition','wheels','steeringWheels','sound')) == spec
        assert vehicles[key]['parts'] > 30 and vehicles[key]['boxes'] > 60
        for prefix in ('item.','entity.'):
            assert prefix+'militaryvehicles.'+key in en
    assert b'CC0-1.0' in a.read(base+'sounds/LICENSE.txt')
result = {'status':'PASS','junit_cases':cases,'jar':jar.name,'bytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'audio':audio,'fleet':fleet,'minecraft_runtime_tested':False}
report.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2))
