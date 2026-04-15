package com.mohamedabdelazeim.zekr.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
import com.mohamedabdelazeim.zekr.service.alarm.AzkarAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val azkarAlarmScheduler: AzkarAlarmScheduler
) : ViewModel() {

    val calculationMethod: StateFlow<String> = settingsRepository.getCalculationMethodFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "UmmAlQura"
        )

    val azkarInterval: StateFlow<Int> = settingsRepository.getAzkarIntervalFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 15
        )

    val manualOffsets: StateFlow<Map<String, Int>> = settingsRepository.getManualOffsetsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val selectedAdhanSounds: StateFlow<Map<String, String>> = settingsRepository.getAdhanSoundsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // الإعدادات الجديدة
    val silentAdhan: StateFlow<Map<String, Boolean>> = settingsRepository.getSilentAdhanFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val prayerReminderEnabled: StateFlow<Boolean> = settingsRepository.getPrayerReminderEnabledFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val azkarReminderEnabled: StateFlow<Boolean> = settingsRepository.getAzkarReminderEnabledFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val azkarAudioEnabled: StateFlow<Boolean> = settingsRepository.getAzkarAudioEnabledFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setCalculationMethod(method: String) {
        viewModelScope.launch {
            settingsRepository.setCalculationMethod(method)
        }
    }

    fun setAzkarInterval(interval: Int) {
        viewModelScope.launch {
            settingsRepository.setAzkarInterval(interval)
            // إعادة جدولة الأذكار بالفترة الجديدة
            azkarAlarmScheduler.cancelAzkarAlarm()
            azkarAlarmScheduler.scheduleAzkarAlarm(interval)
        }
    }

    fun incrementOffset(prayer: String) {
        viewModelScope.launch {
            val current = manualOffsets.value[prayer] ?: 0
            if (current < 30) {
                settingsRepository.setManualOffset(prayer, current + 1)
            }
        }
    }

    fun decrementOffset(prayer: String) {
        viewModelScope.launch {
            val current = manualOffsets.value[prayer] ?: 0
            if (current > -30) {
                settingsRepository.setManualOffset(prayer, current - 1)
            }
        }
    }

    fun setAdhanSound(prayer: String, uri: Uri) {
        viewModelScope.launch {
            settingsRepository.setAdhanSound(prayer, uri.toString())
        }
    }

    fun toggleSilentAdhan(prayer: String) {
        viewModelScope.launch {
            val current = silentAdhan.value[prayer] ?: false
            settingsRepository.setSilentAdhan(prayer, !current)
        }
    }

    fun setPrayerReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerReminderEnabled(enabled)
        }
    }

    fun setAzkarReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAzkarReminderEnabled(enabled)
        }
    }

    fun setAzkarAudioEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAzkarAudioEnabled(enabled)
        }
    }
}
