package com.nutrimuscle.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nutrimuscle.app.MainActivity
import com.nutrimuscle.app.R
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.MealType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "meal_reminders"
        const val EXTRA_REMINDER_MEAL_TYPE = "extra_reminder_meal_type"
        private const val ACCENT_COLOR_PEACH = 0xFFEBA77C.toInt()
    }

    fun createChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meal reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminders to log breakfast, lunch, dinner, and snacks"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun showMealReminder(
        mealType: MealType,
        caloriesLeft: Int,
        language: AppLanguage,
        isTest: Boolean = false
    ): Boolean {
        if (!hasNotificationPermission()) return false
        createChannel()

        val fmt = NumberFormat.getIntegerInstance(Locale.US)
        val kcalStr = fmt.format(caloriesLeft.coerceAtLeast(0))

        val title = if (language == AppLanguage.MM) {
            if (isTest) "NutriMuscle · စမ်းသပ်အသိပေးချက်" else "NutriMuscle"
        } else {
            if (isTest) "NutriMuscle · Test reminder" else "NutriMuscle"
        }

        val body = buildReminderBody(mealType, kcalStr, language)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_MEAL_TYPE, mealType.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdFor(mealType, isTest),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_meal)
            .setColor(ACCENT_COLOR_PEACH)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(
                notificationIdFor(mealType, isTest),
                notification
            )
            true
        }.getOrDefault(false)
    }

    private fun buildReminderBody(
        mealType: MealType,
        kcalStr: String,
        language: AppLanguage
    ): String {
        return if (language == AppLanguage.MM) {
            when (mealType) {
                MealType.BREAKFAST -> "မနက်စာ မှတ်ဖို့ မမေ့နဲ့ · $kcalStr kcal left"
                MealType.LUNCH -> "နေ့လယ်စာ မှတ်ဖို့ မမေ့နဲ့ · $kcalStr kcal left"
                MealType.DINNER -> "ညနေစာ မှတ်ဖို့ မမေ့နဲ့ · $kcalStr kcal left"
                MealType.SNACK -> "ညနေသရေစာ မှတ်ဖို့ မမေ့နဲ့ · $kcalStr kcal left"
            }
        } else {
            when (mealType) {
                MealType.BREAKFAST -> "Don't forget Breakfast · $kcalStr kcal left"
                MealType.LUNCH -> "Don't forget Lunch · $kcalStr kcal left"
                MealType.DINNER -> "Don't forget Dinner · $kcalStr kcal left"
                MealType.SNACK -> "Don't forget Evening snack · $kcalStr kcal left"
            }
        }
    }

    private fun notificationIdFor(mealType: MealType, isTest: Boolean): Int {
        val base = when (mealType) {
            MealType.BREAKFAST -> 4101
            MealType.LUNCH -> 4102
            MealType.DINNER -> 4103
            MealType.SNACK -> 4104
        }
        return if (isTest) base + 100 else base
    }
}
