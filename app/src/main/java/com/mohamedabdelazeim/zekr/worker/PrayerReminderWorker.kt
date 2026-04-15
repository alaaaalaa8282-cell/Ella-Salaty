package com.mohamedabdelazeim.zekr.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesDao
import com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
import com.mohamedabdelazeim.zekr.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class PrayerReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prayerTimesDao: PrayerTimesDao,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val prayerName = inputData.getString("prayer_name") ?: return@withContext Result.failure()
                
                // التحقق من إعدادات التذكير
                if (!settingsRepository.isPrayerReminderEnabled()) {
                    return@withContext Result.success()
                }
                
                // إرسال إشعار التذكير
                val notification = notificationHelper.showPrayerReminderNotification(prayerName)
                notificationHelper.showNotification(
                    NotificationHelper.NOTIFICATION_PRAYER_REMINDER + prayerName.hashCode(),
                    notification
                )
                
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}
