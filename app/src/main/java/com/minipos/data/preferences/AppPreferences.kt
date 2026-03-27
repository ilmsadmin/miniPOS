package com.minipos.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "minipos_prefs")

@Singleton
class AppPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_CURRENT_STORE_ID = stringPreferencesKey("current_store_id")
        private val KEY_CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOGIN_ATTEMPTS = intPreferencesKey("login_attempts")
        private val KEY_LOCK_UNTIL = longPreferencesKey("lock_until")
    }

    val currentStoreId: Flow<String?> = context.dataStore.data.map { it[KEY_CURRENT_STORE_ID] }
    val currentUserId: Flow<String?> = context.dataStore.data.map { it[KEY_CURRENT_USER_ID] }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[KEY_DEVICE_ID] }
    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_ONBOARDED] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    suspend fun setCurrentStore(storeId: String) {
        context.dataStore.edit { it[KEY_CURRENT_STORE_ID] = storeId }
    }

    suspend fun setCurrentUser(userId: String) {
        context.dataStore.edit { it[KEY_CURRENT_USER_ID] = userId }
    }

    suspend fun setDeviceId(deviceId: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = deviceId }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[KEY_IS_ONBOARDED] = value }
    }

    suspend fun getDeviceIdSync(): String {
        return context.dataStore.data.first()[KEY_DEVICE_ID] ?: ""
    }

    suspend fun getCurrentStoreIdSync(): String? {
        return context.dataStore.data.first()[KEY_CURRENT_STORE_ID]
    }

    suspend fun getCurrentUserIdSync(): String? {
        return context.dataStore.data.first()[KEY_CURRENT_USER_ID]
    }

    suspend fun getLoginAttempts(): Int {
        return context.dataStore.data.first()[KEY_LOGIN_ATTEMPTS] ?: 0
    }

    suspend fun setLoginAttempts(count: Int) {
        context.dataStore.edit { it[KEY_LOGIN_ATTEMPTS] = count }
    }

    suspend fun getLockUntil(): Long {
        return context.dataStore.data.first()[KEY_LOCK_UNTIL] ?: 0
    }

    suspend fun setLockUntil(timestamp: Long) {
        context.dataStore.edit { it[KEY_LOCK_UNTIL] = timestamp }
    }

    suspend fun logout() {
        // Only reset session state; keep current user ID so PIN screen knows which user to verify
        context.dataStore.edit {
            it[KEY_LOGIN_ATTEMPTS] = 0
            it[KEY_LOCK_UNTIL] = 0
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
