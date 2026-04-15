package com.mohamedabdelazeim.zekr.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesDao
import com.mohamedabdelazeim.zekr.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class StickyNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prayerTimesDao: PrayerTimesDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val prayerTimes = prayerTimesDao.getLatestPrayerTimes()
                
                if (prayerTimes != null) {
                    val prayers = listOf(
                        "الفجر" to prayerTimes.fajr,
                        "الظهر" to prayerTimes.dhuhr,
                        "العصر" to prayerTimes.asr,
                        "المغرب" to prayerTimes.maghrib,
                        "العشاء" to prayerTimes.isha
                    )
                    
                    val now = Date()
                    val currentTime = dateFormat.format(now)
                    
                    val nextPrayer = prayers.firstOrNull { it.second > currentTime }
                        ?: prayers.firstOrNull()?.copy(first = "${prayers.first().first} (غداً)")
                    
                    val allTimes = prayers.joinToString("\n") { "${it.first}: ${it.second}" }
                    
                    if (nextPrayer != null) {
                        val notification = notificationHelper.showStickyPrayerTimesNotification(
                            nextPrayer.first,
                            nextPrayer.second,
                            allTimes
                        )
                        notificationHelper.showNotification(
                            NotificationHelper.NOTIFICATION_STICKY_TIMES,
                            notification
                        )
                    }
                }
                
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}
