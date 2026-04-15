package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mohamedabdelazeim.zekr.service.alarm.AzkarAlarmScheduler
import com.mohamedabdelazeim.zekr.worker.DailyPrayerWorker
import com.mohamedabdelazeim.zekr.worker.StickyNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository

    @Inject
    lateinit var azkarAlarmScheduler: AzkarAlarmScheduler

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                scheduleDailyPrayerWorker(context)
                rescheduleAzkarAlarms()
                scheduleStickyNotificationWorker(context)
            }
        }
    }

    private fun scheduleDailyPrayerWorker(context: Context) {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyPrayerWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_prayer_update",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    private suspend fun rescheduleAzkarAlarms() {
        val azkarAudioEnabled = settingsRepository.isAzkarAudioEnabled()
        val azkarReminderEnabled = settingsRepository.isAzkarReminderEnabled()

        if (azkarAudioEnabled || azkarReminderEnabled) {
            val interval = settingsRepository.getAzkarInterval()
            azkarAlarmScheduler.scheduleAzkarAlarm(interval)
        }
    }

    private fun scheduleStickyNotificationWorker(context: Context) {
        val stickyWorkRequest = PeriodicWorkRequestBuilder<StickyNotificationWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sticky_notification",
            ExistingPeriodicWorkPolicy.UPDATE,
            stickyWorkRequest
        )
    }
}
