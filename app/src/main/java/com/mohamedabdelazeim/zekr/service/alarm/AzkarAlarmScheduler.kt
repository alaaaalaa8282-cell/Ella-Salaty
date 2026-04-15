package com.mohamedabdelazeim.zekr.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mohamedabdelazeim.zekr.receiver.AzkarAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzkarAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        private const val AZKAR_ALARM_REQUEST_CODE = 2001
    }
    
    fun scheduleAzkarAlarm(intervalMinutes: Int) {
        cancelAzkarAlarm()
        
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, intervalMinutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val intent = Intent(context, AzkarAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AZKAR_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            else -> {
                @Suppress("DEPRECATION")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
    
    fun scheduleRepeatingAzkarAlarm(intervalMinutes: Int) {
        cancelAzkarAlarm()
        
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, intervalMinutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val intent = Intent(context, AzkarAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AZKAR_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val intervalMillis = intervalMinutes * 60 * 1000L
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    intervalMillis,
                    pendingIntent
                )
            }
            else -> {
                @Suppress("DEPRECATION")
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    intervalMillis,
                    pendingIntent
                )
            }
        }
    }
    
    fun cancelAzkarAlarm() {
        val intent = Intent(context, AzkarAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AZKAR_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    fun isAlarmScheduled(): Boolean {
        val intent = Intent(context, AzkarAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AZKAR_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
}
