package com.minipos.core.rating

import com.minipos.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RatingManager — Manages the smart app-rating prompt strategy.
 *
 * Strategy:
 *  - After every successful action (order complete, product saved, etc.),
 *    increment a counter.
 *  - Show the rating dialog when:
 *      1. User has NOT already rated (hasRated == false)
 *      2. successActionCount reaches a threshold (3 for first time, 10 for subsequent)
 *      3. At least 3 days have passed since last dismiss
 *      4. User has dismissed < 3 times total (after that, stop asking)
 *
 * Flow:
 *  - User sees star picker (1-5)
 *  - 1-3 stars → open email to zenixhq.com@gmail.com with feedback
 *  - 4-5 stars → launch Google Play In-App Review
 */
@Singleton
class RatingManager @Inject constructor(
    private val appPreferences: AppPreferences,
) {
    companion object {
        private const val FIRST_PROMPT_THRESHOLD = 3     // show after 3 successful actions
        private const val SUBSEQUENT_THRESHOLD = 10       // show again every 10 actions after dismiss
        private const val MAX_DISMISS_COUNT = 3           // stop asking after 3 dismissals
        private const val MIN_DAYS_BETWEEN_PROMPTS = 3L   // minimum days between prompts
        private const val MILLIS_PER_DAY = 86_400_000L
    }

    /** Emits `true` when the rating dialog should be displayed */
    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog: StateFlow<Boolean> = _showRatingDialog

    /**
     * Call this after a successful user action (order completed, product saved, etc.)
     * Returns true if the rating dialog should be shown.
     */
    suspend fun onSuccessAction(): Boolean {
        // Already rated → never show again
        if (appPreferences.hasRated()) return false

        // Dismissed too many times → stop pestering
        val dismissCount = appPreferences.getRatingDismissCount()
        if (dismissCount >= MAX_DISMISS_COUNT) return false

        // Cooldown period check
        val lastDismiss = appPreferences.getLastRatingDismissTime()
        val now = System.currentTimeMillis()
        if (lastDismiss > 0 && (now - lastDismiss) < MIN_DAYS_BETWEEN_PROMPTS * MILLIS_PER_DAY) {
            // Still in cooldown → just increment counter silently
            appPreferences.incrementSuccessActionCount()
            return false
        }

        val count = appPreferences.incrementSuccessActionCount()

        // Determine threshold based on whether user has dismissed before
        val threshold = if (dismissCount == 0) FIRST_PROMPT_THRESHOLD else SUBSEQUENT_THRESHOLD

        return if (count >= threshold) {
            _showRatingDialog.value = true
            true
        } else {
            false
        }
    }

    /** User dismissed the rating dialog (tapped "Later" or outside) */
    suspend fun onDismiss() {
        _showRatingDialog.value = false
        appPreferences.setRatingDismissCount(appPreferences.getRatingDismissCount() + 1)
        appPreferences.setLastRatingDismissTime(System.currentTimeMillis())
        // Reset action counter so it starts counting again toward next threshold
        appPreferences.resetSuccessActionCount()
    }

    /** User completed the rating (either sent email or completed in-app review) */
    suspend fun onRated(stars: Int) {
        _showRatingDialog.value = false
        appPreferences.setHasRated(true)
        appPreferences.setRatingGivenStars(stars)
    }

    /** Hide the dialog without recording a dismiss (e.g., navigation change) */
    fun hide() {
        _showRatingDialog.value = false
    }

    /** Force-show the rating dialog (for testing / Settings link). Bypasses all checks. */
    fun forceShow() {
        _showRatingDialog.value = true
    }
}
