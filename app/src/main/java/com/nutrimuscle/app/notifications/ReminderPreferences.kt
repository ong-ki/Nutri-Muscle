package com.nutrimuscle.app.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nutrimuscle.app.domain.model.MealType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nutrimuscle_reminders"
)

data class MealReminderSlot(
    val mealType: MealType,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int
)

data class ReminderSettings(
    val masterEnabled: Boolean = false,
    val breakfast: MealReminderSlot = MealReminderSlot(MealType.BREAKFAST, true, 8, 0),
    val lunch: MealReminderSlot = MealReminderSlot(MealType.LUNCH, true, 12, 30),
    val dinner: MealReminderSlot = MealReminderSlot(MealType.DINNER, true, 19, 0),
    val snack: MealReminderSlot = MealReminderSlot(MealType.SNACK, false, 21, 0)
) {
    val allSlots: List<MealReminderSlot>
        get() = listOf(breakfast, lunch, dinner, snack)

    fun slotFor(mealType: MealType): MealReminderSlot = when (mealType) {
        MealType.BREAKFAST -> breakfast
        MealType.LUNCH -> lunch
        MealType.DINNER -> dinner
        MealType.SNACK -> snack
    }
}

@Singleton
class ReminderPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")

        val BREAKFAST_ENABLED = booleanPreferencesKey("breakfast_enabled")
        val BREAKFAST_HOUR = intPreferencesKey("breakfast_hour")
        val BREAKFAST_MINUTE = intPreferencesKey("breakfast_minute")

        val LUNCH_ENABLED = booleanPreferencesKey("lunch_enabled")
        val LUNCH_HOUR = intPreferencesKey("lunch_hour")
        val LUNCH_MINUTE = intPreferencesKey("lunch_minute")

        val DINNER_ENABLED = booleanPreferencesKey("dinner_enabled")
        val DINNER_HOUR = intPreferencesKey("dinner_hour")
        val DINNER_MINUTE = intPreferencesKey("dinner_minute")

        val SNACK_ENABLED = booleanPreferencesKey("snack_enabled")
        val SNACK_HOUR = intPreferencesKey("snack_hour")
        val SNACK_MINUTE = intPreferencesKey("snack_minute")
    }

    val settingsFlow: Flow<ReminderSettings> = context.reminderDataStore.data.map { prefs ->
        ReminderSettings(
            masterEnabled = prefs[Keys.MASTER_ENABLED] ?: false,
            breakfast = MealReminderSlot(
                mealType = MealType.BREAKFAST,
                enabled = prefs[Keys.BREAKFAST_ENABLED] ?: true,
                hour = prefs[Keys.BREAKFAST_HOUR] ?: 8,
                minute = prefs[Keys.BREAKFAST_MINUTE] ?: 0
            ),
            lunch = MealReminderSlot(
                mealType = MealType.LUNCH,
                enabled = prefs[Keys.LUNCH_ENABLED] ?: true,
                hour = prefs[Keys.LUNCH_HOUR] ?: 12,
                minute = prefs[Keys.LUNCH_MINUTE] ?: 30
            ),
            dinner = MealReminderSlot(
                mealType = MealType.DINNER,
                enabled = prefs[Keys.DINNER_ENABLED] ?: true,
                hour = prefs[Keys.DINNER_HOUR] ?: 19,
                minute = prefs[Keys.DINNER_MINUTE] ?: 0
            ),
            snack = MealReminderSlot(
                mealType = MealType.SNACK,
                enabled = prefs[Keys.SNACK_ENABLED] ?: false,
                hour = prefs[Keys.SNACK_HOUR] ?: 21,
                minute = prefs[Keys.SNACK_MINUTE] ?: 0
            )
        )
    }

    suspend fun saveSettings(settings: ReminderSettings) {
        context.reminderDataStore.edit { prefs ->
            prefs[Keys.MASTER_ENABLED] = settings.masterEnabled

            prefs[Keys.BREAKFAST_ENABLED] = settings.breakfast.enabled
            prefs[Keys.BREAKFAST_HOUR] = settings.breakfast.hour
            prefs[Keys.BREAKFAST_MINUTE] = settings.breakfast.minute

            prefs[Keys.LUNCH_ENABLED] = settings.lunch.enabled
            prefs[Keys.LUNCH_HOUR] = settings.lunch.hour
            prefs[Keys.LUNCH_MINUTE] = settings.lunch.minute

            prefs[Keys.DINNER_ENABLED] = settings.dinner.enabled
            prefs[Keys.DINNER_HOUR] = settings.dinner.hour
            prefs[Keys.DINNER_MINUTE] = settings.dinner.minute

            prefs[Keys.SNACK_ENABLED] = settings.snack.enabled
            prefs[Keys.SNACK_HOUR] = settings.snack.hour
            prefs[Keys.SNACK_MINUTE] = settings.snack.minute
        }
    }
}
