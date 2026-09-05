"""Reject missing, skipped or failing VehicleStateTest results after Gradle build.

This checks execution evidence; it never substitutes for running Gradle/JUnit.
"""
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = {
    'fullCargoRoundtripRetainsComponentsAndEverySlot',
    'emptySlotsAndBrokenConditionSurvive',
    'rejectsUnknownSchemaAndType',
    'everyFamilyHasMultipleStableIds',
}
root = Path(__file__).resolve().parent.parent
report = root / 'build/test-results/test/TEST-com.harvester.vehicle.VehicleStateTest.xml'
if not report.is_file():
    raise SystemExit('FAIL: VehicleStateTest XML is missing; JUnit execution is not confirmed')
suite = ET.parse(report).getroot()
cases = suite.findall('.//testcase')
executed = set()
for case in cases:
    if case.find('failure') is not None or case.find('error') is not None or case.find('skipped') is not None:
        raise SystemExit('FAIL: failed/errored/skipped test: ' + case.get('name', '?'))
    executed.add(case.get('name', '').removesuffix('()'))
missing = EXPECTED - executed
if missing:
    raise SystemExit('FAIL: expected tests were not executed: ' + ', '.join(sorted(missing)))
for field in ('failures', 'errors', 'skipped'):
    if int(suite.get(field, '0')) != 0:
        raise SystemExit('FAIL: nonzero ' + field)
output = root / 'build/verification/vehicle-state-tests.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({
    'commit': os.environ.get('GITHUB_SHA', 'local-unidentified'),
    'suite': suite.get('name'),
    'executed': sorted(executed),
    'failures': 0,
    'errors': 0,
    'skipped': 0,
}, indent=2) + '\n', encoding='utf-8')
print('PASS: all expected VehicleStateTest methods executed without failure or skip')
