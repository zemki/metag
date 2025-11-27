# API Integration

## Base URL

Configured in `GlobalConfig.kt`:
```kotlin
// Development
const val DEV_SERVER_URL = "https://dev.example.com/"

// Production
"https://prod.example.com/"
```

Toggle: `USE_DEV_VALUES = true/false`

## Authentication Flow

```
1. POST /api/login (email, password, deviceID)
         ↓
2. Receive: token, case data, custom inputs, api_version
         ↓
3. Store token in TokenStorage (encrypted)
         ↓
4. All subsequent requests include: Authorization: Bearer {token}
```

## Token Handling

### Storage
- **Android**: `EncryptedSharedPreferences`
- **iOS**: Keychain (currently `NSUserDefaults` - needs migration)

### Repository Pattern
```kotlin
// Save token after login
tokenStorage.saveToken(response.token)
tokenStorage.saveApiVersion(response.api_version ?: "v1")

// Use in requests
val token = tokenStorage.getToken()
httpClient.get(url) {
    header("Authorization", "Bearer $token")
}
```

## API Version Detection

- Login response includes optional `api_version` field
- If missing → defaults to "v1"
- Stored and used for all subsequent API calls

| Version | Entity Term | Required Fields |
|---------|-------------|-----------------|
| v1 | "media" | Media selection required |
| v2 | "entity" | Entity selection optional |

## Error Handling

```kotlin
sealed class ApiResult<T> {
    data class Success<T>(val data: T)
    data class NetworkError(val message: String)
    data class AuthError(val type: AuthErrorType)
}
```

## Offline Mode

1. `NetworkMonitor` detects connectivity
2. Offline → entries saved to `OfflineEntryManager`
3. Online → `syncPendingEntries()` uploads pending
4. UI shows sync status: SYNCED / PENDING / FAILED

## Related Files

| File | Purpose |
|------|---------|
| `AuthRepository.kt` | Login/logout logic |
| `EntriesRepository.kt` | Entry CRUD operations |
| `TokenStorage.kt` | Token persistence interface |
| `HttpClientFactory.kt` | Ktor client configuration |
