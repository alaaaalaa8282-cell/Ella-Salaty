package com.mohamedabdelazeim.zekr.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
import com.mohamedabdelazeim.zekr.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AzkarReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

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

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // التحقق من إعدادات تذكير الأذكار
                if (!settingsRepository.isAzkarReminderEnabled()) {
                    return@withContext Result.success()
                }
                
                // اختيار ذكر عشوائي
                val randomZekr = azkarList.random()
                
                // إرسال إشعار التذكير
                val notification = notificationHelper.showAzkarReminderNotification(randomZekr)
                notificationHelper.showNotification(
                    NotificationHelper.NOTIFICATION_AZKAR_REMINDER,
                    notification
                )
                
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}
