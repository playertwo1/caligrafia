from pathlib import Path
import json, xml.etree.ElementTree as ET
from collections import Counter

out = Path(__file__).resolve().parent
suites = [ET.parse(p).getroot() for p in (out/'baseline-xml').glob('TEST-*.xml')]
summary = {'sha':'32b28c11753d5a8b2db86cf5ec75163818eb18cf',
 'baseline':dict(classes=len(suites), **{k:sum(int(s.get(k,0)) for s in suites) for k in ['tests','failures','errors','skipped']}),
 'audits':{s.get('name'):dict(s.attrib) for s in suites if '.audit.' in s.get('name','')},
 'recheck':dict(ET.parse(out/'additional.xml').getroot().attrib),
 'lint':dict(Counter(i.get('severity') for i in ET.parse(out/'lint.xml').getroot().findall('issue')))}
(out/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
print(json.dumps(summary['baseline']),summary['recheck'],summary['lint'])
