package com.mohamedabdelazeim.zekr.ui.prayer

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.ui.theme.DarkGreen
import com.mohamedabdelazeim.zekr.ui.theme.Gold
import com.mohamedabdelazeim.zekr.ui.theme.LightGreen
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PrayerTimesScreen(
    viewModel: PrayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prayerState by viewModel.prayerState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    // تحميل أولي
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            viewModel.loadPrayerTimes()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: الموقع والتاريخ
            PrayerHeader(
                location = locationState.locationName ?: "جاري تحديد الموقع...",
                hijriDate = prayerState.hijriDate ?: "",
                gregorianDate = prayerState.gregorianDate ?: "",
                onRefresh = {
                    if (locationPermissionState.status.isGranted) {
                        viewModel.refreshPrayerTimes()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // عرض الصلاحية لو مش موجودة
            if (!locationPermissionState.status.isGranted) {
                PermissionCard(
                    onRequestPermission = { locationPermissionState.launchPermissionRequest() }
                )
            } else {
                // قائمة المواقيت
                PrayerTimesList(
                    prayers = prayerState.prayers,
                    nextPrayer = prayerState.nextPrayer
                )
            }
        }
    }
}

@Composable
fun PrayerHeader(
    location: String,
    hijriDate: String,
    gregorianDate: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkGreen.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // الموقع مع أيقونة التحديث
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null
