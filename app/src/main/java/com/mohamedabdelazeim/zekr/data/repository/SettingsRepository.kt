package com.mohamedabdelazeim.zekr.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mohamedabdelazeim.zekr.data.local.LocationEntity
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
    }
    
    // طريقة الحساب
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
    
    // فترة الأذكار
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
    
    // التعديل اليدوي للمواقيت
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
    
    // أصوات الأذان
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
    
    // حفظ الموقع
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
