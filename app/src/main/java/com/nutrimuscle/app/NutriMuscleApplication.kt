package com.nutrimuscle.app

import android.app.Application
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.notifications.MealReminderNotificationHelper
import com.nutrimuscle.app.notifications.MealReminderScheduler
import com.nutrimuscle.app.notifications.ReminderPreferencesDataSource
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NutriMuscleApplication : Application() {

    @Inject
    lateinit var repository: NutriRepository

    @Inject
    lateinit var notificationHelper: MealReminderNotificationHelper

    @Inject
    lateinit var reminderPrefs: ReminderPreferencesDataSource

    @Inject
    lateinit var reminderScheduler: MealReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        appScope.launch {
            repository.initializeDatabase()
            val settings = reminderPrefs.settingsFlow.first()
            if (settings.masterEnabled) {
                reminderScheduler.rescheduleAll(settings)
            }
        }
    }
}
