package com.mohamedabdelazeim.zekr.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mohamedabdelazeim.zekr.receiver.PrayerAlarmReceiver
import com.mohamedabdelazeim.zekr.ui.prayer.PrayerItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    fun scheduleAllPrayerAlarms(prayers: List<PrayerTimeData>) {
        prayers.forEach { prayer ->
            schedulePrayerAlarm(prayer)
        }
    }
    
    fun schedulePrayerAlarm(prayer: PrayerTimeData) {
        val prayerTime = parseTimeToCalendar(prayer.time)
        val now = Calendar.getInstance()
        
        // لو الوقت عدى النهاردة، نشغله بكرا
        if (prayerTime.before(now)) {
            prayerTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayer.name)
            putExtra("prayer_time", prayer.time)
        }
        
        val requestCode = getRequestCodeForPrayer(prayer.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayerTime.timeInMillis,
                        pendingIntent
                    )
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    prayerTime.timeInMillis,
                    pendingIntent
                )
            }
            else -> {
                @Suppress("DEPRECATION")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    prayerTime.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
    
    fun cancelPrayerAlarm(prayerName: String) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val requestCode = getRequestCodeForPrayer(prayerName)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    fun cancelAllPrayerAlarms() {
        listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء").forEach { prayerName ->
            cancelPrayerAlarm(prayerName)
        }
    }
    
    private fun parseTimeToCalendar(timeString: String): Calendar {
        return Calendar.getInstance().apply {
            val parsedDate = dateFormat.parse(timeString)
            if (parsedDate != null) {
                time = parsedDate
            }
        }
    }
    
    private fun getRequestCodeForPrayer(prayerName: String): Int {
        return when (prayerName) {
            "الفجر" -> 1001
            "الظهر" -> 1002
            "العصر" -> 1003
            "المغرب" -> 1004
            "العشاء" -> 1005
            else -> 1000
        }
    }
    
    data class PrayerTimeData(
        val name: String,
        val time: String
    )
}
