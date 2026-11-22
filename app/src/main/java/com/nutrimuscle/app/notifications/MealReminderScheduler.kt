package com.nutrimuscle.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.nutrimuscle.app.domain.model.MealType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun exactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            appNotificationSettingsIntent()
        }
    }

    fun appNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    fun rescheduleAll(settings: ReminderSettings) {
        if (!settings.masterEnabled) {
            cancelAll()
            return
        }
        settings.allSlots.forEach { slot ->
            if (slot.enabled) {
                scheduleSlot(slot, forceTomorrow = false)
            } else {
                cancelSlot(slot.mealType)
            }
        }
    }

    fun scheduleSlot(slot: MealReminderSlot, forceTomorrow: Boolean = false) {
        val now = ZonedDateTime.now()
        var trigger = now
            .withHour(slot.hour.coerceIn(0, 23))
            .withMinute(slot.minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)

        if (forceTomorrow || !trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }

        val triggerMillis = trigger.toInstant().toEpochMilli()
        val pending = pendingIntentFor(slot.mealType)

        if (canScheduleExactAlarms()) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pending
                )
            }.onFailure {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pending
                )
            }
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pending
            )
        }
    }

    fun cancelAll() {
        MealType.entries.forEach { cancelSlot(it) }
    }

    fun cancelSlot(mealType: MealType) {
        alarmManager.cancel(pendingIntentFor(mealType))
    }

    private fun pendingIntentFor(mealType: MealType): PendingIntent {
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
            putExtra(MealReminderNotificationHelper.EXTRA_REMINDER_MEAL_TYPE, mealType.name)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(mealType),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeFor(mealType: MealType): Int = when (mealType) {
        MealType.BREAKFAST -> 5101
        MealType.LUNCH -> 5102
        MealType.DINNER -> 5103
        MealType.SNACK -> 5104
    }

    companion object {
        const val ACTION_MEAL_REMINDER = "com.nutrimuscle.app.action.MEAL_REMINDER"
    }
}
