#!/usr/bin/env python3

import sys
import xml.etree.ElementTree as ET
import os
from collections import defaultdict

def process_themes_xml_files(files):
    print("Processing theme XML files to identify and merge duplicates...")
    
    # Dictionary to store theme styles by name
    all_themes = defaultdict(list)
    
    # Parse all theme files and collect definitions
    for file_path in files:
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            
            for style_elem in root.findall(".//style"):
                name = style_elem.get("name")
                parent = style_elem.get("parent")
                
                # Store the entire element along with file path and parent info
                all_themes[name].append((file_path, parent, style_elem))
        except Exception as e:
            print(f"Error processing {file_path}: {e}")
    
    # Find duplicates and create a resolution plan
    duplicates = {name: values for name, values in all_themes.items() if len(values) > 1}
    
    if not duplicates:
        print("No duplicate theme styles found. No changes needed.")
        return
    
    print(f"Found {len(duplicates)} duplicate theme style definitions:")
    
    # Group by file to minimize file operations
    file_changes = defaultdict(dict)
    
    for name, values in duplicates.items():
        print(f"  - '{name}' defined {len(values)} times:")
        
        # For themes, we might need to merge attributes rather than just keep one version
        primary_file, primary_parent, primary_style = values[0]
        print(f"    Primary definition in {os.path.basename(primary_file)}, parent={primary_parent}")
        
        # Get all item names from the primary style
        primary_items = {item.get("name"): item for item in primary_style.findall("item")}
        
        # Track which files need updates
        for file_path, parent, style_elem in values[1:]:
            print(f"    Duplicate in {os.path.basename(file_path)}, parent={parent}")
            
            # Check if parents are consistent
            if parent != primary_parent:
                print(f"    WARNING: Inconsistent parent for '{name}' in {os.path.basename(file_path)}")
                # In this case, we'll use the parent from the primary definition
            
            # Analyze items in this style
            for item in style_elem.findall("item"):
                item_name = item.get("name")
                item_value = item.text.strip() if item.text else ""
                
                # If this item doesn't exist in primary, add it
                if item_name not in primary_items:
                    # Create a new item element
                    new_item = ET.Element("item")
                    new_item.set("name", item_name)
                    new_item.text = item_value
                    primary_style.append(new_item)
                    primary_items[item_name] = new_item
                    
                    print(f"      Added item '{item_name}' from {os.path.basename(file_path)}")
                # If it exists but with different value, issue warning
                elif primary_items[item_name].text.strip() if primary_items[item_name].text else "" != item_value:
                    print(f"      WARNING: Different value for item '{item_name}', keeping value from {os.path.basename(primary_file)}")
            
            # Mark this duplicate for removal
            file_changes[file_path][name] = None
    
    # First update the primary files with merged content
    # We do this first to ensure all content is preserved before removing duplicates
    primary_updates = {}
    for name, values in duplicates.items():
        primary_file = values[0][0]
        primary_updates[primary_file] = True
    
    for file_path in primary_updates:
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            
            # Replace the primary style definitions with our merged versions
            for name, values in duplicates.items():
                if values[0][0] == file_path:
                    # Find the existing style element
                    for style_elem in root.findall(f".//style[@name='{name}']"):
                        # Replace with our updated version
                        idx = list(root).index(style_elem)
                        root.remove(style_elem)
                        root.insert(idx, values[0][2])
            
            # Write back changes
            tree.write(file_path, encoding="utf-8", xml_declaration=True)
            print(f"Updated {file_path} with merged styles")
            
        except Exception as e:
            print(f"Error updating primary file {file_path}: {e}")
    
    # Now remove duplicates from other files
    for file_path, changes in file_changes.items():
        try:
            tree = ET.parse(file_path)
            root = tree.getroot()
            
            # Delete duplicates
            for name in changes:
                for style_elem in root.findall(f".//style[@name='{name}']"):
                    root.remove(style_elem)
            
            # Write back changes
            tree.write(file_path, encoding="utf-8", xml_declaration=True)
            print(f"Updated {file_path}: removed {len(changes)} duplicate theme styles")
            
        except Exception as e:
            print(f"Error updating {file_path}: {e}")
    
    print("Theme merging complete!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python merge_themes.py <themes.xml files...>")
        print("Example: python merge_themes.py $(find app/src/main/res -name \"themes.xml\")")
        sys.exit(1)
    
    process_themes_xml_files(sys.argv[1:])