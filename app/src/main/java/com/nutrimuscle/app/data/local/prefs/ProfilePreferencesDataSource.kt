package com.nutrimuscle.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nutrimuscle.app.domain.model.ActivityLevel
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.FitnessGoal
import com.nutrimuscle.app.domain.model.Gender
import com.nutrimuscle.app.domain.model.ThemeMode
import com.nutrimuscle.app.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrimuscle_profile")

enum class UnitSystem {
    METRIC,
    IMPERIAL
}

enum class GenderOption {
    MALE,
    FEMALE,
    OTHER
}

data class ManualTargetsOverride(
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null
) {
    val hasAnyOverride: Boolean
        get() = calories != null || proteinGrams != null || carbsGrams != null || fatGrams != null
}

data class ProfileExtras(
    val displayName: String = "Athlete",
    val genderOption: GenderOption = GenderOption.MALE,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val targetOverrides: ManualTargetsOverride = ManualTargetsOverride()
)

@Singleton
class ProfilePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val HEIGHT_CM = doublePreferencesKey("height_cm")
        val AGE = intPreferencesKey("age")
        val GENDER = stringPreferencesKey("gender")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val GOAL = stringPreferencesKey("goal")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val GENDER_OPTION = stringPreferencesKey("gender_option")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val OVERRIDE_CALORIES = doublePreferencesKey("override_calories")
        val OVERRIDE_PROTEIN = doublePreferencesKey("override_protein")
        val OVERRIDE_CARBS = doublePreferencesKey("override_carbs")
        val OVERRIDE_FAT = doublePreferencesKey("override_fat")
    }

    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            weightKg = prefs[Keys.WEIGHT_KG] ?: 65.0,
            heightCm = prefs[Keys.HEIGHT_CM] ?: 170.0,
            age = prefs[Keys.AGE] ?: 25,
            gender = runCatching { Gender.valueOf(prefs[Keys.GENDER] ?: "") }.getOrDefault(Gender.MALE),
            activityLevel = runCatching { ActivityLevel.valueOf(prefs[Keys.ACTIVITY_LEVEL] ?: "") }
                .getOrDefault(ActivityLevel.MODERATE),
            goal = runCatching { FitnessGoal.valueOf(prefs[Keys.GOAL] ?: "") }
                .getOrDefault(FitnessGoal.MUSCLE_GAIN),
            language = runCatching { AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: "") }
                .getOrDefault(AppLanguage.EN),
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }
                .getOrDefault(ThemeMode.SYSTEM)
        )
    }

    val extrasFlow: Flow<ProfileExtras> = context.profileDataStore.data.map { prefs ->
        val storedGender = runCatching { Gender.valueOf(prefs[Keys.GENDER] ?: "") }.getOrDefault(Gender.MALE)
        val fallbackGenderOpt = if (storedGender == Gender.FEMALE) GenderOption.FEMALE else GenderOption.MALE
        ProfileExtras(
            displayName = prefs[Keys.DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Athlete",
            genderOption = runCatching { GenderOption.valueOf(prefs[Keys.GENDER_OPTION] ?: "") }
                .getOrDefault(fallbackGenderOpt),
            unitSystem = runCatching { UnitSystem.valueOf(prefs[Keys.UNIT_SYSTEM] ?: "") }
                .getOrDefault(UnitSystem.METRIC),
            targetOverrides = ManualTargetsOverride(
                calories = prefs[Keys.OVERRIDE_CALORIES],
                proteinGrams = prefs[Keys.OVERRIDE_PROTEIN],
                carbsGrams = prefs[Keys.OVERRIDE_CARBS],
                fatGrams = prefs[Keys.OVERRIDE_FAT]
            )
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.WEIGHT_KG] = profile.weightKg
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.AGE] = profile.age
            prefs[Keys.GENDER] = profile.gender.name
            prefs[Keys.ACTIVITY_LEVEL] = profile.activityLevel.name
            prefs[Keys.GOAL] = profile.goal.name
            prefs[Keys.LANGUAGE] = profile.language.name
            prefs[Keys.THEME_MODE] = profile.themeMode.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.name
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setDisplayName(name: String) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = name.trim().ifBlank { "Athlete" }
        }
    }

    suspend fun setGenderOption(option: GenderOption) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.GENDER_OPTION] = option.name
            prefs[Keys.GENDER] = when (option) {
                GenderOption.FEMALE -> Gender.FEMALE.name
                GenderOption.MALE, GenderOption.OTHER -> Gender.MALE.name
            }
        }
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.UNIT_SYSTEM] = unitSystem.name
        }
    }

    suspend fun setTargetOverrides(overrides: ManualTargetsOverride) {
        context.profileDataStore.edit { prefs ->
            if (overrides.calories != null) prefs[Keys.OVERRIDE_CALORIES] = overrides.calories else prefs.remove(Keys.OVERRIDE_CALORIES)
            if (overrides.proteinGrams != null) prefs[Keys.OVERRIDE_PROTEIN] = overrides.proteinGrams else prefs.remove(Keys.OVERRIDE_PROTEIN)
            if (overrides.carbsGrams != null) prefs[Keys.OVERRIDE_CARBS] = overrides.carbsGrams else prefs.remove(Keys.OVERRIDE_CARBS)
            if (overrides.fatGrams != null) prefs[Keys.OVERRIDE_FAT] = overrides.fatGrams else prefs.remove(Keys.OVERRIDE_FAT)
        }
    }

    suspend fun clearTargetOverrides() {
        context.profileDataStore.edit { prefs ->
            prefs.remove(Keys.OVERRIDE_CALORIES)
            prefs.remove(Keys.OVERRIDE_PROTEIN)
            prefs.remove(Keys.OVERRIDE_CARBS)
            prefs.remove(Keys.OVERRIDE_FAT)
        }
    }
}
