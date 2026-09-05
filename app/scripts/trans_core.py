#!/usr/bin/env python3
import os
import xml.etree.ElementTree as ET

# Base English strings
base_xml_path = "/app/src/main/res/values/strings.xml"
tree = ET.parse(base_xml_path)
root = tree.getroot()

base_dict = {}
for child in root:
    if child.tag == "string":
        name = child.attrib.get("name")
        text = child.text or ""
        base_dict[name] = text

print(f"Loaded {len(base_dict)} base English strings.")

def escape_xml(s):
    s = s.replace('&', '&amp;')
    s = s.replace('<', '&lt;')
    s = s.replace('>', '&gt;')
    s = s.replace("'", "\\'")
    s = s.replace('"', '\\"')
    return s

def save_lang(lang_code, trans_map):
    out_dir = f"/app/src/main/res/values-{lang_code}"
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "strings.xml")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for k in base_dict.keys():
            val = trans_map.get(k, base_dict[k])
            f.write(f'    <string name="{k}">{escape_xml(val)}</string>\n')
        f.write('</resources>\n')
    print(f"Generated {out_path} ({len(base_dict)} strings)")

