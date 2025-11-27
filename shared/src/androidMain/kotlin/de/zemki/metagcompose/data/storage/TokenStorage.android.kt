package de.zemki.metagcompose.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorageImpl(private val context: Context) : TokenStorage {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "metag_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override suspend fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }
    
    override suspend fun getToken(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
    }
    
    override suspend fun clearToken() {
        sharedPreferences.edit()
            .remove(TOKEN_KEY)
            .apply()
    }
    
    override suspend fun saveBaseUrl(url: String) {
        sharedPreferences.edit()
            .putString(BASE_URL_KEY, url)
            .apply()
    }
    
    override suspend fun getBaseUrl(): String? {
        return sharedPreferences.getString(BASE_URL_KEY, null)
    }
    
    override suspend fun saveCaseData(caseDataJson: String) {
        sharedPreferences.edit()
            .putString(CASE_DATA_KEY, caseDataJson)
            .apply()
    }
    
    override suspend fun getCaseData(): String? {
        return sharedPreferences.getString(CASE_DATA_KEY, null)
    }
    
    override suspend fun clearCaseData() {
        sharedPreferences.edit()
            .remove(CASE_DATA_KEY)
            .apply()
    }
    
    override suspend fun saveInitialMessageShown(caseId: Int) {
        sharedPreferences.edit()
            .putBoolean("${INITIAL_MESSAGE_KEY}_$caseId", true)
            .apply()
    }
    
    override suspend fun hasInitialMessageBeenShown(caseId: Int): Boolean {
        return sharedPreferences.getBoolean("${INITIAL_MESSAGE_KEY}_$caseId", false)
    }
    
    override suspend fun saveApiVersion(version: String) {
        sharedPreferences.edit()
            .putString(API_VERSION_KEY, version)
            .apply()
    }
    
    override suspend fun getApiVersion(): String? {
        return sharedPreferences.getString(API_VERSION_KEY, null)
    }

    override suspend fun saveFileToken(token: String) {
        sharedPreferences.edit()
            .putString(FILE_TOKEN_KEY, token)
            .apply()
    }

    override suspend fun getFileToken(): String? {
        return sharedPreferences.getString(FILE_TOKEN_KEY, null)
    }

    override suspend fun clearFileToken() {
        sharedPreferences.edit()
            .remove(FILE_TOKEN_KEY)
            .apply()
    }

    override suspend fun saveCachedEntries(caseId: Int, entriesJson: String) {
        sharedPreferences.edit()
            .putString("${CACHED_ENTRIES_KEY}_$caseId", entriesJson)
            .apply()
    }
    
    override suspend fun getCachedEntries(caseId: Int): String? {
        return sharedPreferences.getString("${CACHED_ENTRIES_KEY}_$caseId", null)
    }
    
    override suspend fun clearCachedEntries(caseId: Int) {
        sharedPreferences.edit()
            .remove("${CACHED_ENTRIES_KEY}_$caseId")
            .apply()
    }
    
    override suspend fun savePendingEntries(entriesJson: String) {
        sharedPreferences.edit()
            .putString(PENDING_ENTRIES_KEY, entriesJson)
            .apply()
    }
    
    override suspend fun getPendingEntries(): String? {
        return sharedPreferences.getString(PENDING_ENTRIES_KEY, null)
    }
    
    override suspend fun clearPendingEntries() {
        sharedPreferences.edit()
            .remove(PENDING_ENTRIES_KEY)
            .apply()
    }

    override suspend fun saveEmail(email: String) {
        sharedPreferences.edit()
            .putString(EMAIL_KEY, email)
            .apply()
    }

    override suspend fun getEmail(): String? {
        return sharedPreferences.getString(EMAIL_KEY, null)
    }

    override suspend fun clearEmail() {
        sharedPreferences.edit()
            .remove(EMAIL_KEY)
            .apply()
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
    }
}