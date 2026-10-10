# Java should > 21
run script:
C:\workspace\04_LEARNING\miniproject\UIZalo\src\android_app\app\scripts\set_env_android.ps1

# SDK manager
To list all SDK platform/image for build/emulator
C:\Users\pla2hc\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat --list

platforms;android-37.0 => build package
system-images;android-37;... => run emulator

# install package
C:\Users\pla2hc\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat "system-images;android-37.0;google_apis;x86_64"

# build command : ANDROID / GRADLE COMMANDS
## Build project:
.\gradlew build

## Clean project:
.\gradlew clean

## Clean and build:
.\gradlew clean build

## Build Debug APK: (to device)
.\gradlew assembleDebug

## Build Release APK:
.\gradlew assembleRelease

## Install Debug APK:
.\gradlew installDebug

## Install Release APK:
.\gradlew installRelease

## Run tests:
.\gradlew test

## Build and install Debug APK:
.\gradlew clean assembleDebug installDebug

## View all Gradle tasks:
.\gradlew tasks

## View Android-related tasks:
.\gradlew tasks | Select-String "assemble|install"

## ADB - Install APK:
adb install app\build\outputs\apk\debug\app-debug.apk

## ADB - Reinstall APK:
adb install -r app\build\outputs\apk\debug\app-debug.apk

## ADB - Uninstall app:
adb uninstall <package_name>

## ADB - List connected devices:
adb devices

## ADB - View logs:
adb logcat

## DEBUG APK OUTPUT:
app\build\outputs\apk\debug\app-debug.apk

## RELEASE APK OUTPUT:
app\build\outputs\apk\release\app-release.apk

## AAB:
.\gradlew bundleRelease

## AAB OUTPUT:
app\build\outputs\bundle\release\app-release.aab