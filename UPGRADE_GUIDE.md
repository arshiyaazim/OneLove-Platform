# OneLove App Upgrade Guide

This guide provides instructions on how to upgrade various components of the OneLove app codebase.

## Table of Contents
1. [Android Gradle Plugin (AGP)](#android-gradle-plugin)
2. [Kotlin](#kotlin)
3. [Compose](#compose)
4. [Gradle](#gradle)
5. [Dependencies](#dependencies)
6. [SDK Versions](#sdk-versions)
7. [Troubleshooting](#troubleshooting)

## Android Gradle Plugin

The app currently uses AGP 8.2.0. To upgrade to a newer version:

1. Open the root `build.gradle.kts` file
2. Update the AGP version:
   ```kotlin
   id("com.android.application") version "8.2.0" apply false
   // Change to your desired version
   id("com.android.application") version "8.3.0" apply false
   ```
3. Perform a Gradle sync
4. Test the app to ensure compatibility

When upgrading the AGP, consider:
- Breaking changes in the new version
- Compatibility with your Gradle version
- Compatibility with the Kotlin version

## Kotlin

Current Kotlin version: 1.9.20

To upgrade Kotlin:

1. Open the root `build.gradle.kts` file
2. Update the Kotlin version:
   ```kotlin
   id("org.jetbrains.kotlin.android") version "1.9.20" apply false
   // Change to your desired version
   id("org.jetbrains.kotlin.android") version "1.9.21" apply false
   ```
3. Update the Kotlin compiler extension version in `app/build.gradle.kts`:
   ```kotlin
   composeOptions {
       kotlinCompilerExtensionVersion = "1.5.4"
       // Change to compatible version
       kotlinCompilerExtensionVersion = "1.5.5"
   }
   ```
4. Perform a Gradle sync

Ensure Kotlin version is compatible with:
- Your AGP version
- Your Compose compiler extension version

## Compose

Current Compose BOM version: 2023.10.01
Current Compose Compiler version: 1.5.4

To upgrade Compose:

1. Update the Compose BOM in `app/build.gradle.kts`:
   ```kotlin
   implementation(platform("androidx.compose:compose-bom:2023.10.01"))
   // Change to your desired version
   implementation(platform("androidx.compose:compose-bom:2023.11.01"))
   ```

2. Update Compose compiler extension version:
   ```kotlin
   composeOptions {
       kotlinCompilerExtensionVersion = "1.5.4"
       // Change to compatible version
       kotlinCompilerExtensionVersion = "1.5.5"
   }
   ```

3. Perform a Gradle sync

When upgrading Compose:
- Check compatibility matrix: https://developer.android.com/jetpack/androidx/releases/compose-kotlin
- Ensure the Compose compiler matches your Kotlin version
- Test thoroughly for UI/UX issues

## Gradle

Current Gradle version: 8.2

To upgrade Gradle:

1. Update the `gradle-wrapper.properties` file:
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
   # Change to
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.3-bin.zip
   ```

2. Sync the project

Check compatibility with:
- Your AGP version
- Your Kotlin version
- Your JDK version

## Dependencies

The app uses a flat dependency declaration approach. To upgrade a specific dependency:

1. Find the dependency in `app/build.gradle.kts`
2. Update the version number
3. Sync and test

Example:
```kotlin
// From
implementation("androidx.core:core-ktx:1.12.0")
// To
implementation("androidx.core:core-ktx:1.13.0")
```

Key dependencies to monitor:
- Firebase BOM: `com.google.firebase:firebase-bom`
- Hilt: `com.google.dagger:hilt-android`
- Room: `androidx.room:room-runtime`
- Coroutines: `org.jetbrains.kotlinx:kotlinx-coroutines-core`
- Navigation Compose: `androidx.navigation:navigation-compose`

## SDK Versions

Current settings:
- compileSdk = 34
- minSdk = 24
- targetSdk = 34

To update the target or compile SDK:

1. Open `app/build.gradle.kts`
2. Update the SDK version:
   ```kotlin
   compileSdk = 34
   // Change to
   compileSdk = 35
   
   // And/or
   targetSdk = 34
   // Change to
   targetSdk = 35
   ```

3. Review the API changes and ensure compatibility
4. Check if any new permissions or components need to be added

## Troubleshooting

### Common Issues

1. **Incompatible Kotlin and Compose versions**: 
   - Check the [Compose-Kotlin compatibility matrix](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)
   - Ensure your Kotlin version works with your chosen Compose compiler

2. **Build fails after AGP upgrade**:
   - Check for breaking changes in the AGP version
   - Ensure Gradle version is compatible
   - Clear caches: `./gradlew clean` and invalidate caches in Android Studio

3. **Runtime crashes after upgrade**:
   - Check for deprecated APIs that were removed
   - Review API level compatibility
   - Test thoroughly across multiple devices/API levels

4. **Dependency conflicts**:
   - Use `./gradlew app:dependencies` to view the dependency tree
   - Resolve version conflicts with appropriate version numbers
   - Consider using the `resolutionStrategy` in your Gradle file for complex conflicts

5. **Performance issues after upgrade**:
   - Run Android Studio profiler to identify bottlenecks
   - Check for memory leaks
   - Ensure Compose functions are properly optimized with `remember` and immutable structures

### Getting Help

If you encounter problems:
1. Consult the official documentation for the specific component
2. Check for known issues on the relevant GitHub repositories
3. Search Stack Overflow for similar problems
4. Contact the OneLove development team lead