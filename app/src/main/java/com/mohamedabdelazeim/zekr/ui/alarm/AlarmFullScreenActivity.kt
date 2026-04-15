package com.mohamedabdelazeim.zekr.ui.alarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.service.audio.AudioFocusManager
import com.mohamedabdelazeim.zekr.ui.theme.DarkGreen
import com.mohamedabdelazeim.zekr.ui.theme.Gold
import com.mohamedabdelazeim.zekr.ui.theme.GoldGradient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmFullScreenActivity : ComponentActivity() {

    @Inject
    lateinit var audioFocusManager: AudioFocusManager

    private var prayerName: String = ""
    private var prayerTime: String = ""
    private var audioUri: Uri? = null
    private var audioResId: Int = 0
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var closeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // جعل الشاشة تعمل حتى لو الجهاز مقفول
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // منع الشاشة من القفل
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // قراءة البيانات من الـ Intent
        prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        prayerTime = intent.getStringExtra("prayer_time") ?: ""
        audioResId = intent.getIntExtra("audio_res_id", 0)
        val audioUriString = intent.getStringExtra("audio_uri")
        if (!audioUriString.isNullOrEmpty()) {
            audioUri = Uri.parse(audioUriString)
        }

        setContent {
            AlarmFullScreenContent(
                prayerName = prayerName,
                prayerTime = prayerTime,
                onStop = { 
                    closeJob?.cancel()
                    finish() 
                },
                onSnooze = {
                    closeJob?.cancel()
                    finish()
                }
            )
        }

        // تشغيل صوت الأذان
        playAdhan()
    }

    private fun playAdhan() {
        if (audioResId != 0) {
            audioFocusManager.requestAudioFocusAndPlay(audioResId) {
                // إغلاق تلقائي بعد انتهاء الأذان بـ 3 ثواني
                closeJob = scope.launch {
                    delay(3000)
                    finish()
                }
            }
        } else if (audioUri != null) {
            audioFocusManager.requestAudioFocusAndPlayUri(audioUri!!) {
                // إغلاق تلقائي بعد انتهاء الأذان بـ 3 ثواني
                closeJob = scope.launch {
                    delay(3000)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeJob?.cancel()
        scope.cancel()
        audioFocusManager.stopPlayback()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun AlarmFullScreenContent(
    prayerName: String,
    prayerTime: String,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ===== خلفية صورة الوالد =====
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "رحمه الله",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // طبقة داكنة لتحسين وضوح النص
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // المحتوى الرئيسي
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // مساحة فارغة من فوق
            Spacer(modifier = Modifier.height(20.dp))
            
            // المحتوى الأوسط (الأذان)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // أيقونة الصلاة
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(60.dp))
                        .background(
                            Brush.radialGradient(
                                colors = GoldGradient,
                                radius = 120f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // اسم الصلاة
                Text(
                    text = prayerName,
                    color = Gold,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // وقت الصلاة
                Text(
                    text = prayerTime,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "حان الآن"
                Text(
                    text = "حان الآن",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            // الجزء السفلي (الدعاء + الأزرار)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ===== دعاء للوالد =====
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "اللهم اغفر لمحمد عبد العظيم الطويل وارحمه",
                            color = Gold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "وعافه واعف عنه وأكرم نزله ووسع مدخله",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "واغسله بالماء والثلج والبرد ونقه من الذنوب والخطايا",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اللهم اجعل قبره روضة من رياض الجنة",
                            color = Gold.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // أزرار التحكم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // زر التأجيل
                    Button(
                        onClick = onSnooze,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأجيل 5 دقائق", fontSize = 16.sp)
                    }

                    // زر الإيقاف
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إيقاف", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
