package com.nutrimuscle.app.ui.profile

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimuscle.app.data.local.prefs.GenderOption
import com.nutrimuscle.app.data.local.prefs.ManualTargetsOverride
import com.nutrimuscle.app.data.local.prefs.ProfileExtras
import com.nutrimuscle.app.data.local.prefs.ProfilePreferencesDataSource
import com.nutrimuscle.app.data.local.prefs.UnitSystem
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.domain.calculator.NutritionCalculator
import com.nutrimuscle.app.domain.model.ActivityLevel
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.FitnessGoal
import com.nutrimuscle.app.domain.model.Gender
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.NutritionTotals
import com.nutrimuscle.app.domain.model.ThemeMode
import com.nutrimuscle.app.domain.model.UserProfile
import com.nutrimuscle.app.notifications.MealReminderNotificationHelper
import com.nutrimuscle.app.notifications.MealReminderScheduler
import com.nutrimuscle.app.notifications.MealReminderSlot
import com.nutrimuscle.app.notifications.ReminderPreferencesDataSource
import com.nutrimuscle.app.notifications.ReminderSettings
import com.nutrimuscle.app.ui.haptics.HapticsPreferencesDataSource
import com.nutrimuscle.app.ui.theme.ThemePreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val extras: ProfileExtras = ProfileExtras(),
    val autoTargets: NutritionTargets = NutritionCalculator.calculateTargets(UserProfile()),
    val targets: NutritionTargets = NutritionCalculator.calculateTargets(UserProfile()),
    val reminders: ReminderSettings = ReminderSettings(),
    val hapticFeedbackEnabled: Boolean = true,
    val backupEvent: Pair<Int, Boolean>? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: NutriRepository,
    private val profilePrefs: ProfilePreferencesDataSource,
    private val reminderPrefs: ReminderPreferencesDataSource,
    private val reminderScheduler: MealReminderScheduler,
    private val notificationHelper: MealReminderNotificationHelper,
    private val hapticsPrefs: HapticsPreferencesDataSource,
    private val themePrefs: ThemePreferencesDataSource
) : ViewModel() {

    private val backupEventFlow = MutableStateFlow<Pair<Int, Boolean>?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.profileFlow,
        profilePrefs.extrasFlow,
        reminderPrefs.settingsFlow,
        hapticsPrefs.hapticFeedbackEnabledFlow,
        backupEventFlow
    ) { profile, extras, reminders, hapticEnabled, backupEvent ->
        val autoTargets = computeAutoTargets(profile, extras.genderOption)
        val ov = extras.targetOverrides
        val effectiveTargets = autoTargets.copy(
            calorieTarget = ov.calories ?: autoTargets.calorieTarget,
            proteinGrams = ov.proteinGrams ?: autoTargets.proteinGrams,
            carbsGrams = ov.carbsGrams ?: autoTargets.carbsGrams,
            fatGrams = ov.fatGrams ?: autoTargets.fatGrams
        )
        ProfileUiState(
            profile = profile,
            extras = extras,
            autoTargets = autoTargets,
            targets = effectiveTargets,
            reminders = reminders,
            hapticFeedbackEnabled = hapticEnabled,
            backupEvent = backupEvent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    private fun computeAutoTargets(profile: UserProfile, genderOption: GenderOption): NutritionTargets {
        if (genderOption != GenderOption.OTHER) {
            val mappedGender = if (genderOption == GenderOption.FEMALE) Gender.FEMALE else Gender.MALE
            return NutritionCalculator.calculateTargets(profile.copy(gender = mappedGender))
        }
        val male = NutritionCalculator.calculateTargets(profile.copy(gender = Gender.MALE))
        val female = NutritionCalculator.calculateTargets(profile.copy(gender = Gender.FEMALE))
        return male.copy(
            bmr = (male.bmr + female.bmr) / 2.0,
            tdee = (male.tdee + female.tdee) / 2.0,
            calorieTarget = (male.calorieTarget + female.calorieTarget) / 2.0,
            proteinGrams = (male.proteinGrams + female.proteinGrams) / 2.0,
            carbsGrams = (male.carbsGrams + female.carbsGrams) / 2.0,
            fatGrams = (male.fatGrams + female.fatGrams) / 2.0
        )
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            profilePrefs.setDisplayName(name)
        }
    }

    fun updateWeightKg(weightKg: Double): Boolean {
        if (weightKg !in 20.0..300.0) return false
        val rounded = (weightKg * 10.0).roundToInt() / 10.0
        viewModelScope.launch {
            val current = repository.profileFlow.first()
            repository.saveProfile(current.copy(weightKg = rounded))
        }
        return true
    }

    fun updateHeightCm(heightCm: Double): Boolean {
        if (heightCm !in 100.0..250.0) return false
        val rounded = (heightCm * 10.0).roundToInt() / 10.0
        viewModelScope.launch {
            val current = repository.profileFlow.first()
            repository.saveProfile(current.copy(heightCm = rounded))
        }
        return true
    }

    fun updateAge(age: Int): Boolean {
        if (age !in 10..100) return false
        viewModelScope.launch {
            val current = repository.profileFlow.first()
            repository.saveProfile(current.copy(age = age))
        }
        return true
    }

    fun selectGenderOption(option: GenderOption) {
        viewModelScope.launch {
            profilePrefs.setGenderOption(option)
        }
    }

    fun selectGoal(goal: FitnessGoal) {
        viewModelScope.launch {
            val current = repository.profileFlow.first()
            repository.saveProfile(current.copy(goal = goal))
        }
    }

    fun selectActivity(activity: ActivityLevel) {
        viewModelScope.launch {
            val current = repository.profileFlow.first()
            repository.saveProfile(current.copy(activityLevel = activity))
        }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun selectUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch {
            profilePrefs.setUnitSystem(unitSystem)
        }
    }

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
            themePrefs.setMode(themeMode)
        }
    }

    fun saveTargetOverrides(calories: Double?, protein: Double?, carbs: Double?, fat: Double?) {
        viewModelScope.launch {
            profilePrefs.setTargetOverrides(
                ManualTargetsOverride(
                    calories = calories?.coerceIn(800.0, 8000.0),
                    proteinGrams = protein?.coerceIn(20.0, 500.0),
                    carbsGrams = carbs?.coerceIn(0.0, 1200.0),
                    fatGrams = fat?.coerceIn(10.0, 350.0)
                )
            )
        }
    }

    fun resetTargetsToAuto() {
        viewModelScope.launch {
            profilePrefs.clearTargetOverrides()
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            hapticsPrefs.setHapticFeedbackEnabled(enabled)
        }
    }

    fun hasNotificationPermission(): Boolean = notificationHelper.hasNotificationPermission()

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()

    fun appNotificationSettingsIntent(): Intent = reminderScheduler.appNotificationSettingsIntent()

    fun exactAlarmSettingsIntent(): Intent = reminderScheduler.exactAlarmSettingsIntent()

    fun setMasterRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = reminderPrefs.settingsFlow.first()
            val updated = current.copy(masterEnabled = enabled)
            reminderPrefs.saveSettings(updated)
            if (enabled) {
                notificationHelper.createChannel()
                reminderScheduler.rescheduleAll(updated)
            } else {
                reminderScheduler.cancelAll()
            }
        }
    }

    fun setMealReminderEnabled(mealType: MealType, enabled: Boolean) {
        viewModelScope.launch {
            val current = reminderPrefs.settingsFlow.first()
            val updated = updateSlot(current, mealType) { it.copy(enabled = enabled) }
            reminderPrefs.saveSettings(updated)
            if (updated.masterEnabled) {
                if (enabled) {
                    reminderScheduler.scheduleSlot(updated.slotFor(mealType), forceTomorrow = false)
                } else {
                    reminderScheduler.cancelSlot(mealType)
                }
            }
        }
    }

    fun setMealReminderTime(mealType: MealType, hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = reminderPrefs.settingsFlow.first()
            val updated = updateSlot(current, mealType) {
                it.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59))
            }
            reminderPrefs.saveSettings(updated)
            if (updated.masterEnabled && updated.slotFor(mealType).enabled) {
                reminderScheduler.scheduleSlot(updated.slotFor(mealType), forceTomorrow = false)
            }
        }
    }

    fun sendTestNotification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val profile = repository.profileFlow.first()
            val extras = profilePrefs.extrasFlow.first()
            val autoTargets = computeAutoTargets(profile, extras.genderOption)
            val calTarget = extras.targetOverrides.calories ?: autoTargets.calorieTarget
            val todayMeals = repository.observeMealsForDate(LocalDate.now().toString()).first()
            val totals = todayMeals.fold(NutritionTotals()) { acc, m -> acc + m.totals }
            val kcalLeft = max(0, (calTarget - totals.calories).roundToInt())
            val mealType = MealType.defaultForHour(LocalTime.now().hour)
            val posted = notificationHelper.showMealReminder(
                mealType = mealType,
                caloriesLeft = kcalLeft,
                language = profile.language,
                isTest = true
            )
            onResult(posted)
        }
    }

    private fun updateSlot(
        settings: ReminderSettings,
        mealType: MealType,
        transform: (MealReminderSlot) -> MealReminderSlot
    ): ReminderSettings = when (mealType) {
        MealType.BREAKFAST -> settings.copy(breakfast = transform(settings.breakfast))
        MealType.LUNCH -> settings.copy(lunch = transform(settings.lunch))
        MealType.DINNER -> settings.copy(dinner = transform(settings.dinner))
        MealType.SNACK -> settings.copy(snack = transform(settings.snack))
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            runCatching { repository.importBackupJson(rawJson) }
                .onSuccess {
                    backupEventFlow.update { (it?.first ?: 0) + 1 to true }
                }
                .onFailure {
                    backupEventFlow.update { (it?.first ?: 0) + 1 to false }
                }
        }
    }
}
