package com.nutrimuscle.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.notifications.MealReminderNotificationHelper
import com.nutrimuscle.app.ui.haptics.HapticsPreferencesDataSource
import com.nutrimuscle.app.ui.haptics.LocalHapticsEnabled
import com.nutrimuscle.app.ui.haptics.LocalHapticsPreferences
import com.nutrimuscle.app.ui.navigation.NutriMuscleAppRoot
import com.nutrimuscle.app.ui.theme.NutriMuscleTheme
import com.nutrimuscle.app.ui.theme.ThemePreferencesDataSource
import com.nutrimuscle.app.ui.theme.ThemeState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: NutriRepository

    @Inject
    lateinit var hapticsPrefs: HapticsPreferencesDataSource

    @Inject
    lateinit var themePrefs: ThemePreferencesDataSource

    private val pendingReminderMealState = mutableStateOf<MealType?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReminderIntent(intent)

        lifecycleScope.launch {
            repository.profileFlow
                .map { it.language }
                .distinctUntilChanged()
                .collect { lang ->
                    applyLocale(lang)
                }
        }

        setContent {
            val themeState by themePrefs.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
            val hapticsEnabled by hapticsPrefs.hapticFeedbackEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
            NutriMuscleTheme(
                preset = themeState.preset,
                themeMode = themeState.mode
            ) {
                CompositionLocalProvider(
                    LocalHapticsEnabled provides hapticsEnabled,
                    LocalHapticsPreferences provides hapticsPrefs
                ) {
                    NutriMuscleAppRoot(
                        pendingReminderMealType = pendingReminderMealState.value,
                        onReminderConsumed = { pendingReminderMealState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    private fun handleReminderIntent(intent: Intent?) {
        val mealName = intent?.getStringExtra(MealReminderNotificationHelper.EXTRA_REMINDER_MEAL_TYPE)
            ?: return
        val mealType = runCatching { MealType.valueOf(mealName) }.getOrNull() ?: return
        pendingReminderMealState.value = mealType
        intent.removeExtra(MealReminderNotificationHelper.EXTRA_REMINDER_MEAL_TYPE)
    }

    private fun applyLocale(language: AppLanguage) {
        val targetLocales = LocaleListCompat.forLanguageTags(language.tag)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.toLanguageTags() != targetLocales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(targetLocales)
        }
    }
}
