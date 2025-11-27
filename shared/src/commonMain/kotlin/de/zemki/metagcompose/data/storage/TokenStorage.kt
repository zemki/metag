package de.zemki.metagcompose.data.storage

interface TokenStorage {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
    suspend fun saveBaseUrl(url: String)
    suspend fun getBaseUrl(): String?
    suspend fun saveCaseData(caseDataJson: String)
    suspend fun getCaseData(): String?
    suspend fun clearCaseData()
    suspend fun saveInitialMessageShown(caseId: Int)
    suspend fun hasInitialMessageBeenShown(caseId: Int): Boolean
    suspend fun saveApiVersion(version: String)
    suspend fun getApiVersion(): String?
    suspend fun saveFileToken(token: String)
    suspend fun getFileToken(): String?
    suspend fun clearFileToken()

    // Entry caching for offline support
    suspend fun saveCachedEntries(caseId: Int, entriesJson: String)
    suspend fun getCachedEntries(caseId: Int): String?
    suspend fun clearCachedEntries(caseId: Int)
    suspend fun savePendingEntries(entriesJson: String)
    suspend fun getPendingEntries(): String?
    suspend fun clearPendingEntries()

    // Email persistence for "remember me" feature
    suspend fun saveEmail(email: String)
    suspend fun getEmail(): String?
    suspend fun clearEmail()
}