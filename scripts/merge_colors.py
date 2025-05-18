#!/usr/bin/env python3

import sys
import xml.etree.ElementTree as ET
import os
from collections import defaultdict

def process_colors_xml_files(files):
    print("Processing color XML files to identify and merge duplicates...")
    
    # Dictionary to store color definitions by name
    all_colors = defaultdict(list)
    
    # Parse all color files and collect definitions
    for file_path in files:
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            
            for color_elem in root.findall(".//color"):
                name = color_elem.get("name")
                value = color_elem.text.strip() if color_elem.text else ""
                all_colors[name].append((file_path, value))
        except Exception as e:
            print(f"Error processing {file_path}: {e}")
    
    # Find duplicates and create a resolution plan
    duplicates = {name: values for name, values in all_colors.items() if len(values) > 1}
    
    if not duplicates:
        print("No duplicate colors found. No changes needed.")
        return
    
    print(f"Found {len(duplicates)} duplicate color definitions:")
    
    # Group by file to minimize file operations
    file_changes = defaultdict(dict)
    
    for name, values in duplicates.items():
        print(f"  - '{name}' defined {len(values)} times with values: {[v for _, v in values]}")
        
        # Keep the first definition and mark the rest for deletion
        keep_file, keep_value = values[0]
        
        # Check if values are consistent
        consistent = all(v == keep_value for _, v in values)
        if not consistent:
            print(f"    WARNING: Inconsistent values for '{name}', keeping value from {os.path.basename(keep_file)}: '{keep_value}'")
        
        # Mark duplicates for removal in each file
        for file_path, _ in values[1:]:
            file_changes[file_path][name] = None  # Mark for deletion
    
    # Apply changes to files
    for file_path, changes in file_changes.items():
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            
            # Delete duplicates
            for name in changes:
                for color_elem in root.findall(f".//color[@name='{name}']"):
                    root.remove(color_elem)
            
            # Write back changes
            tree.write(file_path, encoding="utf-8", xml_declaration=True)
            print(f"Updated {file_path}: removed {len(changes)} duplicate color definitions")
            
        except Exception as e:
            print(f"Error updating {file_path}: {e}")
    
    print("Color merging complete!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python merge_colors.py <colors.xml files...>")
        print("Example: python merge_colors.py $(find app/src/main/res -name \"colors.xml\")")
        sys.exit(1)
    
    process_colors_xml_files(sys.argv[1:])