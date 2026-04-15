package com.mohamedabdelazeim.zekr.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.MainActivity
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.service.audio.AudioFocusManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AzkarPlaybackService : Service() {
    
    @Inject
    lateinit var audioFocusManager: AudioFocusManager
    
    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val azkarList = mutableListOf<String>()
    private var currentAzkarIndex = 0
    
    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "azkar_playback_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadAzkar()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            playRandomAzkar()
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل الأذكار",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تشغيل الأذكار في الخلفية"
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AzkarPlaybackService::class.java).apply {
                action = "STOP"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("جاري تشغيل الأذكار")
            .setContentText("يتم تشغيل الأذكار في الخلفية")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(0, "إيقاف", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun loadAzkar() {
        // تحميل الأذكار من الملف أو استخدام القائمة الافتراضية
        azkarList.addAll(listOf(
            "سبحان الله",
            "الحمد لله",
            "الله أكبر",
            "لا إله إلا الله",
            "سبحان الله وبحمده سبحان الله العظيم",
            "لا حول ولا قوة إلا بالله",
            "أستغفر الله العظيم وأتوب إليه",
            "اللهم صل على محمد وعلى آل محمد"
        ))
    }
    
    private suspend fun playRandomAzkar() {
        if (azkarList.isEmpty()) return
        
        val random = Random()
        currentAzkarIndex = random.nextInt(azkarList.size)
        val azkar = azkarList[currentAzkarIndex]
        
        // تشغيل الذكر باستخدام TextToSpeech
        speakAzkar(azkar)
        
        // انتظار حتى ينتهي الصوت
        delay(5000)
        
        // إيقاف الخدمة بعد التشغيل
        stopSelf()
    }
    
    private fun speakAzkar(text: String) {
        // هنا ممكن استخدام TextToSpeech أو MediaPlayer لتشغيل ملف صوتي
        // للتطبيق الحالي، سنستخدم إشعار فقط
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioFocusManager.release()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
