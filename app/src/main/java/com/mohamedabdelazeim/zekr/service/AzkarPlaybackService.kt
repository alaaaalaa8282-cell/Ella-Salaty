package com.mohamedabdelazeim.zekr.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.MainActivity
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.service.audio.AudioFocusManager
import com.mohamedabdelazeim.zekr.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AzkarPlaybackService : Service(), TextToSpeech.OnInitListener {

    @Inject
    lateinit var audioFocusManager: AudioFocusManager

    @Inject
    lateinit var settingsRepository: com.mohamedabdelazeim.zekr.data.repository.SettingsRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val azkarList = listOf(
        "سبحان الله",
        "الحمد لله",
        "الله أكبر",
        "لا إله إلا الله",
        "سبحان الله وبحمده سبحان الله العظيم",
        "لا حول ولا قوة إلا بالله",
        "أستغفر الله العظيم وأتوب إليه",
        "اللهم صل على محمد وعلى آل محمد",
        "سبحان الله والحمد لله ولا إله إلا الله والله أكبر",
        "اللهم إني أسألك الجنة وأعوذ بك من النار"
    )

    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "azkar_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.apply {
                val result = setLanguage(Locale("ar"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    setLanguage(Locale.US)
                }
                setPitch(1.0f)
                setSpeechRate(0.9f)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val voice = voices.firstOrNull { 
                        it.locale.language == "ar" && it.quality == TextToSpeech.Voice.QUALITY_HIGH 
                    }
                    voice?.let { setVoice(it) }
                }
                
                isTtsInitialized = true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopPlayback()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = notificationHelper.showAzkarPlaybackNotification()
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            // التحقق من إعدادات الصوت مرة أخرى
            val azkarAudioEnabled = settingsRepository.isAzkarAudioEnabled()
            
            if (!azkarAudioEnabled) {
                stopSelf()
                return@launch
            }

            // جلب الصوت المخصص من الإعدادات
            val selectedAudioUri = settingsRepository.getAzkarAudioUri()

            if (selectedAudioUri != null) {
                // تشغيل الصوت المختار من الهاتف
                playAudioFromUri(selectedAudioUri)
            } else {
                // استخدام TextToSpeech كبديل
                var attempts = 0
                while (!isTtsInitialized && attempts < 50) {
                    delay(100)
                    attempts++
                }
                
                if (isTtsInitialized) {
                    playRandomAzkarWithTTS()
                } else {
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun playAudioFromUri(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            
            val success = audioFocusManager.requestAudioFocusAndPlayUri(uri) {
                stopSelf()
            }
            
            if (!success) {
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
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

    private fun playRandomAzkarWithTTS() {
        if (azkarList.isEmpty() || textToSpeech == null) {
            stopSelf()
            return
        }

        val random = Random()
        val selectedAzkar = azkarList[random.nextInt(azkarList.size)]

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                stopSelf()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                stopSelf()
            }
        })

        val utteranceId = UUID.randomUUID().toString()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0f)
            }
            textToSpeech?.speak(selectedAzkar, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0")
            }
            @Suppress("DEPRECATION")
            textToSpeech?.speak(selectedAzkar, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    private fun stopPlayback() {
        audioFocusManager.stopPlayback()
        textToSpeech?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopPlayback()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
