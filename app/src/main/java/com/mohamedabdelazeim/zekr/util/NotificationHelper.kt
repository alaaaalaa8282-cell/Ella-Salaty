package com.mohamedabdelazeim.zekr.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.MainActivity
import com.mohamedabdelazeim.zekr.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        // قنوات الإشعارات
        const val CHANNEL_PRAYER = "prayer_channel"
        const val CHANNEL_AZKAR = "azkar_channel"
        const val CHANNEL_REMINDER = "reminder_channel"
        const val CHANNEL_STICKY = "sticky_channel"
        
        // معرفات الإشعارات
        const val NOTIFICATION_PRAYER_ALARM = 1001
        const val NOTIFICATION_PRAYER_REMINDER = 1002
        const val NOTIFICATION_AZKAR_PLAYBACK = 2001
        const val NOTIFICATION_AZKAR_REMINDER = 2002
        const val NOTIFICATION_STICKY_TIMES = 3001
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // قناة الأذان - أهمية عالية
            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER,
                "الأذان والصلوات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة وتشغيل الأذان"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                setBypassDnd(true) // يتجاوز وضع عدم الإزعاج
            }
            
            // قناة الأذكار - أهمية منخفضة
            val azkarChannel = NotificationChannel(
                CHANNEL_AZKAR,
                "الأذكار المتكررة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تشغيل الأذكار في الخلفية"
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(false)
            }
            
            // قناة التذكير - أهمية متوسطة
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "تذكيرات العبادة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير بالصلاة والأذكار"
                enableVibration(true)
                setBypassDnd(false)
            }
            
            // قناة الإشعارات الدائمة - أهمية منخفضة جداً
            val stickyChannel = NotificationChannel(
                CHANNEL_STICKY,
                "مواقيت الصلاة",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "عرض مواقيت الصلاة في شريط الإشعارات"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(prayerChannel)
            notificationManager.createNotificationChannel(azkarChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(stickyChannel)
        }
    }
    
    // إشعار الأذان
    fun showPrayerAlarmNotification(prayerName: String, prayerTime: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_PRAYER)
            .setContentTitle("حان الآن موعد صلاة $prayerName")
            .setContentText("قم إلى الصلاة - $prayerTime")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
    }
    
    // إشعار تذكير بالصلاة
    fun showPrayerReminderNotification(prayerName: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val reminderMessages = listOf(
            "هل صليت $prayerName؟",
            "لا تنس صلاة $prayerName",
            "ذكر نفسك بصلاة $prayerName",
            "حافظ على صلاة $prayerName"
        )
        
        return NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setContentTitle("تذكير بالصلاة")
            .setContentText(reminderMessages.random())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }
    
    // إشعار تذكير بالذكر
    fun showAzkarReminderNotification(zekrText: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setContentTitle("تذكير بالذكر")
            .setContentText(zekrText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(zekrText))
            .build()
    }
    
    // إشعار دائم بمواقيت الصلاة
    fun showStickyPrayerTimesNotification(
        nextPrayer: String,
        nextPrayerTime: String,
        allTimes: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_STICKY)
            .setContentTitle("الصلاة القادمة: $nextPrayer")
            .setContentText("في $nextPrayerTime")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(allTimes))
            .setSilent(true)
            .build()
    }
    
    // إشعار تشغيل الأذكار
    fun showAzkarPlaybackNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_AZKAR)
            .setContentTitle("جاري تشغيل الأذكار")
            .setContentText("يتم تشغيل الأذكار في الخلفية")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    // عرض الإشعار
    fun showNotification(notificationId: Int, notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }
    
    // إلغاء إشعار
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    // إلغاء كل الإشعارات
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
