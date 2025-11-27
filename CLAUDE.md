# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform Mobile (KMM)** project called "MeTag" that targets both Android and iOS platforms using **Compose Multiplatform** for the UI layer.

## Key Technologies

- **Kotlin Multiplatform** (v2.0.0) - Shared business logic
- **Compose Multiplatform** (v1.6.11) - Cross-platform UI
- **Ktor** (v2.3.12) - HTTP networking
- **Kotlinx Serialization** (v1.6.3) - JSON handling
- **Kotlinx Coroutines** (v1.7.3) - Async programming
- **Kotlinx DateTime** (v0.6.2) - Date/time handling
- **AndroidX Security Crypto** (v1.1.0-alpha06) - Android secure storage
- **AndroidX Lifecycle ViewModel Compose** (v2.7.0) - ViewModel support
- **Android Gradle Plugin** (v8.9.2) - Build tooling

## Architecture

The project follows **Clean Architecture** with MVVM pattern:

- **shared/** - Kotlin Multiplatform module with business logic
  - **commonMain/** - Platform-agnostic code (repositories, models, UI)
  - **androidMain/** - Android-specific implementations
  - **iosMain/** - iOS-specific implementations
- **MeTag/** - Android application module
- **MeTag/** - iOS Xcode project (contains Swift files: `ContentView.swift`, `iOSApp.swift`)

## Essential Commands

### Build & Run
```bash
# Build entire project
./gradlew build

# Clean build
./gradlew clean build

# Install Android app
./gradlew installDebug
```

### Testing
```bash
# Run all tests
./gradlew test

# Run with verbose output
./gradlew test --info

# Run specific module tests
./gradlew :shared:test
./gradlew :MeTag:test
```

### Lint
```bash
# Run Android lint
./gradlew lint

# Run lint with fixes
./gradlew lintFix
```

### iOS Development
```bash
# Build iOS frameworks
./gradlew linkDebugFrameworkIosSimulatorArm64
```

## Platform-Specific Implementations

When implementing platform-specific features, use the expect/actual pattern:

1. Define interface in `commonMain`
2. Implement in `androidMain` and `iosMain`
3. Examples: `TokenStorage`, `HttpClientFactory`, `Platform`, `TimeUtil`, `LoginViewModel`

## Key Features

- **Authentication**: Token-based auth with `AuthRepository`
- **Secure Storage**: Platform-specific encrypted token storage
- **Network Layer**: Ktor client with authentication interceptor
- **Development Mode**: `DevConfig.kt` contains development credentials
- **UI Screens**: 
  - `LoginScreen` - Authentication with offline indicator
  - `CasesScreen` - User cases listing
  - `EntriesScreen` - Entry management with sync status
  - `ModernEntriesScreen` - Alternative modern UI for entries with edit support
  - `AddEntryScreen` - Entry creation and editing with audio recording support
  - `ConsultationScreen` - Consultation view with read-only inputs
- **Network Security**: Android uses `network_security_config.xml` for development
- **Offline Support**: 
  - Persistent login across app restarts
  - Local entry storage when offline
  - Automatic sync when connection restored
  - Visual indicators for sync status (SYNCED/PENDING/FAILED)
  - Network connectivity monitoring
  - Graceful offline error handling
- **Entry Management**:
  - Create new entries with timestamp, entity selection, and audio recording
  - Edit existing entries (only allowed before case end date)
  - Delete entries with confirmation dialog
  - Clean entry list display showing only date, time, and entity name
  - Audio fields automatically filtered from entry display to avoid showing raw base64 data

## Internationalization (i18n)

The app supports **multi-language functionality** using **Compose Multiplatform Resources** (v1.6.0+):

### Supported Languages
- **English** (default) - `values/strings.xml`
- **Italian** - `values-it/strings.xml`  
- **German** - `values-de/strings.xml`

### Implementation Details
- **Technology**: Compose Multiplatform Resources (official i18n solution)
- **Resource Package**: `de.zemki.metagcompose.resources`
- **Generated Classes**: Type-safe `Res.string.*` access
- **Resource Location**: `shared/src/commonMain/composeResources/`

### Usage in Code
```kotlin
// Import resources
import org.jetbrains.compose.resources.stringResource
import de.zemki.metagcompose.resources.Res
import de.zemki.metagcompose.resources.*

// Use in composables
Text(text = stringResource(Res.string.app_name))
Text(text = stringResource(Res.string.validation_required_fields, fieldNames))
```

### Key Translated Elements
- **App Name**: "MeTag" (renamed from "MetaG")
- **Login Screen**: All form fields, buttons, and messages
- **Entry Management**: Creation, editing, validation messages
- **Audio Components**: Recording, playback, and control texts
- **Error Messages**: Network, authentication, and validation errors
- **Case Information**: Submission deadlines and status messages
- **Navigation**: Back buttons, save/cancel actions

### Language Detection
- **Automatic**: Uses system language (Android/iOS settings)
- **Runtime Switching**: Not yet implemented (planned enhancement)
- **Testing**: Change device language in system settings

### Build Configuration
```kotlin
// shared/build.gradle.kts
compose.resources {
    publicResClass = false
    packageOfResClass = "de.zemki.metagcompose.resources"
    generateResClass = auto
}
```

### Resource Structure
```
shared/src/commonMain/composeResources/
├── drawable/           # Shared images (logo.png, header.jpg)
├── values/            # English strings (default)
├── values-it/         # Italian strings
└── values-de/         # German strings
```

## Important Notes

- Android min SDK: 24, target SDK: 35
- iOS deployment target: Check Xcode project settings
- iOS targets: iosX64, iosArm64, iosSimulatorArm64
- JVM target: 1.8
- Material3 design system is used for UI components
- Test files should be placed in appropriate test source sets

## Assets and Images

### Android Assets
Place Android-specific images and assets in:
```
MeTag/src/main/res/
├── drawable/          # Vector drawables (XML)
├── drawable-hdpi/     # High density images (72 dpi)
├── drawable-mdpi/     # Medium density images (48 dpi)
├── drawable-xhdpi/    # Extra high density images (96 dpi)
├── drawable-xxhdpi/   # Extra extra high density images (144 dpi)
├── drawable-xxxhdpi/  # Extra extra extra high density images (192 dpi)
├── mipmap-hdpi/       # App icons high density
├── mipmap-mdpi/       # App icons medium density
├── mipmap-xhdpi/      # App icons extra high density
├── mipmap-xxhdpi/     # App icons extra extra high density
├── mipmap-xxxhdpi/    # App icons extra extra extra high density
└── values/
    └── colors.xml     # Color resources
```

### iOS Assets
Place iOS-specific images and assets in:
```
MeTag/MeTag/Assets.xcassets/
├── AppIcon.appiconset/    # App icon set (all required sizes)
├── AccentColor.colorset/  # App accent color
└── [YourImageSet].imageset/  # Custom image sets
```

### Compose Multiplatform Shared Assets
For assets used across both platforms, place them in:
```
shared/src/commonMain/composeResources/
├── drawable/          # Platform-independent vector graphics
├── drawable-hdpi/     # High density bitmaps
├── drawable-mdpi/     # Medium density bitmaps  
├── drawable-xhdpi/    # Extra high density bitmaps
├── drawable-xxhdpi/   # Extra extra high density bitmaps
├── drawable-xxxhdpi/  # Extra extra extra high density bitmaps
├── font/              # Custom fonts (.ttf, .otf)
├── values/            # String resources, colors, etc.
└── files/             # Raw files (JSON, text files, etc.)
```

### Usage in Code
- **Android**: Use standard Android resource system (`R.drawable.image_name`)
- **iOS**: Access through iOS bundle (`UIImage(named: "image_name")`)
- **Compose Multiplatform**: Use Compose resources (`painterResource("drawable/image_name.png")`)

### Recommendations
- Use vector drawables (SVG) when possible for scalability
- Provide multiple density versions for bitmap images
- Keep file sizes optimized for mobile apps
- Use WebP format for better compression (Android) or HEIF (iOS) when supported
- App icons should follow platform guidelines (Material Design for Android, Human Interface Guidelines for iOS)

## Backend Integration

This mobile app connects to the **MetaG-Analyze** backend located at `/Users/belli/code/metag-analyze`.

### Backend Overview
- **Laravel-based** research platform for academic and media research studies
- **API Base URL**: Configured in `DevConfig.kt` (currently using development server)
- **Authentication**: Laravel Sanctum token-based authentication
- **API Versions**: Supports both V1 (legacy) and V2 (current) APIs with automatic detection

### Complete API Reference

#### Authentication (Unversioned)
```http
POST /api/login
Body: {
  "email": "user@example.com",
  "password": "password",
  "deviceID": "mobile_123456789",
  "datetime": 1704067200,
  "duration": 1704067200
}
Response: {
  "token": "Bearer_token_string",
  "case": { /* CaseData with project details */ },
  "custominputs": [
    {
      "name": "Field Label/Question Text",
      "type": "text|one choice|multiple choice|scale|audio recording",
      "mandatory": true|false,
      "numberofanswer": 0|number_of_options,
      "answers": ["option1", "option2", "..."]
    }
  ],
  "inputs": {
    "custominputs": "[ /* same as above custominputs array */ ]",
    "media": [/* array of media/entity options */],
    "entity": [/* same as media for V2 compatibility */],
    "entityName": "media|entity|custom_name",
    "useEntity": true|false
  },
  "api_version": "v1" | "v2" | null  // Optional field for version detection
}
```

#### V1 API Endpoints (Legacy - uses 'media' terminology)

**Project Information**
```http
GET /api/v1/project/{projectId}
Authorization: Bearer {token}
```

**Project Inputs Configuration**
```http
GET /api/v1/inputs/{projectId}
Authorization: Bearer {token}
```

**Get Entries for Case**
```http
GET /api/v1/entry/{caseId}
Authorization: Bearer {token}
Response: {
  "data": [
    {
      "id": 123,
      "begin": "2024-03-15 10:30:00.000000",
      "end": "2024-03-15 11:45:00.000000",
      "case_id": 456,
      "media_id": 789,  // Note: V1 uses 'media_id'
      "media_name": "Media Name",
      "inputs": "{\"field1\":\"value1\"}",
      "created_at": "2024-03-15 10:30:00",
      "updated_at": "2024-03-15 10:30:00"
    }
  ]
}
```

**Create Entry**
```http
POST /api/v1/cases/{caseId}/entries
Authorization: Bearer {token}
Body: {
  "begin": "2024-03-15 10:30:00.000000",
  "end": "2024-03-15 11:45:00.000000",
  "case_id": 456,
  "entity_id": "media_name_string" | 123,  // Can be string name or numeric ID
  "inputs": {
    "custom_field": "value",
    "scale_input": 3,
    "register audio": "base64_audio_data"
  }
}
Response: { "id": 789 }  // Returns only the created entry ID
```

**Update Entry**
```http
PATCH /api/v1/cases/{caseId}/entries/{entryId}
Authorization: Bearer {token}
Body: { /* same as create */ }
```

**Delete Entry**
```http
DELETE /api/v1/cases/{caseId}/entries/{entryId}
Authorization: Bearer {token}
```

#### V2 API Endpoints (Current - uses 'entity' terminology)

**Project Information**
```http
GET /api/v2/project/{projectId}
Authorization: Bearer {token}
```

**Project Inputs Configuration**
```http
GET /api/v2/inputs/{projectId}
Authorization: Bearer {token}
```

**Get Entries for Case**
```http
GET /api/v2/entry/{caseId}
Authorization: Bearer {token}
Response: {
  "data": [
    {
      "id": 123,
      "begin": "2024-03-15 10:30:00.000000",
      "end": "2024-03-15 11:45:00.000000",
      "case_id": 456,
      "entity_id": 789,  // Note: V2 uses 'entity_id'
      "entity_name": "Entity Name",
      "inputs": "{\"field1\":\"value1\"}",
      "created_at": "2024-03-15 10:30:00",
      "updated_at": "2024-03-15 11:30:00"
    }
  ]
}
```

**Create Entry**
```http
POST /api/v2/cases/{caseId}/entries
Authorization: Bearer {token}
Body: {
  "begin": "2024-03-15 10:30:00.000000",
  "end": "2024-03-15 11:45:00.000000",
  "case_id": 456,
  "entity_id": "entity_name_string" | 123,  // Can be string name or numeric ID
  "inputs": {
    "custom_field": "value",
    "scale_input": 3,
    "register audio": "base64_audio_data"
  }
}
Response: { "id": 789 }
```

**Update Entry**
```http
PATCH /api/v2/cases/{caseId}/entries/{entryId}
Authorization: Bearer {token}
Body: { /* same as create */ }
```

**Delete Entry**
```http
DELETE /api/v2/cases/{caseId}/entries/{entryId}
Authorization: Bearer {token}
```

**File Downloads (V2 Only)**
```http
GET /api/v2/files/{fileId}
Authorization: Bearer {token}
Response: {
  "data": "base64_encoded_file_content"
}
```

**Pages API (V2 Only)**
```http
GET    /api/v2/projects/{projectId}/pages
POST   /api/v2/projects/{projectId}/pages
GET    /api/v2/projects/{projectId}/pages/{pageId}
PATCH  /api/v2/projects/{projectId}/pages/{pageId}
DELETE /api/v2/projects/{projectId}/pages/{pageId}
PATCH  /api/v2/projects/{projectId}/pages/order
Authorization: Bearer {token}
```

### API Version Detection & Compatibility

The mobile app automatically detects API versions and adapts accordingly:

1. **Version Detection**: The `api_version` field in login response determines which API to use
   - If `api_version` is missing or null, defaults to "v1" for backward compatibility
   - Stores detected version in secure storage for subsequent requests

2. **Terminology Adaptation**: 
   - **V1 API**: Uses "media" terminology in UI and API calls
   - **V2 API**: Uses "entity" terminology in UI and API calls

3. **Endpoint Mapping**: Both versions use identical URL patterns for core operations:
   - Create: `POST /api/{version}/cases/{caseId}/entries`
   - Read: `GET /api/{version}/entry/{caseId}`
   - Update: `PATCH /api/{version}/cases/{caseId}/entries/{entryId}`
   - Delete: `DELETE /api/{version}/cases/{caseId}/entries/{entryId}`

4. **Data Model Compatibility**: 
   - V1 backend responses may lack `entity_name`, `use_entity`, `api_version` fields
   - V2 responses include all modern fields
   - Mobile app handles both formats gracefully with optional/nullable fields

5. **Field Validation Differences**:
   - **V1 API (Media)**: Media/entity field is **REQUIRED** for entry creation/update
   - **V2 API (Entity)**: Entity field is **OPTIONAL** and can be left blank
   - App validates mandatory fields before submission and shows error messages
   - Visual indicators (* red asterisk) show required fields in the UI

### Data Models

#### Project
- Contains custom input fields (questions)
- Defines entity usage and naming

#### Case
- Represents a participant in a project
- Has timing constraints (start/end dates)
- Contains multiple entries

#### Entry
- Data point with timestamp
- References optional entity/media
- Contains input field responses

### Backend Development
For backend changes, refer to `/Users/belli/code/metag-analyze/CLAUDE.md` which contains:
- Complete API documentation
- Database schema details
- Laravel development commands
- Testing instructions

## Known Issues & Future Improvements

### Current Suppressions & Warnings
- **Lint Suppression**: `MissingPermission` warnings suppressed in `NetworkMonitor.android.kt` - permission is declared in app module
- **Kotlin Beta Warnings**: `expect`/`actual` classes show beta warnings - can be suppressed with `-Xexpect-actual-classes` flag
- **Icon Deprecation**: Using deprecated `Icons.Filled.ArrowBack` and `Icons.Filled.ArrowForward` - should migrate to AutoMirrored versions
- **Memory Issues**: Gradle daemon experiencing heap space issues - needs `org.gradle.jvmargs` configuration in `gradle.properties`
- **KLIB Resolver Warning**: Duplicate library names in build - needs investigation

### Recent Updates

### Version 2.0 - Flutter Successor Release (2025-11-26)
- **Version Update**: Official successor to Flutter app (v1.4.0+31)
  - Android: 2.0 (build 30)
  - iOS: 2.0 (build 29)
- **Audio Recording Format Fix**: Changed from MPEG-4 container to AAC_ADTS
  - **Problem**: Backend detected audio as "video" due to MPEG-4 container format
  - **Solution**: Now uses `OutputFormat.AAC_ADTS` (Android) - pure audio format matching Flutter
  - File extension changed from `.m4a` to `.aac`
  - MIME type in Data URI: `data:audio/mp3;base64,...` (matches Flutter/backend expectation)
- **Timestamp Format - API Layer**:
  - **Sending**: Converts to Unix timestamps (Long) when creating/updating entries
  - **Receiving**: Entry model stores timestamps as String, converted from Long
  - **Display**: `ModernEntriesScreen` converts Unix timestamps to human-readable format
  - Uses `parseTimestamp()` utility to handle both old datetime strings and Unix timestamps
- **Date/Time Display Fixes**:
  - Entry list shows proper dates (e.g., "24 Nov 2025") instead of Unix timestamps
  - Time ranges display correctly (e.g., "10:30 - 11:45")
  - Duration calculation works with Unix timestamps
  - Date grouping properly extracts dates from timestamps
- **Audio Playback Error Handling**:
  - Fixed false "Audio playback failed" error when pausing
  - Added `CancellationException` handling to distinguish pause from actual errors
  - Only real playback errors now show error messages
- **Production Readiness**:
  - ⚠️ **Before App Store submission**: Change `GlobalConfig.USE_DEV_VALUES = false`
  - iOS: All required icons, privacy descriptions, and signing configured
  - Android: Version updated, ready for Play Store

### ESM/MART Project Detection and Redirect (2025-07-04)
- **Project Type Detection**: Added automatic detection of ESM (Experience Sampling Method) projects during login
  - `MartUtils.kt` detects projects with `type: "mart"` in the inputs JSON structure
  - Projects are identified by checking if the first input element has `"type": "mart"`
- **ESM Redirect Screen**: 
  - `MartRedirectScreen.kt` displays user-friendly message when ESM project access is attempted
  - Material Design 3 UI matching app design system with warning icon and info card
  - Explains ESM projects require dedicated MART mobile application
  - "Back to Login" button returns user to login screen
  - Placeholder for future "Download MART App" functionality
- **Authentication Flow Integration**:
  - `AuthRepository.kt` blocks login when MART project detected, emits `AuthError.MartProject`
  - `LoginViewModel.kt` handles MART project state with `isMartProject` flag
  - `App.kt` navigation logic properly handles MART redirect using `remember` for ViewModel persistence
- **Error Handling**: New `AuthError.MartProject` type for clean separation of MART vs standard project errors
- **Debug Logging**: Comprehensive logging for troubleshooting MART detection and navigation flow
- **Backend Integration**: Follows backend specification where MART projects are identified by inputs structure
- **User Experience**: Clear messaging that distinguishes between standard MetaG app and MART mobile app usage
- **Files Added/Modified**:
  - **New**: `MartUtils.kt` - MART project detection utility
  - **New**: `MartRedirectScreen.kt` - ESM redirect UI component  
  - **Modified**: `AuthRepository.kt` - Added MART detection in login flow
  - **Modified**: `ApiResult.kt` - Added `AuthError.MartProject` error type
  - **Modified**: `LoginViewModel.kt` - Added MART project state handling
  - **Modified**: `App.kt` - Integrated MART redirect navigation logic

### Mandatory Field Validation Implementation (2025-08-07)
- **Backend Integration**: Mandatory field information comes from backend login API response
  - Each field in `custominputs` array has `mandatory: true/false` property
  - Backend defines which fields are required per project configuration
  - Mobile app parses mandatory flags from backend JSON response
- **Complete Validation System**: Implemented comprehensive mandatory field validation for entry creation/update
- **API Version-Specific Validation**: 
  - **V1 API (Media)**: Media/entity field is **REQUIRED** - validation blocks submission if empty
  - **V2 API (Entity)**: Entity field is **OPTIONAL** - no validation enforcement
  - API version detection happens at login and determines validation behavior
- **Field Type Support**: Validation handles all input field types:
  - **TEXT**: Validates non-blank strings
  - **SCALE**: Validates values between 1-5 (0 treated as empty)
  - **ONE_CHOICE**: Validates non-blank selection
  - **MULTIPLE_CHOICE**: Validates at least one selection
  - **AUDIO_RECORDING**: Validates non-null audio data
- **User Experience**: 
  - Visual indicators with red asterisk (*) for mandatory fields
  - Clear error messages listing specific missing required fields
  - Entity field shows "(required)" for V1 API, "(optional)" for V2 API
  - Validation occurs before API submission to provide immediate feedback
- **Technical Implementation**:
  - Validation logic in `AddEntryScreen.kt` `createEntry()` function (lines 174-217)
  - `InputFieldParser.parseCustomInputs()` properly extracts mandatory flags from backend JSON
  - Uses `inputDef.mandatory` property from backend configuration
  - Error handling with user-friendly messages
  - Prevents API calls when validation fails
- **Files Modified**:
  - **Primary**: `AddEntryScreen.kt` - Added validation logic in `createEntry()` function
  - **Primary**: `InputFieldModels.kt` - Fixed parsing to extract mandatory field from backend JSON
  - **Secondary**: Entity selection components - Added visual required/optional indicators

### Previous Updates
- **Entry Editing**: Implemented entry editing functionality - entries can be edited before case end date, view-only after
- **UI Improvements**: Entry list now shows clean display (date, time, entity name) with audio data filtered out
- **Navigation**: Added edit mode navigation in App.kt with proper state management
- **Audio Player UI**: Redesigned audio recording component with modern player interface

### Audio Playback Implementation
- **Audio Preview**: Real audio playback is now fully implemented for Android
  - ✅ **Enhanced AudioPlayer Interface**: Added `playWithProgress()` method with real-time callbacks
  - ✅ **Real Audio Duration**: Gets actual audio duration from MediaPlayer (not hardcoded)
  - ✅ **Real-time Progress**: Shows actual playback position with 100ms updates
  - ✅ **Completion Handling**: Automatically resets UI when audio finishes playing
  - ✅ **MediaPlayer Integration**: Uses Android MediaPlayer with proper lifecycle management
  - ✅ **Base64 Decoding**: Properly decodes base64 audio data to temporary files
  - ✅ **Error Handling**: Comprehensive error handling with user-friendly messages
  - ✅ **Resource Management**: Automatic cleanup of MediaPlayer and temporary files
- **Platform Status**:
  - ✅ **Android**: Complete audio playback with progress tracking using MediaPlayer
  - ⏳ **iOS**: AudioPlayer interface ready, needs AVAudioPlayer implementation
- **Audio Quality**: 
  - Supports various audio formats (WAV, MP4, M4A) from base64 encoding
  - Proper audio file handling with temporary file creation and cleanup
  - Real audio duration detection (no more hardcoded 5-second limitation)
- **Simulator Audio**: Audio recording/playback may not work in simulators
  - Use physical devices for testing audio functionality
  - Simulator limitations affect both recording and playback

### iOS Limitations
- **Network Monitoring**: iOS `NetworkMonitor` implementation is simplified (always reports connected)
  - TODO: Implement proper Network framework integration for real connectivity detection
  - Current fallback ensures iOS builds work but lacks true offline detection

### iOS Keyboard Issue Fix (2025-08-07)
- **Issue**: Multiple iOS keyboard problems including text cut-off, keyboard not showing, layout shifts, and Chinese input method crashes
- **Root Cause**: Compose Multiplatform on iOS has known keyboard handling issues (GitHub issue #4016)
- **Comprehensive Solution Applied**:
  - **Custom Container View Controller**: Created `ComposeContainerViewController` that properly manages the Compose view as a child
  - **OnFocusBehavior Configuration**: Set to `DoNothing` to let Compose handle keyboard manually via `WindowInsets.ime`
  - **SwiftUI Safe Area Handling**: Added `.ignoresSafeArea(.keyboard, edges: .all)` to prevent keyboard layout shifts
  - **Proper IME Padding**: Using `windowInsetsPadding(WindowInsets.ime)` in Scaffold components
  - **Platform Layers**: Enabled for better touch handling in ComposeUIViewController
- **Key Implementation Details**:
  - `MainViewController.kt`: Configured `onFocusBehavior = OnFocusBehavior.DoNothing`
  - `ContentView.swift`: Custom container with proper responder chain and keyboard safe area handling
  - All text input screens use `WindowInsets.ime` for proper keyboard avoidance
- **Fixes Applied For**:
  - ✅ Text fields not showing keyboard
  - ✅ Keyboard appearing when no text field is focused  
  - ✅ Top app bar getting pushed off-screen by keyboard
  - ✅ Extra whitespace above keyboard
  - ✅ Chinese input method stability issues
  - ✅ Layout shifts when keyboard appears/disappears
- **Files Modified**: 
  - `MainViewController.kt` - Added proper focus behavior and platform layers
  - `ContentView.swift` - Custom container with keyboard safe area handling
  - `AddEntryScreen.kt`, `LoginScreen.kt` - Proper WindowInsets.ime usage
- **References**: 
  - GitHub Issue: https://github.com/JetBrains/compose-multiplatform/issues/4016
  - Workarounds based on community solutions from @oblakr24 and @gandrewstone

### Security Improvements Needed
- **iOS Token Storage**: Currently uses `NSUserDefaults` (unencrypted)
  - TODO: Migrate to iOS Keychain for secure token storage to match Android security level
- **Network Security**: Development mode allows cleartext traffic
  - TODO: Remove `android:usesCleartextTraffic="true"` for production builds

### Offline Feature Enhancements
- **Retry Logic**: Basic retry mechanism for failed syncs
  - TODO: Implement exponential backoff for better network efficiency
- **Conflict Resolution**: No handling for server-side data conflicts
  - TODO: Add conflict resolution for entries modified on both client and server
- **Cache Expiration**: No automatic cache cleanup
  - TODO: Implement cache expiration and cleanup policies
- **Bulk Sync**: Individual entry sync may be inefficient
  - TODO: Implement batch sync for better performance

### ESM/MART Project Integration
- **Detection Logic**: `MartUtils.isMartProject()` checks for `type: "mart"` in first input element
- **Authentication Flow**: Login is blocked immediately after backend response parsing if MART project detected
- **Navigation State**: Uses `remember` for `LoginViewModel` persistence to ensure proper state observation
- **Error Handling**: `AuthError.MartProject` provides specific error type for MART project access attempts  
- **User Feedback**: `MartRedirectScreen` explains ESM projects require MART mobile app, not standard MetaG app
- **Backend Compatibility**: Follows backend specification where MART projects have distinct inputs structure
- **Debugging**: Comprehensive logging at `MartUtils`, `AuthRepository`, `LoginViewModel`, and `App` levels

### Development Workflow
- **Error Handling**: Some network errors could be more specific
- **Loading States**: Some UI loading states could be more granular  
- **Testing**: Offline functionality needs comprehensive testing
  - TODO: Add unit tests for `OfflineEntryManager`
  - TODO: Add integration tests for sync scenarios
  - TODO: Add UI tests for offline mode
- **MART Testing**: Test MART project detection with debug credentials for validation

### Build Configuration
```gradle
// Add to gradle.properties to fix memory issues:
org.gradle.jvmargs=-Xmx4g -Xms1g -XX:MaxMetaspaceSize=512m

// Add to suppress Kotlin warnings:
kotlin.mpp.androidGradlePluginCompatibility.nowarn=true
```

### Performance Optimizations
- **Database**: Consider SQLDelight for more efficient local storage
- **Sync Frequency**: Implement smart sync intervals based on user activity
- **Memory**: Review object creation in hot paths (UI rendering)
- always use build-orchestrator agent to build apps

### Android Release Signing
- **Keystore Location**: `/Users/belli/metagPlaystoreKey.jks`
- **Key Alias**: `upload`
- **Password**: Same as key.properties
- **key.properties Location**: `MeTag/key.properties` (gitignored)
- **Application ID**: `de.unibremen.metag`
- **Build AAB**: `./gradlew :MeTag:bundleRelease`
- **Output**: `MeTag/build/outputs/bundle/release/MeTag-release.aab`

### iOS Release
- **Bundle ID**: `uni.bremen.metag`
- **Team ID**: `QK3V29YKEZ`
- **Build**: Archive in Xcode → Upload to App Store Connect