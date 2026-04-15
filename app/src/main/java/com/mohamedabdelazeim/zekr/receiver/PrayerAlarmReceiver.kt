package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.ui.alarm.AlarmFullScreenActivity
import com.mohamedabdelazeim.zekr.util.NotificationHelper
import com.mohamedabdelazeim.zekr.worker.PrayerReminderWorker
import androidx.work.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val prayerTime = intent.getStringExtra("prayer_time") ?: ""

        scope.launch {
            // التحقق من إعدادات الأذان الصامت
            val silentAdhan = settingsRepository.getSilentAdhan()
            val isSilent = silentAdhan[prayerName] ?: false

            if (!isSilent) {
                // تشغيل شاشة الأذان فل سكرين (مع صوت)
                val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
                    putExtra("prayer_name", prayerName)
                    putExtra("prayer_time", prayerTime)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                // الحصول على صوت الأذان المخصص
                val adhanSounds = settingsRepository.getAdhanSounds()
                val adhanUri = adhanSounds[prayerName]

                if (adhanUri != null) {
                    fullScreenIntent.putExtra("audio_uri", adhanUri)
                } else {
                    // الصوت الافتراضي
                    val defaultAdhanRes = when (prayerName) {
                        "الفجر" -> R.raw.adhan_fajr
                        else -> R.raw.adhan_normal
                    }
                    fullScreenIntent.putExtra("audio_res_id", defaultAdhanRes)
                }

                context.startActivity(fullScreenIntent)
            }

            // إرسال إشعار الأذان (حتى لو كان صامت)
            val notification = notificationHelper.showPrayerAlarmNotification(prayerName, prayerTime)
            notificationHelper.showNotification(
                when (prayerName) {
                    "الفجر" -> 1001
                    "الظهر" -> 1002
                    "العصر" -> 1003
                    "المغرب" -> 1004
                    "العشاء" -> 1005
                    else -> 1000
                },
                notification
            )

            // جدولة تذكير "هل صليت؟" بعد 30 دقيقة
            schedulePrayerReminder(context, prayerName)
        }
    }

    private fun schedulePrayerReminder(context: Context, prayerName: String) {
        scope.launch {
            val reminderEnabled = settingsRepository.isPrayerReminderEnabled()
            if (!reminderEnabled) return@launch

            val reminderWork = OneTimeWorkRequestBuilder<PrayerReminderWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(
                    workDataOf("prayer_name" to prayerName)
                )
                .addTag("prayer_reminder_$prayerName")
                .build()

            WorkManager.getInstance(context).enqueue(reminderWork)
        }
    }
}
