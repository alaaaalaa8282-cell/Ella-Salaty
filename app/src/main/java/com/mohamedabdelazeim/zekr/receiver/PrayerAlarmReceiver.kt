package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.ui.alarm.AlarmFullScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
    
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val prayerTime = intent.getStringExtra("prayer_time") ?: ""
        
        // تشغيل شاشة الأذان فل سكرين
        val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            putExtra("prayer_name", prayerName)
            putExtra("prayer_time", prayerTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // الحصول على صوت الأذان المخصص من الإعدادات
        val adhanUri = getAdhanSoundForPrayer(prayerName)
        if (adhanUri != null) {
            fullScreenIntent.putExtra("audio_uri", adhanUri.toString())
        } else {
            // الصوت الافتراضي
            val defaultAdhanRes = when (prayerName) {
                "الفجر" -> R.raw.adhan_fajr
                else -> R.raw.adhan_normal
            }
            fullScreenIntent.putExtra("audio_res_id", defaultAdhanRes)
        }
        
        context.startActivity(fullScreenIntent)
        
        // إرسال إشعار
        showNotification(context, prayerName)
    }
    
    private fun getAdhanSoundForPrayer(prayerName: String): Uri? {
        // هنا هنجيب الصوت المحفوظ من SettingsRepository
        // هيتنفذ بعد ما نضبط الـ Repository
        return null
    }
    
    private fun showNotification(context: Context, prayerName: String) {
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        
        val notification = NotificationCompat.Builder(context, "prayer_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("حان الآن موعد صلاة $prayerName")
            .setContentText("قم إلى الصلاة")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        
        val notificationId = when (prayerName) {
            "الفجر" -> 1001
            "الظهر" -> 1002
            "العصر" -> 1003
            "المغرب" -> 1004
            "العشاء" -> 1005
            else -> 1000
        }
        
        notificationManager.notify(notificationId, notification)
    }
}
