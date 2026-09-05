#!/usr/bin/env python3
"""
Modular multi-language localization generator for Horizon TV Android Application.
Generates 30 localized values-<lang>/strings.xml directories.
"""
import os
import xml.etree.ElementTree as ET

BASE_PATH = "/app/src/main/res/values/strings.xml"
tree = ET.parse(BASE_PATH)
root = tree.getroot()

base_keys = []
base_dict = {}
for child in root:
    if child.tag == "string":
        k = child.attrib.get("name")
        v = child.text or ""
        base_keys.append(k)
        base_dict[k] = v

print(f"Base strings loaded: {len(base_dict)}")

def escape_xml(s):
    s = s.replace('&', '&amp;')
    s = s.replace('<', '&lt;')
    s = s.replace('>', '&gt;')
    s = s.replace("'", "\\'")
    s = s.replace('"', '\\"')
    return s

def write_locale(code, lang_map):
    target_dir = f"/app/src/main/res/values-{code}"
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, "strings.xml")
    with open(target_file, "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for k in base_keys:
            val = lang_map.get(k, base_dict[k])
            f.write(f'    <string name="{k}">{escape_xml(val)}</string>\n')
        f.write('</resources>\n')
    print(f"Saved: values-{code}/strings.xml")

if __name__ == "__main__":
    print("Locale generator engine ready.")
