package com.nutrimuscle.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.domain.calculator.NutritionCalculator
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.NutritionTotals
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

@AndroidEntryPoint
class MealReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderPrefs: ReminderPreferencesDataSource

    @Inject
    lateinit var scheduler: MealReminderScheduler

    @Inject
    lateinit var notificationHelper: MealReminderNotificationHelper

    @Inject
    lateinit var repository: NutriRepository

    override fun onReceive(context: Context, intent: Intent) {
        val mealTypeName = intent.getStringExtra(MealReminderNotificationHelper.EXTRA_REMINDER_MEAL_TYPE)
        val mealType = mealTypeName?.let { runCatching { MealType.valueOf(it) }.getOrNull() } ?: return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = reminderPrefs.settingsFlow.first()
                val slot = settings.slotFor(mealType)

                // Always re-schedule for the next day if still enabled
                if (settings.masterEnabled && slot.enabled) {
                    scheduler.scheduleSlot(slot, forceTomorrow = true)
                } else {
                    return@launch
                }

                // Skip notification if this meal is already logged today
                val todayIso = LocalDate.now().toString()
                val todayMeals = repository.observeMealsForDate(todayIso).first()
                val alreadyLogged = todayMeals.any { it.mealType == mealType && it.items.isNotEmpty() }
                if (alreadyLogged) return@launch

                val profile = repository.profileFlow.first()
                val targets = NutritionCalculator.calculateTargets(profile)
                val totals = todayMeals.fold(NutritionTotals()) { acc, m -> acc + m.totals }
                val kcalLeft = max(0, (targets.calorieTarget - totals.calories).roundToInt())

                notificationHelper.showMealReminder(
                    mealType = mealType,
                    caloriesLeft = kcalLeft,
                    language = profile.language,
                    isTest = false
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
