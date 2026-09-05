"""Require actual execution of state, physics, passenger, animation and resource tests.

Run after a clean Gradle build; this verifier never substitutes for executing tests.
"""
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = {
    'VehicleStateTest': {
        'fullCargoRoundtripRetainsComponentsAndEverySlot',
        'emptySlotsAndBrokenConditionSurvive',
        'rejectsUnknownSchemaAndType',
        'everyFamilyHasMultipleStableIds',
    },
    'VehiclePhysicsTest': {
        'shortestYawAndPitchAreBounded',
        'invalidLookCannotInjectThrustOrAngles',
        'backwardsKeyBrakesWithoutReversing',
        'planeNeedsRunwayAndCannotHoverWithoutThrust',
        'verticalHoverConsumesFuelButParkedVehicleDoesNot',
        'floodedFlightIgnoresEveryInputCombination',
        'emptyTankAndStaleInputCannotPowerTakeoff',
        'buoyancyConvergesFromAboveAndBelow',
        'boatOneDryTickDoesNotStopItButLandCannotAccelerate',
        'boatSidewaysDragTurnAndReverseAreLimited',
        'longFlightStaysFiniteAndSpeedLimited',
    },
    'PassengerPoseTest': {
        'cameraFullTurnDoesNotRotateBody',
        'headAnglesAreRelativeBoundedAndFinite',
        'everySeatHasFiniteLimbPose',
        'motorcyclePassengerDoesNotHoldDriverHandlebars',
        'itemUseAndAttackKeepVanillaArms',
        'rearViewSeamIsSmoothedAndSeatChangeResetsHistory',
        'headSmoothingIsIndependentForPlayersAndFrameSubdivision',
    },
    'VehicleAnimationTest': {
        'smoothingIsFrameRateIndependentAndBounded',
        'wheelRotationUsesSignedDistanceAndRadius',
        'hubAndTireHaveIdenticalRollingRadius',
        'rotorPhaseDoesNotDependOnFrameSubdivision',
        'longerFramesUseTheSameResponseForRotorSpeedAndPhase',
        'stoppedRotorsSettleAndHeaderDoesNotJump',
        'historiesAreIndependentAndTeleportDoesNotSpinWheels',
    },
    'VehicleVisualResourcesTest': {
        'passengerMixinsAreClientOnlyAndRequired',
        'pistonPlaceholderIsNotRestored',
    },
}
OUTPUTS = {
    'VehicleStateTest': 'vehicle-state-tests.json',
    'VehiclePhysicsTest': 'vehicle-physics-tests.json',
    'PassengerPoseTest': 'passenger-pose-tests.json',
    'VehicleAnimationTest': 'vehicle-animation-tests.json',
    'VehicleVisualResourcesTest': 'vehicle-visual-resources-tests.json',
}
root = Path(__file__).resolve().parent.parent
output_dir = root / 'build/verification'
# A failed run must not leave an older PASS summary behind.
for filename in OUTPUTS.values():
    (output_dir / filename).unlink(missing_ok=True)
verified = {}
for name, expected in EXPECTED.items():
    report = root / f'build/test-results/test/TEST-com.harvester.vehicle.{name}.xml'
    if not report.is_file():
        raise SystemExit(f'FAIL: {name} XML is missing; JUnit execution is not confirmed')
    suite = ET.parse(report).getroot()
    executed = set()
    for case in suite.findall('.//testcase'):
        if any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')):
            raise SystemExit(f'FAIL: {name}: failed/errored/skipped test: ' + case.get('name', '?'))
        executed.add(case.get('name', '').removesuffix('()'))
    missing = expected - executed
    if missing:
        raise SystemExit(f'FAIL: {name}: tests not executed: ' + ', '.join(sorted(missing)))
    for field in ('failures', 'errors', 'skipped'):
        if int(suite.get(field, '0')) != 0:
            raise SystemExit(f'FAIL: {name}: nonzero {field}')
    verified[name] = {
        'commit': os.environ.get('GITHUB_SHA', 'local-unidentified'),
        'suite': suite.get('name'),
        'executed': sorted(executed),
        'failures': 0,
        'errors': 0,
        'skipped': 0,
    }
output_dir.mkdir(parents=True, exist_ok=True)
for name, filename in OUTPUTS.items():
    (output_dir / filename).write_text(json.dumps(verified[name], indent=2) + '\n', encoding='utf-8')
    print(f'PASS: all expected {name} methods executed without failure or skip')
