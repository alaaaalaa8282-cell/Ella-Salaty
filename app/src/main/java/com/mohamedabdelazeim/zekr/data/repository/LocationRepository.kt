package com.mohamedabdelazeim.zekr.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.mohamedabdelazeim.zekr.data.local.LocationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationManager = 
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _savedLocation = MutableStateFlow<LocationEntity?>(null)
    val savedLocation: StateFlow<LocationEntity?> = _savedLocation.asStateFlow()
    
    init {
        loadSavedLocation()
    }
    
    private fun loadSavedLocation() {
        // هنا هنحمل الموقع المحفوظ من DataStore
        // هيتنفذ بعد ما نعمل SettingsRepository
    }
    
    suspend fun getCurrentLocation(): Location {
        return suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resumeWithException(SecurityException("Location permission not granted"))
                return@suspendCancellableCoroutine
            }
            
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build()
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        fusedLocationClient.removeLocationUpdates(this)
                        continuation.resume(location)
                    }
                }
                
                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        fusedLocationClient.removeLocationUpdates(this)
                        continuation.resumeWithException(Exception("Location not available"))
                    }
                }
            }
            
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                continuation.resumeWithException(e)
            }
            
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }
    
    suspend fun saveLocation(location: Location, locationName: String) {
        val locationEntity = LocationEntity(
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = locationName,
            timestamp = System.currentTimeMillis()
        )
        _savedLocation.value = locationEntity
        settingsRepository.saveLocation(locationEntity)
    }
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

data class LocationEntity(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val timestamp: Long
)
