package com.nutrimuscle.app.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hapticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nutrimuscle_haptics"
)

@Singleton
class HapticsPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val LAST_GOAL_CELEBRATED_DATE = stringPreferencesKey("last_goal_celebrated_date")
    }

    val hapticFeedbackEnabledFlow: Flow<Boolean> = context.hapticsDataStore.data.map { prefs ->
        prefs[Keys.HAPTIC_ENABLED] ?: true
    }

    val lastGoalCelebratedDateFlow: Flow<String> = context.hapticsDataStore.data.map { prefs ->
        prefs[Keys.LAST_GOAL_CELEBRATED_DATE] ?: ""
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.hapticsDataStore.edit { prefs ->
            prefs[Keys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun markGoalCelebrated(dateIso: String) {
        context.hapticsDataStore.edit { prefs ->
            prefs[Keys.LAST_GOAL_CELEBRATED_DATE] = dateIso
        }
    }
}

val LocalHapticsEnabled = compositionLocalOf { true }

val LocalHapticsPreferences = compositionLocalOf<HapticsPreferencesDataSource?> { null }

class Haptics(
    private val view: View,
    private val context: Context,
    private val enabled: Boolean
) {
    /** Light tick for chips, toggles, steppers, tab changes, week strip. */
    fun tick() {
        if (!enabled) return
        if (!performViewHaptic(HapticFeedbackConstants.CLOCK_TICK)) {
            performViewHaptic(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /** Medium click for buttons and add/remove actions. */
    fun click() {
        if (!enabled) return
        if (!performViewHaptic(HapticFeedbackConstants.CONTEXT_CLICK)) {
            performViewHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /** Strong confirm when a food is added to the log or daily goal is reached. */
    fun confirm() {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performViewHaptic(constant)
    }

    /** Reject feedback for validation errors. */
    fun reject() {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performViewHaptic(HapticFeedbackConstants.REJECT)
            return
        }
        vibrateOneShot(40L)
    }

    private fun performViewHaptic(feedbackConstant: Int): Boolean {
        if (!view.isHapticFeedbackEnabled) return false
        return view.performHapticFeedback(feedbackConstant)
    }

    @Suppress("DEPRECATION")
    private fun vibrateOneShot(durationMs: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            runCatching {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, context, enabled) {
        Haptics(view = view, context = context, enabled = enabled)
    }
}
