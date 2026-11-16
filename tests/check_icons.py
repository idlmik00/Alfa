from pathlib import Path
import xml.etree.ElementTree as ET
root = ET.parse('app/src/main/assets/icons.svg').getroot()
ns = {'s': 'http://www.w3.org/2000/svg'}
ids=set()
for item in root.findall('.//s:symbol',ns):
    name=item.attrib['id']
    assert name not in ids
    ids.add(name)
    tokens=item.find('s:path',ns).attrib['d'].split()
    i=0
    while i<len(tokens):
        count={'M':2,'L':2,'Q':4,'C':6,'Z':0}[tokens[i]]
        assert i+count<len(tokens)
        for v in tokens[i+1:i+1+count]: float(v)
        i+=count+1
assert len(ids)>=30
assert {'wallet','transfer','chart','target','home','list','person','plus','settings','more'}.issubset(ids)
print(f'{len(ids)} SVG icons validated')
