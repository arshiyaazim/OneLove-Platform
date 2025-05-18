#!/bin/bash

echo "=========================================================="
echo "OneLove Dating App - Version Conflict Resolution Helper"
echo "=========================================================="
echo

# Create output directory if it doesn't exist
mkdir -p reports

# Check for missing local.properties
if [ ! -f "local.properties" ]; then
  echo "🔴 Error: local.properties file missing"
  echo "  Creating template local.properties file..."
  echo "sdk.dir=/path/to/your/android/sdk" > local.properties
  echo "  Please edit local.properties with your actual Android SDK path"
  echo
fi

# Check Gradle version
GRADLE_WRAPPER_FILE="gradle/wrapper/gradle-wrapper.properties"
if [ -f "$GRADLE_WRAPPER_FILE" ]; then
  GRADLE_VERSION=$(grep -o "gradle-[0-9]\.[0-9]\(\.[0-9]\)\?" "$GRADLE_WRAPPER_FILE")
  echo "✓ Gradle version: $GRADLE_VERSION"
  
  # Check if it's not 8.2
  if [[ "$GRADLE_VERSION" != "gradle-8.2" ]]; then
    echo "🔶 Warning: Project requires Gradle 8.2"
    echo "  Updating Gradle wrapper..."
    sed -i 's/distributionUrl=.*/distributionUrl=https\\:\/\/services.gradle.org\/distributions\/gradle-8.2-bin.zip/g' "$GRADLE_WRAPPER_FILE"
    echo "  ✓ Gradle wrapper updated to 8.2"
  fi
else
  echo "🔴 Error: Gradle wrapper properties file not found"
fi

# Check AGP and Kotlin versions
ROOT_GRADLE_FILE="build.gradle.kts"
if [ -f "$ROOT_GRADLE_FILE" ]; then
  AGP_VERSION=$(grep -o 'id("com.android.application") version "[0-9]\.[0-9]\.[0-9]"' "$ROOT_GRADLE_FILE" | grep -o '"[0-9]\.[0-9]\.[0-9]"' | tr -d '"')
  KOTLIN_VERSION=$(grep -o 'id("org.jetbrains.kotlin.android") version "[0-9]\.[0-9]\.[0-9]"' "$ROOT_GRADLE_FILE" | grep -o '"[0-9]\.[0-9]\.[0-9]"' | tr -d '"')
  
  echo "✓ AGP version: $AGP_VERSION"
  echo "✓ Kotlin version: $KOTLIN_VERSION"
  
  # Check if they match required versions
  if [[ "$AGP_VERSION" != "8.2.0" ]]; then
    echo "🔶 Warning: Project requires Android Gradle Plugin 8.2.0"
    echo "  Please update in $ROOT_GRADLE_FILE"
  fi
  
  if [[ "$KOTLIN_VERSION" != "1.9.20" ]]; then
    echo "🔶 Warning: Project requires Kotlin 1.9.20"
    echo "  Please update in $ROOT_GRADLE_FILE"
  fi
else
  echo "🔴 Error: Root build.gradle.kts file not found"
fi

# Check for Compose compiler compatibility
APP_GRADLE_FILE="app/build.gradle.kts"
if [ -f "$APP_GRADLE_FILE" ]; then
  COMPOSE_COMPILER_VERSION=$(grep -o 'kotlinCompilerExtensionVersion = "[0-9]\.[0-9]\.[0-9]"' "$APP_GRADLE_FILE" | grep -o '"[0-9]\.[0-9]\.[0-9]"' | tr -d '"')
  
  echo "✓ Compose Compiler version: $COMPOSE_COMPILER_VERSION"
  
  # For Kotlin 1.9.20, compose compiler should be 1.5.4
  if [[ "$KOTLIN_VERSION" == "1.9.20" && "$COMPOSE_COMPILER_VERSION" != "1.5.4" ]]; then
    echo "🔴 Error: Kotlin 1.9.20 requires Compose Compiler 1.5.4"
    echo "  Updating Compose Compiler version..."
    sed -i 's/kotlinCompilerExtensionVersion = "[0-9]\.[0-9]\.[0-9]"/kotlinCompilerExtensionVersion = "1.5.4"/g' "$APP_GRADLE_FILE"
    echo "  ✓ Compose Compiler updated to 1.5.4"
  fi
else
  echo "🔴 Error: App build.gradle.kts file not found"
fi

# Check SDK versions
if [ -f "$APP_GRADLE_FILE" ]; then
  COMPILE_SDK=$(grep -o "compileSdk = [0-9]\+" "$APP_GRADLE_FILE" | grep -o "[0-9]\+")
  TARGET_SDK=$(grep -o "targetSdk = [0-9]\+" "$APP_GRADLE_FILE" | grep -o "[0-9]\+")
  
  echo "✓ compileSdk: $COMPILE_SDK"
  echo "✓ targetSdk: $TARGET_SDK"
  
  # Check for SDK compatibility with AGP 8.2.0
  if [[ "$COMPILE_SDK" -gt "34" || "$TARGET_SDK" -gt "34" ]]; then
    echo "🔴 Error: AGP 8.2.0 officially supports up to compileSdk and targetSdk 34"
    echo "  Consider downgrading or updating AGP"
  fi
fi

# Check for Java version compatibility
if [ -f "$APP_GRADLE_FILE" ]; then
  JAVA_VERSION=$(grep -o 'JavaVersion.VERSION_[0-9]\+' "$APP_GRADLE_FILE" | head -1)
  
  echo "✓ Java compatibility: $JAVA_VERSION"
  
  if [[ "$JAVA_VERSION" != "JavaVersion.VERSION_17" ]]; then
    echo "🔶 Warning: Project requires Java 17 compatibility"
    echo "  Please update in $APP_GRADLE_FILE"
  fi
else
  echo "🔴 Error: App build.gradle.kts file not found"
fi

# Check for duplicate resource definitions
echo
echo "Checking for duplicate resource definitions..."

# Colors
COLOR_DUPLICATES=$(grep -r 'name="[^"]*"' --include="colors.xml" ./app/src/main/res | cut -d'"' -f2 | sort | uniq -d)
if [ -n "$COLOR_DUPLICATES" ]; then
  echo "🔶 Warning: Duplicate color definitions found:"
  echo "$COLOR_DUPLICATES"
  echo "  Run scripts/merge_colors.py to fix this issue"
else
  echo "✓ No duplicate color definitions found"
fi

# Strings
STRING_DUPLICATES=$(grep -r 'name="[^"]*"' --include="strings.xml" ./app/src/main/res | cut -d'"' -f2 | sort | uniq -d)
if [ -n "$STRING_DUPLICATES" ]; then
  echo "🔶 Warning: Duplicate string definitions found:"
  echo "$STRING_DUPLICATES"
  echo "  Consider merging duplicate string definitions"
else
  echo "✓ No duplicate string definitions found"
fi

# Dimens
DIMEN_DUPLICATES=$(grep -r 'name="[^"]*"' --include="dimens.xml" ./app/src/main/res | cut -d'"' -f2 | sort | uniq -d)
if [ -n "$DIMEN_DUPLICATES" ]; then
  echo "🔶 Warning: Duplicate dimension definitions found:"
  echo "$DIMEN_DUPLICATES"
  echo "  Consider merging duplicate dimension definitions"
else
  echo "✓ No duplicate dimension definitions found"
fi

# Check for missing google-services.json
if [ ! -f "app/google-services.json" ]; then
  echo "🔶 Warning: google-services.json file missing"
  echo "  You will need to add your Firebase configuration file"
  echo "  Download it from Firebase Console and place it in the app/ directory"
fi

# Check for Firebase BOM version
FIREBASE_BOM=$(grep -o "firebase-bom:[0-9]\+\.[0-9]\+\.[0-9]\+" "$APP_GRADLE_FILE" | head -1)
if [ -n "$FIREBASE_BOM" ]; then
  echo "✓ Firebase BOM version: $FIREBASE_BOM"
  
  # Extract major version
  FIREBASE_MAJOR=$(echo $FIREBASE_BOM | cut -d: -f2 | cut -d. -f1)
  
  if [[ "$FIREBASE_MAJOR" -ge "33" && "$KOTLIN_VERSION" == "1.9.20" ]]; then
    echo "🔴 Error: Firebase BOM 33+ requires Kotlin 2.1.0+"
    echo "  Either downgrade Firebase BOM to 32.x or upgrade Kotlin to 2.1.0"
  fi
fi

echo
echo "Running ./gradlew clean..."
./gradlew clean

echo
echo "Generating dependency tree report..."
./gradlew app:dependencies > reports/dependency_tree.txt
echo "Dependency tree saved to reports/dependency_tree.txt"

echo
echo "Version conflict check complete!"
echo "To fix detected issues:"
echo "1. Run scripts/merge_colors.py to fix color duplicates"
echo "2. Run scripts/merge_themes.py to fix theme duplicates"
echo "3. Update versions as suggested above"
echo "=========================================================="