#!/usr/bin/env python3
import os
import sys

def escape_xml(s):
    # XML escaping for Android strings.xml
    s = s.replace('&', '&amp;')
    s = s.replace('<', '&lt;')
    s = s.replace('>', '&gt;')
    s = s.replace("'", "\\'")
    s = s.replace('"', '\\"')
    return s

def write_strings_xml(lang_code, strings_dict):
    out_dir = f"/app/src/main/res/values-{lang_code}"
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "strings.xml")
    
    with open(out_file, "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for k, v in strings_dict.items():
            f.write(f'    <string name="{k}">{escape_xml(v)}</string>\n')
        f.write('</resources>\n')
    print(f"Written: {out_file} ({len(strings_dict)} strings)")

print("Helper ready")
