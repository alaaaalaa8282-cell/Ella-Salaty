package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mohamedabdelazeim.zekr.worker.DailyPrayerWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleDailyPrayerWorker(context)
            rescheduleAzkarAlarms(context)
        }
    }
    
    private fun scheduleDailyPrayerWorker(context: Context) {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyPrayerWorker>(
            1, TimeUnit.DAYS
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_prayer_update",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
    
    private fun rescheduleAzkarAlarms(context: Context) {
        // هنا هنعيد جدولة الأذكار بعد إعادة التشغيل
        // هيتنفذ بعد ما نعمل AzkarAlarmScheduler
    }
}
