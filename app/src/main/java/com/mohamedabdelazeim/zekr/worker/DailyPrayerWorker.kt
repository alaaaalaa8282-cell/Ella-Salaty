package com.mohamedabdelazeim.zekr.worker

import android.content.Context
import android.location.Geocoder
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesDao
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesEntity
import com.mohamedabdelazeim.zekr.data.repository.LocationRepository
import com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
import com.mohamedabdelazeim.zekr.service.alarm.PrayerAlarmScheduler
import com.mohamedabdelazeim.zekr.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyPrayerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesDao: PrayerTimesDao,
    private val prayerAlarmScheduler: PrayerAlarmScheduler,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val gregorianDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val location = locationRepository.getCurrentLocation()
                val locationName = getLocationName(location.latitude, location.longitude)

                locationRepository.saveLocation(location, locationName)

                val prayers = calculatePrayerTimes(location.latitude, location.longitude)
                val hijriDate = getHijriDate()
                val gregorianDate = gregorianDateFormat.format(Date())

                savePrayerTimesToDatabase(prayers, hijriDate, gregorianDate)

                // إلغاء التنبيهات القديمة وجدولة الجديدة
                prayerAlarmScheduler.cancelAllPrayerAlarms()
                
                prayers.forEach { prayer ->
                    prayerAlarmScheduler.schedulePrayerAlarm(
                        PrayerAlarmScheduler.PrayerTimeData(prayer.name, prayer.time)
                    )
                }

                // تحديث الإشعار الدائم بمواقيت الصلاة
                updateStickyNotification(prayers)

                // جدولة تذكيرات "هل صليت؟" للصلوات
                schedulePrayerReminders(prayers)

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }

    private suspend fun calculatePrayerTimes(latitude: Double, longitude: Double): List<PrayerTimeData> {
        val coordinates = Coordinates(latitude, longitude)
        val date = DateComponents.from(Date())
        val calculationMethod = settingsRepository.getCalculationMethod()

        val params = CalculationMethod.valueOf(calculationMethod).params
        params.madhab = Madhab.SHAFI

        val prayerTimes = PrayerTimes(coordinates, date, params)

        return listOf(
            PrayerTimeData("الفجر", dateFormat.format(prayerTimes.fajr)),
            PrayerTimeData("الشروق", dateFormat.format(prayerTimes.sunrise)),
            PrayerTimeData("الظهر", dateFormat.format(prayerTimes.dhuhr)),
            PrayerTimeData("العصر", dateFormat.format(prayerTimes.asr)),
            PrayerTimeData("المغرب", dateFormat.format(prayerTimes.maghrib)),
            PrayerTimeData("العشاء", dateFormat.format(prayerTimes.isha))
        )
    }

    private fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(applicationContext, Locale("ar"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0]?.locality ?: addresses[0]?.adminArea ?: "موقع غير معروف"
            } else {
                "موقع غير معروف"
            }
        } catch (e: Exception) {
            "موقع غير معروف"
        }
    }

    private fun getHijriDate(): String {
        return try {
            val calendar = Calendar.getInstance()
            val hijriCalendar = com.github.msarhan.ummalqura.calendar.UmmalquraCalendar()
            hijriCalendar.time = calendar.time

            val day = hijriCalendar.get(Calendar.DAY_OF_MONTH)
            val month = hijriCalendar.get(Calendar.MONTH) + 1
            val year = hijriCalendar.get(Calendar.YEAR)

            val monthNames = arrayOf(
                "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
                "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
                "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
            )

            "$day ${monthNames[month - 1]} $year"
        } catch (e: Exception) {
            "التاريخ الهجري غير متاح"
        }
    }

    private suspend fun savePrayerTimesToDatabase(
        prayers: List<PrayerTimeData>,
        hijriDate: String,
        gregorianDate: String
    ) {
        val entity = PrayerTimesEntity(
            date = System.currentTimeMillis(),
            fajr = prayers[0].time,
            sunrise = prayers[1].time,
            dhuhr = prayers[2].time,
            asr = prayers[3].time,
            maghrib = prayers[4].time,
            isha = prayers[5].time,
            hijriDate = hijriDate,
            gregorianDate = gregorianDate
        )
        prayerTimesDao.insertPrayerTimes(entity)
    }

    private fun updateStickyNotification(prayers: List<PrayerTimeData>) {
        val now = Date()
        val currentTime = dateFormat.format(now)
        
        val prayerList = listOf(
            "الفجر" to prayers[0].time,
            "الظهر" to prayers[1].time,
            "العصر" to prayers[2].time,
            "المغرب" to prayers[3].time,
            "العشاء" to prayers[4].time
        )
        
        val nextPrayer = prayerList.firstOrNull { it.second > currentTime }
            ?: prayerList.firstOrNull()?.copy(first = "${prayerList.first().first} (غداً)")
        
        val allTimes = prayerList.joinToString("\n") { "${it.first}: ${it.second}" }
        
        if (nextPrayer != null) {
            val notification = notificationHelper.showStickyPrayerTimesNotification(
                nextPrayer.first,
                nextPrayer.second,
                allTimes
            )
            notificationHelper.showNotification(NotificationHelper.NOTIFICATION_STICKY_TIMES, notification)
        }
    }

    private fun schedulePrayerReminders(prayers: List<PrayerTimeData>) {
        val prayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
        
        prayerNames.forEach { prayerName ->
            val reminderWork = OneTimeWorkRequestBuilder<PrayerReminderWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(
                    workDataOf("prayer_name" to prayerName)
                )
                .addTag("prayer_reminder_$prayerName")
                .build()
            
            WorkManager.getInstance(applicationContext).enqueue(reminderWork)
        }
    }

    data class PrayerTimeData(
        val name: String,
        val time: String
    )
}
