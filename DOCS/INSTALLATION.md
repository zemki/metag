# Installation & Setup

## Requirements

### Android Development
| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17+ |
| Android SDK | API 35 (target), API 24 (min) |
| Kotlin Plugin | 2.0.0+ |

### iOS Development (macOS only)
| Tool | Version |
|------|---------|
| Xcode | 15.0+ |
| CocoaPods | Latest (`sudo gem install cocoapods`) |

## Android Studio Setup

1. **Install Android Studio**
   - Download from [developer.android.com](https://developer.android.com/studio)

2. **Install SDK Components**
   - SDK Manager → SDK Platforms → Android 14 (API 35)
   - SDK Manager → SDK Tools → Android SDK Build-Tools 35

3. **Install KMM Plugin**
   - Settings → Plugins → Marketplace → "Kotlin Multiplatform"

4. **Open Project**
   - File → Open → Select `MetagCompose` folder
   - Wait for Gradle sync to complete

## Emulator Setup

### Android Emulator
1. Device Manager → Create Device
2. Select: Pixel 6 (or similar)
3. System Image: API 34 (x86_64)
4. RAM: 2048 MB minimum

### iOS Simulator
1. Xcode → Settings → Platforms → iOS Simulator
2. Download iOS 17+ runtime

## First Run Checklist

```bash
# 1. Verify Gradle
./gradlew --version

# 2. Build shared module
./gradlew :shared:build

# 3. Build Android app
./gradlew :MeTag:assembleDebug

# 4. (macOS) Build iOS framework
./gradlew linkDebugFrameworkIosSimulatorArm64
```

## Configuration Files

| File | Purpose |
|------|---------|
| `gradle.properties` | JVM args, Kotlin settings |
| `local.properties` | SDK path (auto-generated) |
| `GlobalConfig.kt` | Server URL, dev credentials |

## Memory Settings

Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -Xms1g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
kotlin.mpp.androidGradlePluginCompatibility.nowarn=true
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| "SDK not found" | Set `ANDROID_HOME` env variable |
| Kotlin version mismatch | Sync Gradle (Ctrl+Shift+O) |
| iOS build fails | Run `pod install` in `MeTag/` |
