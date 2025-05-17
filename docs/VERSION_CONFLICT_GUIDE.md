# OneLove Dating App - Version Conflict Resolution Guide

This document explains the version conflict resolution tools included in the OneLove project and how to use them.

## Available Tools

The OneLove project includes several tools to help manage version conflicts:

1. **Version Conflict Helper Script** - Located at `scripts/version_conflict_helper.sh`
2. **Colors Merger Script** - Located at `scripts/merge_colors.py`
3. **Themes Merger Script** - Located at `scripts/merge_themes.py`
4. **Gradle Resolution Strategies** - Integrated into build scripts

## How to Use the Tools

### Version Conflict Helper Script

This script checks for common version conflicts and compatibility issues in the project.

**Location:** `scripts/version_conflict_helper.sh`

**Usage:**
```bash
# Make sure the script is executable
chmod +x scripts/version_conflict_helper.sh

# Run the script
./scripts/version_conflict_helper.sh
```

**What it checks:**
- Gradle version compatibility
- Android Gradle Plugin version
- Kotlin version
- Compose compiler compatibility
- Java version compatibility
- Duplicate resource definitions
- Missing configuration files

### Color Resource Conflicts

The colors merger script identifies and merges duplicate color definitions across your resource files.

**Location:** `scripts/merge_colors.py`

**Usage:**
```bash
# Make sure the script is executable
chmod +x scripts/merge_colors.py

# Run the script on all colors.xml files
python3 scripts/merge_colors.py $(find app/src/main/res -name "colors.xml")
```

### Theme Conflicts

The themes merger script identifies and merges duplicate theme definitions across your resource files.

**Location:** `scripts/merge_themes.py`

**Usage:**
```bash
# Make sure the script is executable
chmod +x scripts/merge_themes.py

# Run the script on all themes.xml files
python3 scripts/merge_themes.py $(find app/src/main/res -name "themes.xml")
```

### Gradle Dependency Resolution

If you need to fix dependency version conflicts via Gradle, run the included Gradle task:

```bash
# Run the version conflict resolution task
./gradlew app:fixVersionConflicts
```

This task enforces consistent versions for common libraries and helps identify conflicting dependencies.

## Automated Integration

These tools can be integrated into your development workflow:

1. **Pre-commit Hook**: Create a Git pre-commit hook that runs these scripts before allowing commits
2. **CI/CD Pipeline**: Add these scripts to your CI/CD pipeline to catch conflicts early
3. **Build Process**: Configure Gradle to run these checks during the build process

## Example Integration Script

Create a file at `.git/hooks/pre-commit`:

```bash
#!/bin/bash

echo "Running version conflict checks..."
./scripts/version_conflict_helper.sh

# Check for duplicate colors and themes
python3 scripts/merge_colors.py $(find app/src/main/res -name "colors.xml")
python3 scripts/merge_themes.py $(find app/src/main/res -name "themes.xml")

# Exit with error code if issues were found
if [ $? -ne 0 ]; then
  echo "Version conflicts found. Please fix them before committing."
  exit 1
fi
```

Make the hook executable:
```bash
chmod +x .git/hooks/pre-commit
```

## Troubleshooting Common Issues

### 1. Incompatible Kotlin and Compose Versions

If you encounter errors related to Kotlin and Compose compatibility:

```
Execution failed for task ':app:compileDebugKotlin'.
> Kotlin Compose Compiler version is incompatible with Kotlin version
```

Run the version conflict helper script to automatically fix the issue:

```bash
./scripts/version_conflict_helper.sh
```

### 2. Duplicate Resource Definitions

If you encounter errors about duplicate resources:

```
Android resource linking failed
error: resource color/primary already exists.
```

Run the appropriate merger script:

```bash
python3 scripts/merge_colors.py $(find app/src/main/res -name "colors.xml")
```

### 3. Theme Attribute Conflicts

For theme-related conflicts, use the theme merger script:

```bash
python3 scripts/merge_themes.py $(find app/src/main/res -name "themes.xml")
```

## Manual Conflict Resolution

For conflicts that cannot be resolved automatically:

1. Check the full dependency tree:
   ```bash
   ./gradlew app:dependencies > dependencies.txt
   ```

2. Look for multiple versions of the same library and force a specific version in your app's `build.gradle.kts`:
   ```kotlin
   configurations.all {
       resolutionStrategy {
           force("problematic.library:name:correct.version")
       }
   }
   ```

3. For complex theme conflicts, use Android Studio's Resource Manager to visualize and edit themes.