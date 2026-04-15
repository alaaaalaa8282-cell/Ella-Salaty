package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.mohamedabdelazeim.zekr.service.AzkarPlaybackService
import com.mohamedabdelazeim.zekr.service.alarm.AzkarAlarmScheduler
import com.mohamedabdelazeim.zekr.worker.AzkarReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class AzkarAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository

    @Inject
    lateinit var azkarAlarmScheduler: AzkarAlarmScheduler

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        scope.launch {
            // التحقق من إعدادات صوت الأذكار
            val azkarAudioEnabled = settingsRepository.isAzkarAudioEnabled()

            if (azkarAudioEnabled) {
                // بدء خدمة تشغيل الذكر الصوتي
                val serviceIntent = Intent(context, AzkarPlaybackService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            // جدولة تذكير الأذكار المنبثقة
            scheduleAzkarReminder(context)

            // إعادة جدولة التنبيه التالي
            val interval = settingsRepository.getAzkarInterval()
            azkarAlarmScheduler.scheduleAzkarAlarm(interval)
        }
    }

    private fun scheduleAzkarReminder(context: Context) {
        scope.launch {
            val reminderEnabled = settingsRepository.isAzkarReminderEnabled()
            if (!reminderEnabled) return@launch

            val reminderWork = OneTimeWorkRequestBuilder<AzkarReminderWorker>()
                .setInitialDelay(1, TimeUnit.SECONDS)
                .addTag("azkar_reminder")
                .build()

            WorkManager.getInstance(context).enqueue(reminderWork)
        }
    }
}
