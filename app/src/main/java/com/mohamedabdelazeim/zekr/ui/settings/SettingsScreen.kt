package com.mohamedabdelazeim.zekr.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.ui.theme.DarkGreen
import com.mohamedabdelazeim.zekr.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val calculationMethod by viewModel.calculationMethod.collectAsState()
    val azkarInterval by viewModel.azkarInterval.collectAsState()
    val manualOffsets by viewModel.manualOffsets.collectAsState()
    val selectedAdhanSounds by viewModel.selectedAdhanSounds.collectAsState()
    
    // الإعدادات الجديدة
    val silentAdhan by viewModel.silentAdhan.collectAsState()
    val prayerReminderEnabled by viewModel.prayerReminderEnabled.collectAsState()
    val azkarReminderEnabled by viewModel.azkarReminderEnabled.collectAsState()
    val azkarAudioEnabled by viewModel.azkarAudioEnabled.collectAsState()

    var showCalculationMethodDialog by remember { mutableStateOf(false) }
    var showAzkarIntervalDialog by remember { mutableStateOf(false) }
    var showManualAdjustDialog by remember { mutableStateOf(false) }
    var selectedPrayerForSound by remember { mutableStateOf<String?>(null) }
    var showSilentAdhanDialog by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPrayerForSound?.let { prayer ->
                viewModel.setAdhanSound(prayer, it)
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedPrayerForSound = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // عنوان التطبيق
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "محمد عبد العظيم الطويل الإسلامي",
                        color = Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "صلاتي",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // قسم الصلاحيات
        item {
            SectionHeader(title = "الصلاحيات")
        }

        item {
            PermissionCard(
                title = "الرسم فوق التطبيقات",
                description = "لظهور شاشة الأذان فوق أي تطبيق",
                icon = Icons.Default.OpenInNew,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }

        item {
            PermissionCard(
                title = "تجاهل تحسينات البطارية",
                description = "لضمان عمل التطبيق في الخلفية",
                icon = Icons.Default.BatterySaver,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                }
            )
        }

        item {
            PermissionCard(
                title = "التنبيه الدقيق",
                description = "لتشغيل الأذان في الوقت المحدد",
                icon = Icons.Default.Alarm,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                }
            )
        }

        // قسم إعدادات الصلاة
        item {
            SectionHeader(title = "إعدادات الصلاة")
        }

        item {
            SettingsItem(
                title = "طريقة الحساب",
                subtitle = calculationMethod,
                icon = Icons.Default.Calculate,
                onClick = { showCalculationMethodDialog = true }
            )
        }

        item {
            SettingsItem(
                title = "التعديل اليدوي للمواقيت",
                subtitle = "تعديل أوقات الصلاة بالدقائق",
                icon = Icons.Default.Edit,
                onClick = { showManualAdjustDialog = true }
            )
        }

        // قسم الأذان
        item {
            SectionHeader(title = "إعدادات الأذان")
        }

        item {
            SettingsItem(
                title = "الأذان الصامت",
                subtitle = "اختيار الصلوات التي لا يصدر فيها صوت",
                icon = Icons.Default.VolumeOff,
                onClick = { showSilentAdhanDialog = true }
            )
        }

        // أصوات الأذان لكل صلاة
        listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء").forEach { prayer ->
            item {
                SoundSelectionItem(
                    prayerName = prayer,
                    selectedSound = selectedAdhanSounds[prayer],
                    isSilent = silentAdhan[prayer] ?: false,
                    onClick = {
                        selectedPrayerForSound = prayer
                        audioPickerLauncher.launch("audio/*")
                    }
                )
            }
        }

        // قسم التذكيرات
        item {
            SectionHeader(title = "التذكيرات")
        }

        item {
            SwitchSettingItem(
                title = "تذكير بالصلاة",
                description = "إشعار بعد الصلاة: هل صليت؟",
                icon = Icons.Default.Notifications,
                checked = prayerReminderEnabled,
                onCheckedChange = { viewModel.setPrayerReminderEnabled(it) }
            )
        }

        item {
            SwitchSettingItem(
                title = "تذكير بالأذكار",
                description = "إشعارات منبثقة بالأذكار",
                icon = Icons.Default.Menu,
                checked = azkarReminderEnabled,
                onCheckedChange = { viewModel.setAzkarReminderEnabled(it) }
            )
        }

        // قسم الأذكار
        item {
            SectionHeader(title = "الأذكار المتكررة")
        }

        item {
            SettingsItem(
                title = "فترة التكرار",
                subtitle = "$azkarInterval دقيقة",
                icon = Icons.Default.Timer,
                onClick = { showAzkarIntervalDialog = true }
            )
        }

        item {
            SwitchSettingItem(
                title = "تشغيل صوت الأذكار",
                description = "تشغيل صوت عند تكرار الأذكار",
                icon = Icons.Default.VolumeUp,
                checked = azkarAudioEnabled,
                onCheckedChange = { viewModel.setAzkarAudioEnabled(it) }
            )
        }

        // معلومات التطبيق
        item {
            SectionHeader(title = "معلومات")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الإصدار 1.0.0",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "برمجيات محمد عبد العظيم الطويل",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // حوار الأذان الصامت
    if (showSilentAdhanDialog) {
        AlertDialog(
            onDismissRequest = { showSilentAdhanDialog = false },
            title = { Text("الأذان الصامت", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء").forEach { prayer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSilentAdhan(prayer) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prayer, color = Color.White, fontSize = 16.sp)
                            Switch(
                                checked = silentAdhan[prayer] ?: false,
                                onCheckedChange = { viewModel.toggleSilentAdhan(prayer) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Gold,
                                    checkedTrackColor = Gold.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSilentAdhanDialog = false }) {
                    Text("تم", color = Gold)
                }
            },
            containerColor = DarkGreen
        )
    }

    // حوار اختيار طريقة الحساب
    if (showCalculationMethodDialog) {
        AlertDialog(
            onDismissRequest = { showCalculationMethodDialog = false },
            title = { Text("طريقة الحساب", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        "MuslimWorldLeague" to "رابطة العالم الإسلامي",
                        "Egyptian" to "الهيئة المصرية العامة للمساحة",
                        "Karachi" to "جامعة العلوم الإسلامية - كراتشي",
                        "UmmAlQura" to "أم القرى - مكة المكرمة",
                        "Dubai" to "دبي",
                        "NorthAmerica" to "أمريكا الشمالية"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCalculationMethod(value)
                                    showCalculationMethodDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = calculationMethod == value,
                                onClick = {
                                    viewModel.setCalculationMethod(value)
                                    showCalculationMethodDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Gold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalculationMethodDialog = false }) {
                    Text("إلغاء", color = Gold)
                }
            },
            containerColor = DarkGreen
        )
    }

    // حوار اختيار فترة الأذكار
    if (showAzkarIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showAzkarIntervalDialog = false },
            title = { Text("فترة تكرار الأذكار", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(5, 10, 15, 20, 30, 60).forEach { interval ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAzkarInterval(interval)
                                    showAzkarIntervalDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = azkarInterval == interval,
                                onClick = {
                                    viewModel.setAzkarInterval(interval)
                                    showAzkarIntervalDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Gold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$interval دقيقة", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAzkarIntervalDialog = false }) {
                    Text("إلغاء", color = Gold)
                }
            },
            containerColor = DarkGreen
        )
    }

    // حوار التعديل اليدوي
    if (showManualAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showManualAdjustDialog = false },
            title = { Text("التعديل اليدوي (بالدقائق)", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء").forEach { prayer ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prayer, color = Color.White, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.decrementOffset(prayer) }) {
                                    Icon(Icons.Default.Remove, null, tint = Gold)
                                }
                                Text(
                                    text = "${manualOffsets[prayer] ?: 0}",
                                    color = Gold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { viewModel.incrementOffset(prayer) }) {
                                    Icon(Icons.Default.Add, null, tint = Gold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualAdjustDialog = false }) {
                    Text("تم", color = Gold)
                }
            },
            containerColor = DarkGreen
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Gold,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(text = description, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Gold.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(text = description, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Icon(Icons.Default.Settings, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SoundSelectionItem(
    prayerName: String,
    selectedSound: String?,
    isSilent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkGreen.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSilent) Icons.Default.VolumeOff else Icons.Default.MusicNote,
                null,
                tint = if (isSilent) Color.Gray else Gold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "أذان $prayerName",
                    color = if (isSilent) Color.Gray else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when {
                        isSilent -> "صامت"
                        selectedSound != null -> "تم اختيار صوت مخصص"
                        else -> "الصوت الافتراضي"
                    },
                    color = when {
                        isSilent -> Color.Gray
                        selectedSound != null -> Color(0xFF4CAF50)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontSize = 13.sp
                )
            }
            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}
