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
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")       // "system", "light", "dark"
        private val KEY_LANGUAGE = stringPreferencesKey("language")            // "system", "en", "vi"
        private val KEY_SETUP_GUIDE_DISMISSED = booleanPreferencesKey("setup_guide_dismissed")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_LAST_SEEN_TODAY_ORDERS = intPreferencesKey("last_seen_today_orders")
        // Rating / Review
        private val KEY_HAS_RATED = booleanPreferencesKey("has_rated")
        private val KEY_RATING_DISMISS_COUNT = intPreferencesKey("rating_dismiss_count")
        private val KEY_SUCCESS_ACTION_COUNT = intPreferencesKey("success_action_count")
        private val KEY_LAST_RATING_DISMISS_TIME = longPreferencesKey("last_rating_dismiss_time")
        private val KEY_RATING_GIVEN_STARS = intPreferencesKey("rating_given_stars")
    }

    val currentStoreId: Flow<String?> = context.dataStore.data.map { it[KEY_CURRENT_STORE_ID] }
    val currentUserId: Flow<String?> = context.dataStore.data.map { it[KEY_CURRENT_USER_ID] }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[KEY_DEVICE_ID] }
    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_ONBOARDED] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val themeMode: Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "system" }
    val setupGuideDismissed: Flow<Boolean> = context.dataStore.data.map { it[KEY_SETUP_GUIDE_DISMISSED] ?: false }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }

    suspend fun setCurrentStore(storeId: String) {
        context.dataStore.edit { it[KEY_CURRENT_STORE_ID] = storeId }
    }

    suspend fun setCurrentUser(userId: String) {
        context.dataStore.edit { it[KEY_CURRENT_USER_ID] = userId }
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { it[KEY_IS_LOGGED_IN] = value }
    }

    suspend fun isLoggedInSync(): Boolean {
        return context.dataStore.data.first()[KEY_IS_LOGGED_IN] ?: false
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

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setSetupGuideDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[KEY_SETUP_GUIDE_DISMISSED] = dismissed }
    }

    suspend fun getLastSeenTodayOrders(): Int {
        return context.dataStore.data.first()[KEY_LAST_SEEN_TODAY_ORDERS] ?: 0
    }

    suspend fun setLastSeenTodayOrders(count: Int) {
        context.dataStore.edit { it[KEY_LAST_SEEN_TODAY_ORDERS] = count }
    }

    // ── Rating / Review ──

    suspend fun hasRated(): Boolean =
        context.dataStore.data.first()[KEY_HAS_RATED] ?: false

    suspend fun setHasRated(value: Boolean) {
        context.dataStore.edit { it[KEY_HAS_RATED] = value }
    }

    suspend fun getRatingDismissCount(): Int =
        context.dataStore.data.first()[KEY_RATING_DISMISS_COUNT] ?: 0

    suspend fun setRatingDismissCount(count: Int) {
        context.dataStore.edit { it[KEY_RATING_DISMISS_COUNT] = count }
    }

    suspend fun getSuccessActionCount(): Int =
        context.dataStore.data.first()[KEY_SUCCESS_ACTION_COUNT] ?: 0

    suspend fun incrementSuccessActionCount(): Int {
        val current = getSuccessActionCount() + 1
        context.dataStore.edit { it[KEY_SUCCESS_ACTION_COUNT] = current }
        return current
    }

    suspend fun resetSuccessActionCount() {
        context.dataStore.edit { it[KEY_SUCCESS_ACTION_COUNT] = 0 }
    }

    suspend fun getLastRatingDismissTime(): Long =
        context.dataStore.data.first()[KEY_LAST_RATING_DISMISS_TIME] ?: 0L

    suspend fun setLastRatingDismissTime(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_RATING_DISMISS_TIME] = timestamp }
    }

    suspend fun getRatingGivenStars(): Int =
        context.dataStore.data.first()[KEY_RATING_GIVEN_STARS] ?: 0

    suspend fun setRatingGivenStars(stars: Int) {
        context.dataStore.edit { it[KEY_RATING_GIVEN_STARS] = stars }
    }

    suspend fun logout() {
        // Only reset session state; keep current user ID so PIN screen knows which user to verify
        context.dataStore.edit {
            it[KEY_LOGIN_ATTEMPTS] = 0
            it[KEY_LOCK_UNTIL] = 0
            it[KEY_IS_LOGGED_IN] = false
        }
    }

    /** Full logout — clears current user so Login screen is shown */
    suspend fun clearCurrentUser() {
        context.dataStore.edit {
            it.remove(KEY_CURRENT_USER_ID)
            it[KEY_LOGIN_ATTEMPTS] = 0
            it[KEY_LOCK_UNTIL] = 0
            it[KEY_IS_LOGGED_IN] = false
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
