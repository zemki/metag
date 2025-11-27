# Architecture Overview

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform 1.6.11 |
| Shared Logic | Kotlin Multiplatform 2.0.0 |
| Networking | Ktor 2.3.12 |
| Serialization | Kotlinx Serialization 1.6.3 |
| Async | Kotlinx Coroutines 1.7.3 |
| Date/Time | Kotlinx DateTime 0.6.2 |
| Design | Material3 |

## Module Structure

```
MetagCompose/
├── shared/                      # KMM shared module
│   ├── commonMain/              # Cross-platform code
│   │   └── kotlin/.../
│   │       ├── data/            # Repositories, models, storage
│   │       ├── ui/              # Compose screens & components
│   │       ├── util/            # Utilities (audio, network, time)
│   │       └── platform/        # expect declarations
│   ├── androidMain/             # Android implementations
│   └── iosMain/                 # iOS implementations
├── MeTag/                       # Android app module
└── MeTag/ (xcodeproj)           # iOS Xcode project
```

## Key Classes

### Data Layer
| Class | Purpose |
|-------|---------|
| `AuthRepository` | Login, logout, token management |
| `EntriesRepository` | CRUD operations for entries |
| `TokenStorage` | Secure token persistence (expect/actual) |
| `OfflineEntryManager` | Local entry caching for offline mode |

### UI Layer
| Screen | File |
|--------|------|
| Login | `ui/login/LoginScreen.kt` |
| Entries List | `ui/entries/ModernEntriesScreen.kt` |
| Add/Edit Entry | `ui/entries/AddEntryScreen.kt` |
| Audio Recording | `ui/components/AudioRecordingField.kt` |

### Platform-Specific (expect/actual)
- `TokenStorage` - Keychain (iOS) / EncryptedSharedPrefs (Android)
- `AudioRecorder` - AVAudioRecorder (iOS) / MediaRecorder (Android)
- `AudioPlayer` - AVAudioPlayer (iOS) / MediaPlayer (Android)
- `NetworkMonitor` - Network framework (iOS) / ConnectivityManager (Android)
- `openCamera()` - DataScannerViewController (iOS) / Camera Intent (Android)

## QR Code Login
- **iOS**: In-app scanner using VisionKit `DataScannerViewController` (iOS 16+)
- **Android**: Opens system camera app (most cameras auto-detect QR codes)
- **Flow**: Scans `metagapp://login?token=xxx` → `DeepLinkHandler` → auto-login

## Data Flow

```
User Action → ViewModel → Repository → Ktor HTTP Client → Backend API
                ↓                              ↓
            UI State ←──────────────── JSON Response
                ↓
         Compose Recomposition
```

## Offline Support
1. Network check via `NetworkMonitor`
2. If offline → `OfflineEntryManager.addPendingEntry()`
3. On reconnect → `syncPendingEntries()` batch upload
4. Visual indicators: SYNCED / PENDING / FAILED

## i18n
- Resources: `shared/src/commonMain/composeResources/values[-lang]/strings.xml`
- Languages: English (default), Italian (`-it`), German (`-de`)
- Usage: `stringResource(Res.string.key_name)`
