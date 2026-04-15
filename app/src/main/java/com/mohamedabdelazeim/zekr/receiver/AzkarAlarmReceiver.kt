package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mohamedabdelazeim.zekr.service.AzkarPlaybackService
import com.mohamedabdelazeim.zekr.service.alarm.AzkarAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AzkarAlarmReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
    
    @Inject
    lateinit var azkarAlarmScheduler: AzkarAlarmScheduler
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        // بدء خدمة تشغيل الذكر
        val serviceIntent = Intent(context, AzkarPlaybackService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        // إعادة جدولة التنبيه التالي
        scope.launch {
            val interval = settingsRepository.getAzkarInterval()
            azkarAlarmScheduler.scheduleAzkarAlarm(interval)
        }
    }
}
