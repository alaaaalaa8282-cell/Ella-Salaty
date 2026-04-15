package com.mohamedabdelazeim.zekr.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
    private var mediaPlayer: MediaPlayer? = null
    
    // قائمة الأذكار مع ملفاتها الصوتية
    private val azkarWithAudio = listOf(
        AzkarAudio("سبحان الله", R.raw.subhanallah),
        AzkarAudio("الحمد لله", R.raw.alhamdulillah),
        AzkarAudio("الله أكبر", R.raw.allahuakbar),
        AzkarAudio("لا إله إلا الله", R.raw.lailahaillallah),
        AzkarAudio("سبحان الله وبحمده سبحان الله العظيم", R.raw.subhanallah_wabihamdihi),
        AzkarAudio("لا حول ولا قوة إلا بالله", R.raw.lahawla),
        AzkarAudio("أستغفر الله العظيم وأتوب إليه", R.raw.astaghfirullah),
        AzkarAudio("اللهم صل على محمد وعلى آل محمد", R.raw.salawat)
    )
    
    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "azkar_playback_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopPlayback()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        
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
    
    private suspend fun playRandomAzkar() {
        if (azkarWithAudio.isEmpty()) return
        
        val random = Random()
        val selectedAzkar = azkarWithAudio[random.nextInt(azkarWithAudio.size)]
        
        // استخدام AudioFocusManager لتشغيل الصوت
        val success = audioFocusManager.requestAudioFocusAndPlay(selectedAzkar.audioResId) {
            // عند انتهاء التشغيل، نوقف الخدمة
            stopSelf()
        }
        
        if (!success) {
            // لو فشل التشغيل (مثلاً فيه مكالمة شغالة)، نوقف الخدمة
            stopSelf()
        }
    }
    
    private fun stopPlayback() {
        audioFocusManager.stopPlayback()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopPlayback()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    data class AzkarAudio(
        val text: String,
        val audioResId: Int
    )
}
