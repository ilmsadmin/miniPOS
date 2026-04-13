package com.minipos.core.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.minipos.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme modes: follow system, force light, or force dark.
 */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String): ThemeMode = entries.find { it.key == key } ?: SYSTEM
    }
}

/**
 * Language options: follow system, English, or Vietnamese.
 */
enum class AppLanguage(val key: String, val localeTag: String) {
    SYSTEM("system", ""),
    ENGLISH("en", "en"),
    VIETNAMESE("vi", "vi");

    companion object {
        fun fromKey(key: String): AppLanguage = entries.find { it.key == key } ?: SYSTEM
    }
}

/**
 * Central manager for theme and language preferences.
 * Singleton injected by Hilt; holds reactive state that the root
 * Composable (MiniPosTheme) observes.
 */
@Singleton
class ThemeManager @Inject constructor(
    private val appPreferences: AppPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _language = MutableStateFlow(AppLanguage.SYSTEM)
    val language: StateFlow<AppLanguage> = _language

    init {
        // Load saved values
        scope.launch {
            _themeMode.value = ThemeMode.fromKey(appPreferences.themeMode.first())
            _language.value = AppLanguage.fromKey(appPreferences.language.first())
            applyLocale(_language.value)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch { appPreferences.setThemeMode(mode.key) }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        scope.launch { appPreferences.setLanguage(lang.key) }
        applyLocale(lang)
    }

    private fun applyLocale(lang: AppLanguage) {
        val localeList = if (lang == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(lang.localeTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
