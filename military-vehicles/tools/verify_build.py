#!/usr/bin/env python3
"""Strict post-build gate: exact executed JUnit methods, remapped JAR, resources and crafting."""
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
'FleetResourceTest': {'allFiveItemsHaveModernDefinitionsAndDistinctBoundedModels', 'bothLanguagesNameEveryRegisteredVehicleAndCapacityTooltip', 'fleetManifestMatchesTheRuntimeProfilesAndMeshCounts', 'generatedManifestKeepsOriginalTruckAndNewTypesSeparate'},
'TrackedDriveTest': {'counterRotatingTracksPivotAtRest','bothDirectionsRespectPerTrackLimits','brakingStopsBothTracksIncludingPivot','oppositeInputCrossesZeroBeforeReversing','unpoweredAndAirCannotAddKineticEnergy','collisionCorrectionDoesNotStoreForwardImpulse','invalidValuesFailClosed','turningDirectionIsConsistentInReverse','padsCirculateBoundedlyAndIndependently'},
'VehicleOperationsTest': {'driverAndGunnerPrivilegesAreSeparate','deploymentLocksFromIntentThroughRetraction','deploymentRejectsMotionAirAndFaults','cancelledDeploymentCannotFire','fuelServiceConservesTotalsAndReserve','fuelServiceRejectsInvalidStores','repairsRequireAStockedKitAndClampToCapacity','gunRequiresAmmoFuelGroundAndCooldown','aimIsRateLimitedWrappedAndPitchClamped','stationParkingRejectsNonFiniteSpeed'},
'ExpansionGeometryTest': {'saveProfilesPreserveFleetAndRejectCrossType','newRigsHaveOwnMissionHardware','animatedTrackedAndServiceRigsFitCollision','newProfilesHaveExpectedCapacitiesAndDriveFamilies'},
'ExpansionResourceTest': {'everyVehicleAndSupplyHasOneBoundedSurvivalRecipe','craftingNeverConsumesPackedVehiclesOrDuplicatesCans','recipeBookUnlockCoversExactCraftingSet','localizedRoleHelpAndRemappableKeysCoverTheFleet','generatedRoleFlagsMatchProductionProfiles'}}

expected['FleetTuningTest'] = {'defaultsPreserveLegacyEconomy', 'partialFilesInheritDocumentedDefaults', 'rejectsUnknownAndDuplicateKeys', 'rejectsUnsupportedSchemaAndMalformedValues', 'integerBoundsCannotBeBypassed', 'canonicalRoundTripIsDeterministic', 'createsMissingFileWithoutOverwritingExistingEdits', 'invalidReloadKeepsLastGoodSnapshot', 'boundsBytesAndRejectsMalformedUtf8', 'separateStoresDoNotLeakServerState', 'configuredTransfersConserveFuelAndReserve', 'configuredRepairsRequireKitsAndRespectCapacity', 'weaponDisableAndReloadLimitsApply', 'supportDisableStopsAccounting', 'engineDebitCannotUnderflow', 'restoringCannotShortenConfiguredCooldown', 'firstPhysicalHitBlocksTargetsBehindIt', 'raySelectionIsOrderIndependentAndFinite', 'serviceTargetsMustBeGroundedDryAndStationary'}

expected['VehicleParkingTest'] = {'packingAllowsStoppedDryEmptyVehicle', 'packingKeepsPassengerAndDeploymentLocks', 'packingRejectsAirAndWater', 'packingIncludesVerticalAndDiagonalMotion', 'packingRejectsNonFiniteAndOverflowingVelocity', 'packingPreservesLegacySpeedBoundary', 'stationSwitchRequiresArmedSoleFirstPassenger', 'stationSwitchRejectsAirWaterAndVerticalMotion', 'stationSwitchRejectsNonFiniteAndOverflowingVelocity', 'stationSwitchPreservesStopThreshold'}

expected['EngineIntentTest'] = {'firstPressAfterMountStillStartsEngine', 'heldEngineKeyCannotToggleAfterControlTimeout', 'repeatedTimeoutsCannotAccumulateIntent', 'releaseAfterTimeoutRestoresEnginePress', 'remountWithHeldKeyCannotStartEngine', 'remountAfterReleaseTogglesAgain', 'awaitingReleaseDoesNotBlockDrivingOrBrake', 'staleInputStillBrakesAndDiscardsToggle', 'invalidAndReplayedPacketsCannotArmTheEngine', 'separateSeatsDoNotShareEngineIntent', 'legacyControlLatchExpectationsPreserved'}

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
    for clazz in ['core/VehicleParking', 'core/ControlLatch', 'entity/TruckEntity', 'entity/TruckExhaust', 'client/TruckRenderer', 'client/TruckAudio', 'client/TruckEngineSound', 'core/EngineFeedback', 'core/TruckFeedback', 'core/VehicleKind', 'core/VehicleGeometry', 'core/VehicleSaveCodec', 'entity/CargoScreenHandler', 'init/MilitarySounds','core/TrackDrive','core/VehicleOperations','core/ExpansionGeometry','entity/VehicleSystems','network/VehicleAction','core/FleetTuning','core/ServiceTargeting','config/FleetConfigFile','config/ServerFleetConfig','network/FleetSettingsPayload','client/ClientFleetSettings']:
        assert f'com/prokstudio/militaryvehicles/{clazz}.class' in names, clazz
    assert b'net/minecraft/class_' in a.read('com/prokstudio/militaryvehicles/entity/TruckEntity.class'), 'Expected intermediary-remapped entity class'
    assert not any(n.startswith(('com/harvester/', 'ws/schild/')) or n.endswith(('.exe','.dll','.so','.dylib','.wav','.pcm')) for n in names)
    assert 'LICENSE_military-vehicles' in names and m['license'] == 'CC0-1.0'
    for n in names:
        if n.endswith('.json'):
            json.loads(a.read(n))
    expected_fleet = {
        'truck_6x6': (2,27,2400,200,6,2,'truck_engine'), 'scout_buggy': (2,9,1200,120,4,2,'buggy_engine'), 'carrier_8x8': (6,18,3200,360,8,4,'carrier_engine'),
        'warden_tank': (2,9,4000,500,14,0,'tank_engine'), 'fuel_tanker': (2,9,9600,240,6,2,'truck_engine'), 'field_workshop': (2,27,2800,260,6,2,'truck_engine'),
        'recovery_vehicle': (2,18,3200,300,6,2,'truck_engine'), 'bastion_howitzer': (2,18,3600,340,14,0,'artillery_engine')}
    expected_items = set(expected_fleet) | {'fuel_can','repair_kit','vehicle_frame','vehicle_shell'}
    assert {n.removeprefix('assets/militaryvehicles/items/').removesuffix('.json') for n in names if n.startswith('assets/militaryvehicles/items/') and n.endswith('.json')} == expected_items
    for item in sorted(expected_items):
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
    for key in ['system_disabled','config_failed','config_reloaded','enabled','disabled']:
        assert 'message.militaryvehicles.'+key in en
    for key in ['rules_pending','server_rules','server_gun','server_tanker','server_workshop','server_recovery']:
        assert 'help.militaryvehicles.'+key in en
    base = 'assets/militaryvehicles/'
    sounds = json.loads(a.read(base+'sounds.json'))
    expected_sounds = {'truck_engine','buggy_engine','carrier_engine','tank_engine','artillery_engine'}
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
    assert len(fleet['vehicles']) == len(vehicles) == len(expected_fleet) == 8
    assert set(vehicles) == set(expected_fleet)
    for key, spec in expected_fleet.items():
        assert tuple(vehicles[key][field] for field in ('seats','cargoSlots','tank','condition','wheels','steeringWheels','sound')) == spec
        assert vehicles[key]['parts'] > 30 and vehicles[key]['boxes'] > 60
        assert vehicles[key]['tracked'] == (key in {'warden_tank','bastion_howitzer'})
        assert vehicles[key]['armed'] == vehicles[key]['tracked']
        assert vehicles[key]['support'] == (key in {'fuel_tanker','field_workshop','recovery_vehicle'})
        for prefix in ('item.','entity.','help.'):
            assert prefix+'militaryvehicles.'+key in en
    recipe_prefix = 'data/militaryvehicles/recipe/'
    expected_recipes = expected_items | {'refill_fuel_can'}
    assert {n.removeprefix(recipe_prefix).removesuffix('.json') for n in names if n.startswith(recipe_prefix) and n.endswith('.json')} == expected_recipes
    for key in expected_recipes:
        recipe = json.loads(a.read(recipe_prefix+key+'.json'))
        assert recipe['result']['id'] == 'militaryvehicles:'+('fuel_can' if key == 'refill_fuel_can' else key)
        assert 1 <= recipe['result']['count'] <= 4
        ingredients = list(recipe.get('key',{}).values()) + recipe.get('ingredients',[])
        assert all(i not in {'militaryvehicles:'+v for v in expected_fleet} for i in ingredients), 'Packed vehicles must never be recipe inputs'
    refill=json.loads(a.read(recipe_prefix+'refill_fuel_can.json'))
    assert refill['ingredients']==['militaryvehicles:fuel_can','minecraft:coal','minecraft:coal'] and refill['result']['count']==1
    book=json.loads(a.read('data/militaryvehicles/advancement/recipes/field_manual.json'))
    assert set(book['rewards']['recipes'])=={'militaryvehicles:'+r for r in expected_recipes}
    assert b'CC0-1.0' in a.read(base+'sounds/LICENSE.txt')
result = {'status':'PASS','junit_cases':cases,'jar':jar.name,'bytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'audio':audio,'fleet':fleet,'recipes':len(expected_recipes),'minecraft_runtime_tested':False,'server_rules_schema':1}
report.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2))
