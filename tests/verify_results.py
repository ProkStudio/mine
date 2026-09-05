"""Reject missing, skipped or failing state AND movement regression results.

This checks execution evidence; it never substitutes for running Gradle/JUnit.
Run after a clean build to avoid accepting stale XML from another revision.
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
}
root = Path(__file__).resolve().parent.parent
output_dir = root / 'build/verification'
# Remove old evidence before checking anything: failure must not retain an old PASS summary.
for filename in ('vehicle-state-tests.json', 'vehicle-physics-tests.json'):
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
for name, filename in [('VehicleStateTest', 'vehicle-state-tests.json'),
                       ('VehiclePhysicsTest', 'vehicle-physics-tests.json')]:
    (output_dir / filename).write_text(json.dumps(verified[name], indent=2) + '\n', encoding='utf-8')
    print(f'PASS: all expected {name} methods executed without failure or skip')
