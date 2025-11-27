package de.zemki.metagcompose.data.storage

import platform.Foundation.*
import platform.Security.*
import platform.CoreFoundation.*
import platform.posix.memcpy
import kotlinx.cinterop.*
import de.zemki.metagcompose.util.AppLogger

class TokenStorageImpl : TokenStorage {

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val keychainHelper = KeychainHelper()

    // Flag to track if migration has been attempted
    private var migrationAttempted: Boolean
        get() = userDefaults.boolForKey(MIGRATION_FLAG_KEY)
        set(value) { userDefaults.setBool(value, MIGRATION_FLAG_KEY) }

    init {
        // Perform migration from Flutter app on first launch
        if (!migrationAttempted) {
            performMigration()
            migrationAttempted = true
        }
    }

    /**
     * Migrates data from Flutter app (SharedPreferences/NSUserDefaults) to Keychain
     * Flutter stored: 'token', 'file_token', 'case' in NSUserDefaults (unencrypted)
     */
    private fun performMigration() {
        AppLogger.d("Starting migration from Flutter app storage...", tag = "TokenStorage.iOS")

        try {
            // Migrate auth token: 'token' → Keychain 'auth_token'
            userDefaults.stringForKey("token")?.let { oldToken ->
                AppLogger.d("Found Flutter auth token, migrating to Keychain...", tag = "TokenStorage.iOS")
                keychainHelper.save(TOKEN_KEY, oldToken)
                userDefaults.removeObjectForKey("token") // Clean up old data
                AppLogger.d("Auth token migrated successfully", tag = "TokenStorage.iOS")
            }

            // Migrate file token: 'file_token' → Keychain 'file_token'
            userDefaults.stringForKey("file_token")?.let { oldFileToken ->
                if (oldFileToken != "0") { // Flutter used "0" as default
                    AppLogger.d("Found Flutter file token, migrating to Keychain...", tag = "TokenStorage.iOS")
                    keychainHelper.save(FILE_TOKEN_KEY, oldFileToken)
                    userDefaults.removeObjectForKey("file_token")
                    AppLogger.d("File token migrated successfully", tag = "TokenStorage.iOS")
                }
            }

            // Migrate case data: 'case' → Keychain 'case_data'
            userDefaults.stringForKey("case")?.let { oldCase ->
                AppLogger.d("Found Flutter case data, migrating to Keychain...", tag = "TokenStorage.iOS")
                keychainHelper.save(CASE_DATA_KEY, oldCase)
                userDefaults.removeObjectForKey("case")
                AppLogger.d("Case data migrated successfully", tag = "TokenStorage.iOS")
            }

            AppLogger.d("Migration from Flutter app completed", tag = "TokenStorage.iOS")
        } catch (e: Exception) {
            AppLogger.e("Migration failed: ${e.message}", tag = "TokenStorage.iOS")
            // Continue anyway - user will need to login again
        }
    }

    // ============= SENSITIVE DATA (Keychain) =============

    override suspend fun saveToken(token: String) {
        keychainHelper.save(TOKEN_KEY, token)
    }

    override suspend fun getToken(): String? {
        return keychainHelper.get(TOKEN_KEY)
    }

    override suspend fun clearToken() {
        keychainHelper.delete(TOKEN_KEY)
    }

    override suspend fun saveFileToken(token: String) {
        keychainHelper.save(FILE_TOKEN_KEY, token)
    }

    override suspend fun getFileToken(): String? {
        return keychainHelper.get(FILE_TOKEN_KEY)
    }

    override suspend fun clearFileToken() {
        keychainHelper.delete(FILE_TOKEN_KEY)
    }

    // ============= NON-SENSITIVE DATA (UserDefaults) =============

    override suspend fun saveBaseUrl(url: String) {
        userDefaults.setObject(url, BASE_URL_KEY)
    }

    override suspend fun getBaseUrl(): String? {
        return userDefaults.stringForKey(BASE_URL_KEY)
    }

    override suspend fun saveCaseData(caseDataJson: String) {
        // Case data contains participant info - store in Keychain
        keychainHelper.save(CASE_DATA_KEY, caseDataJson)
    }

    override suspend fun getCaseData(): String? {
        return keychainHelper.get(CASE_DATA_KEY)
    }

    override suspend fun clearCaseData() {
        keychainHelper.delete(CASE_DATA_KEY)
    }

    override suspend fun saveInitialMessageShown(caseId: Int) {
        userDefaults.setBool(true, "${INITIAL_MESSAGE_KEY}_$caseId")
    }

    override suspend fun hasInitialMessageBeenShown(caseId: Int): Boolean {
        return userDefaults.boolForKey("${INITIAL_MESSAGE_KEY}_$caseId")
    }

    override suspend fun saveApiVersion(version: String) {
        userDefaults.setObject(version, API_VERSION_KEY)
    }

    override suspend fun getApiVersion(): String? {
        return userDefaults.stringForKey(API_VERSION_KEY)
    }

    override suspend fun saveCachedEntries(caseId: Int, entriesJson: String) {
        userDefaults.setObject(entriesJson, "${CACHED_ENTRIES_KEY}_$caseId")
    }

    override suspend fun getCachedEntries(caseId: Int): String? {
        return userDefaults.stringForKey("${CACHED_ENTRIES_KEY}_$caseId")
    }

    override suspend fun clearCachedEntries(caseId: Int) {
        userDefaults.removeObjectForKey("${CACHED_ENTRIES_KEY}_$caseId")
    }

    override suspend fun savePendingEntries(entriesJson: String) {
        userDefaults.setObject(entriesJson, PENDING_ENTRIES_KEY)
    }

    override suspend fun getPendingEntries(): String? {
        return userDefaults.stringForKey(PENDING_ENTRIES_KEY)
    }

    override suspend fun clearPendingEntries() {
        userDefaults.removeObjectForKey(PENDING_ENTRIES_KEY)
    }

    override suspend fun saveEmail(email: String) {
        userDefaults.setObject(email, EMAIL_KEY)
    }

    override suspend fun getEmail(): String? {
        return userDefaults.stringForKey(EMAIL_KEY)
    }

    override suspend fun clearEmail() {
        userDefaults.removeObjectForKey(EMAIL_KEY)
    }

    companion object {
        private const val TOKEN_KEY = "auth_token"
        private const val BASE_URL_KEY = "base_url"
        private const val CASE_DATA_KEY = "case_data"
        private const val INITIAL_MESSAGE_KEY = "initial_message_shown"
        private const val CACHED_ENTRIES_KEY = "cached_entries"
        private const val PENDING_ENTRIES_KEY = "pending_entries"
        private const val API_VERSION_KEY = "api_version"
        private const val FILE_TOKEN_KEY = "file_token"
        private const val EMAIL_KEY = "saved_email"
        private const val MIGRATION_FLAG_KEY = "keychain_migration_completed"
    }
}

/**
 * Helper class for iOS Keychain operations with error handling
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class KeychainHelper {

    private val serviceName = "de.zemki.metagcompose.secure"

    /**
     * Save data to Keychain with error handling and retry capability
     */
    fun save(key: String, value: String): Boolean {
        // First, delete any existing item (required for updates)
        delete(key)

        return memScoped {
            val valueData = value.encodeToByteArray().toNSData()

            // Create query dictionary for saving
            val query = CFDictionaryCreateMutable(
                null,
                4,
                null,
                null
            )

            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceName.toCFString())
            CFDictionaryAddValue(query, kSecAttrAccount, key.toCFString())
            CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(valueData))

            // Set accessibility - accessible after first unlock, for background notifications
            CFDictionaryAddValue(
                query,
                kSecAttrAccessible,
                kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            )

            val status = SecItemAdd(query, null)
            CFRelease(query)

            when (status) {
                errSecSuccess -> {
                    AppLogger.d("Keychain save successful for key: $key", tag = "KeychainHelper")
                    true
                }
                errSecDuplicateItem -> {
                    AppLogger.w("Duplicate item in Keychain for key: $key", tag = "KeychainHelper")
                    // Try updating instead
                    update(key, value)
                }
                errSecInteractionNotAllowed -> {
                    AppLogger.e("Keychain access denied (device locked?): $status", tag = "KeychainHelper")
                    throw KeychainAccessDeniedException("Device may be locked or Keychain access was denied")
                }
                else -> {
                    AppLogger.e("Keychain save failed for key: $key, status: $status", tag = "KeychainHelper")
                    false
                }
            }
        }
    }

    /**
     * Update existing Keychain item
     */
    private fun update(key: String, value: String): Boolean {
        return memScoped {
            val valueData = value.encodeToByteArray().toNSData()

            // Query to find the item
            val query = CFDictionaryCreateMutable(null, 3, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceName.toCFString())
            CFDictionaryAddValue(query, kSecAttrAccount, key.toCFString())

            // Attributes to update
            val attributes = CFDictionaryCreateMutable(null, 1, null, null)
            CFDictionaryAddValue(attributes, kSecValueData, CFBridgingRetain(valueData))

            val status = SecItemUpdate(query, attributes)
            CFRelease(query)
            CFRelease(attributes)

            when (status) {
                errSecSuccess -> {
                    AppLogger.d("Keychain update successful for key: $key", tag = "KeychainHelper")
                    true
                }
                else -> {
                    AppLogger.e("Keychain update failed for key: $key, status: $status", tag = "KeychainHelper")
                    false
                }
            }
        }
    }

    /**
     * Retrieve data from Keychain
     */
    fun get(key: String): String? {
        return memScoped {
            val query = CFDictionaryCreateMutable(null, 5, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceName.toCFString())
            CFDictionaryAddValue(query, kSecAttrAccount, key.toCFString())
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            CFRelease(query)

            when (status) {
                errSecSuccess -> {
                    val data = CFBridgingRelease(result.value) as? NSData
                    val value = data?.toByteArray()?.decodeToString()
                    AppLogger.d("Keychain get successful for key: $key", tag = "KeychainHelper")
                    value
                }
                errSecItemNotFound -> {
                    AppLogger.d("Keychain item not found for key: $key", tag = "KeychainHelper")
                    null
                }
                errSecInteractionNotAllowed -> {
                    AppLogger.e("Keychain access denied (device locked?): $status", tag = "KeychainHelper")
                    throw KeychainAccessDeniedException("Device may be locked or Keychain access was denied")
                }
                else -> {
                    AppLogger.e("Keychain get failed for key: $key, status: $status", tag = "KeychainHelper")
                    null
                }
            }
        }
    }

    /**
     * Delete data from Keychain
     */
    fun delete(key: String): Boolean {
        return memScoped {
            val query = CFDictionaryCreateMutable(null, 3, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceName.toCFString())
            CFDictionaryAddValue(query, kSecAttrAccount, key.toCFString())

            val status = SecItemDelete(query)
            CFRelease(query)

            when (status) {
                errSecSuccess -> {
                    AppLogger.d("Keychain delete successful for key: $key", tag = "KeychainHelper")
                    true
                }
                errSecItemNotFound -> {
                    // Not an error - item didn't exist
                    true
                }
                else -> {
                    AppLogger.w("Keychain delete failed for key: $key, status: $status", tag = "KeychainHelper")
                    false
                }
            }
        }
    }

    // Helper extensions
    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = this.size.toULong()
            )
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(this.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
            }
        }
    }

    private fun String.toCFString(): CFStringRef? {
        return CFBridgingRetain(this as NSString) as CFStringRef?
    }
}

/**
 * Custom exception for Keychain access errors
 */
class KeychainAccessDeniedException(message: String) : Exception(message)
