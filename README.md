# Avis Tool

BlackBox-based box that installs and launches Oxide: Survival Island (base.apk + split APKs).

## Build

- JDK 11
- Android SDK: platform 30, build-tools 30.0.3, CMake 3.10.2, NDK 21.0.6113669
- `local.properties`: `sdk.dir=/path/to/Android/Sdk`

```
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Debug builds log to `Download/avis_tool.log`.
