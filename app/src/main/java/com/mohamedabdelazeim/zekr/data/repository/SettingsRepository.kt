package com.mohamedabdelazeim.zekr.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val gson = Gson()
    
    companion object {
        private val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        private val AZKAR_INTERVAL = intPreferencesKey("azkar_interval")
        private val MANUAL_OFFSETS = stringPreferencesKey("manual_offsets")
        private val ADHAN_SOUNDS = stringPreferencesKey("adhan_sounds")
        private val SAVED_LOCATION = stringPreferencesKey("saved_location")
        
        // الإعدادات الجديدة
        private val SILENT_ADHAN = stringPreferencesKey("silent_adhan")
        private val PRAYER_REMINDER_ENABLED = booleanPreferencesKey("prayer_reminder_enabled")
        private val AZKAR_REMINDER_ENABLED = booleanPreferencesKey("azkar_reminder_enabled")
        private val AZKAR_AUDIO_ENABLED = booleanPreferencesKey("azkar_audio_enabled")
        private val AZKAR_AUDIO_URI = stringPreferencesKey("azkar_audio_uri")
    }
    
    // ========== طريقة الحساب ==========
    suspend fun setCalculationMethod(method: String) {
        context.dataStore.edit { preferences ->
            preferences[CALCULATION_METHOD] = method
        }
    }
    
    fun getCalculationMethodFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CALCULATION_METHOD] ?: "UmmAlQura"
        }
    }
    
    suspend fun getCalculationMethod(): String {
        return getCalculationMethodFlow().first()
    }
    
    // ========== فترة الأذكار ==========
    suspend fun setAzkarInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[AZKAR_INTERVAL] = interval
        }
    }
    
    fun getAzkarIntervalFlow(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[AZKAR_INTERVAL] ?: 15
        }
    }
    
    suspend fun getAzkarInterval(): Int {
        return getAzkarIntervalFlow().first()
    }
    
    // ========== التعديل اليدوي ==========
    suspend fun setManualOffset(prayer: String, offset: Int) {
        val currentOffsets = getManualOffsets().toMutableMap()
        currentOffsets[prayer] = offset
        val json = gson.toJson(currentOffsets)
        context.dataStore.edit { preferences ->
            preferences[MANUAL_OFFSETS] = json
        }
    }
    
    fun getManualOffsetsFlow(): Flow<Map<String, Int>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[MANUAL_OFFSETS] ?: "{}"
            try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    suspend fun getManualOffsets(): Map<String, Int> {
        return getManualOffsetsFlow().first()
    }
    
    // ========== أصوات الأذان ==========
    suspend fun setAdhanSound(prayer: String, uri: String) {
        val currentSounds = getAdhanSounds().toMutableMap()
        currentSounds[prayer] = uri
        val json = gson.toJson(currentSounds)
        context.dataStore.edit { preferences ->
            preferences[ADHAN_SOUNDS] = json
        }
    }
    
    fun getAdhanSoundsFlow(): Flow<Map<String, String>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[ADHAN_SOUNDS] ?: "{}"
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    suspend fun getAdhanSounds(): Map<String, String> {
        return getAdhanSoundsFlow().first()
    }
    
    // ========== الأذان الصامت ==========
    suspend fun setSilentAdhan(prayer: String, silent: Boolean) {
        val currentSilent = getSilentAdhan().toMutableMap()
        currentSilent[prayer] = silent
        val json = gson.toJson(currentSilent)
        context.dataStore.edit { preferences ->
            preferences[SILENT_ADHAN] = json
        }
    }
    
    fun getSilentAdhanFlow(): Flow<Map<String, Boolean>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[SILENT_ADHAN] ?: "{}"
            try {
                val type = object : TypeToken<Map<String, Boolean>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    suspend fun getSilentAdhan(): Map<String, Boolean> {
        return getSilentAdhanFlow().first()
    }
    
    fun isAdhanSilent(prayer: String): Flow<Boolean> {
        return getSilentAdhanFlow().map { it[prayer] ?: false }
    }
    
    // ========== تذكير الصلاة ==========
    suspend fun setPrayerReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PRAYER_REMINDER_ENABLED] = enabled
        }
    }
    
    fun getPrayerReminderEnabledFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PRAYER_REMINDER_ENABLED] ?: true
        }
    }
    
    suspend fun isPrayerReminderEnabled(): Boolean {
        return getPrayerReminderEnabledFlow().first()
    }
    
    // ========== تذكير الأذكار ==========
    suspend fun setAzkarReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AZKAR_REMINDER_ENABLED] = enabled
        }
    }
    
    fun getAzkarReminderEnabledFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AZKAR_REMINDER_ENABLED] ?: true
        }
    }
    
    suspend fun isAzkarReminderEnabled(): Boolean {
        return getAzkarReminderEnabledFlow().first()
    }
    
    // ========== صوت الأذكار ==========
    suspend fun setAzkarAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AZKAR_AUDIO_ENABLED] = enabled
        }
    }
    
    fun getAzkarAudioEnabledFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AZKAR_AUDIO_ENABLED] ?: true
        }
    }
    
    suspend fun isAzkarAudioEnabled(): Boolean {
        return getAzkarAudioEnabledFlow().first()
    }
    
    // ========== صوت الأذكار المخصص ==========
    suspend fun setAzkarAudioUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[AZKAR_AUDIO_URI] = uri
        }
    }
    
    fun getAzkarAudioUriFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[AZKAR_AUDIO_URI]
        }
    }
    
    suspend fun getAzkarAudioUri(): String? {
        return getAzkarAudioUriFlow().first()
    }
    
    // ========== حفظ الموقع ==========
    suspend fun saveLocation(location: LocationEntity) {
        val json = gson.toJson(location)
        context.dataStore.edit { preferences ->
            preferences[SAVED_LOCATION] = json
        }
    }
    
    fun getSavedLocationFlow(): Flow<LocationEntity?> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[SAVED_LOCATION] ?: return@map null
            try {
                gson.fromJson(json, LocationEntity::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun getSavedLocation(): LocationEntity? {
        return getSavedLocationFlow().first()
    }
}

data class LocationEntity(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val timestamp: Long
)
