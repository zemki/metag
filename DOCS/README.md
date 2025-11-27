# MeTag - Build & Run Guide

## Quick Start

```bash
# Build entire project
./gradlew build

# Build Android app only
./gradlew :MeTag:assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean build
```

## Run & Debug

### Android
1. Open project in Android Studio
2. Select `MeTag` run configuration
3. Choose device/emulator → Run (Shift+F10)

### iOS
1. Run: `./gradlew linkDebugFrameworkIosSimulatorArm64`
2. Open `MeTag/MeTag.xcodeproj` in Xcode
3. Select simulator → Run (Cmd+R)

## Common Tasks

### Run Tests
```bash
./gradlew test                    # All tests
./gradlew :shared:test            # Shared module only
./gradlew test --info             # Verbose output
```

### Lint
```bash
./gradlew lint                    # Run lint checks
./gradlew lintFix                 # Auto-fix issues
```

### Build iOS Framework
```bash
./gradlew linkDebugFrameworkIosSimulatorArm64    # Simulator
./gradlew linkReleaseFrameworkIosArm64           # Device
```

## Configuration

### Development Server
Edit `shared/.../GlobalConfig.kt`:
```kotlin
const val USE_DEV_VALUES = true   // false for production
```

### Debug Credentials
Edit `shared/.../GlobalConfig.kt` → `DevConfig` object

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle OOM | Add to `gradle.properties`: `org.gradle.jvmargs=-Xmx4g` |
| iOS build fails | Run `./gradlew linkDebugFrameworkIosSimulatorArm64` first |
| Sync issues | File → Invalidate Caches → Restart |

## Version Info
- **Current**: 2.0 (Android/iOS build 32)
- **Successor to**: Flutter app v1.4.0
