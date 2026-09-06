from pathlib import Path
import xml.etree.ElementTree as ET
files=list(Path('build/test-results/test').glob('TEST-*.xml'))
assert files,'No JUnit results: tests did not run'
results=[ET.parse(p).getroot() for p in files]
assert sum(int(r.get('tests','0')) for r in results)==4,'Expected four executed JUnit cases'
for r in results:
 for key in ('failures','errors','skipped'):assert int(r.get(key,'0'))==0,(key,r.attrib)
print('PASS: all four JUnit cases actually executed, zero failures/errors/skips')
