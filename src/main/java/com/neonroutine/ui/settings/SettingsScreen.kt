package com.neonroutine.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.neonroutine.NeonRoutineApp
import com.neonroutine.data.prefs.ThemePreferences
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.glassHover
import com.neonroutine.ui.theme.glassPanel
import com.neonroutine.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    // Use app-wide singleton so changes are reflected immediately in the theme
    val themePrefs = remember { (context.applicationContext as NeonRoutineApp).themePreferences }
    val appPrefs = remember { (context.applicationContext as NeonRoutineApp).appPreferences }
    val themePreset by themePrefs.themePreset.collectAsState()
    val widgetTitle by appPrefs.widgetTitle.collectAsState()
    val widgetSubtitle by appPrefs.widgetSubtitle.collectAsState()
    val homeGreeting by appPrefs.homeGreeting.collectAsState()
    val motivationQuote by appPrefs.motivationQuote.collectAsState()
    
    // File picker for Quick JSON import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonStr = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                viewModel.importData(jsonStr)
                Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // File picker for Full Archive (.neonbak / .zip) restore
    val fullRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isRestoring = true
            scope.launch {
                val result = com.neonroutine.util.BackupRestoreUtil.restoreFullBackupArchive(context, uri, viewModel)
                isRestoring = false
                result.onSuccess { msg ->
                    Toast.makeText(context, "🎉 $msg", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "❌ Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Settings") 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance section
            SectionHeader("Appearance")

            // Theme
            SettingCard(
                icon = Icons.Filled.Settings,
                title = "Theme",
                subtitle = "Light, Dark, or System default"
            ) {
                val selectedTheme by themePrefs.themeMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Light", "Dark", "System").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { themePrefs.setThemeMode(index) }
                                .background(if (selectedTheme == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTheme == index) MaterialTheme.colorScheme.onPrimary 
                                        else if (themePreset.designStyle == com.neonroutine.ui.theme.DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Font Size
            SettingCard(
                icon = Icons.Filled.Settings,
                title = "Font Size",
                subtitle = "Adjust text size"
            ) {
                var selectedSize by remember { mutableIntStateOf(1) }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Small", "Medium", "Large").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSize = index }
                                .background(if (selectedSize == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedSize == index) MaterialTheme.colorScheme.onPrimary 
                                        else if (themePreset.designStyle == com.neonroutine.ui.theme.DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Theme Ecosystem — Full design-style preview cards
            SettingCard(
                icon = Icons.Filled.Palette,
                title = "Design Themes",
                subtitle = "Choose a complete design language — shapes, fonts & colors all change"
            ) {
                val designGroups = remember {
                    listOf(
                        "🌌 Neon Glow" to com.neonroutine.ui.theme.ThemePreset.entries.filter {
                            it.designStyle == com.neonroutine.ui.theme.DesignStyle.NEON_GLOW
                        },
                        "🌸 Soft Pastel" to com.neonroutine.ui.theme.ThemePreset.entries.filter {
                            it.designStyle == com.neonroutine.ui.theme.DesignStyle.SOFT_PASTEL
                        },
                        "⬛ Brutal Minimal" to com.neonroutine.ui.theme.ThemePreset.entries.filter {
                            it.designStyle == com.neonroutine.ui.theme.DesignStyle.BRUTAL_MINIMAL
                        },
                        "💎 Glass Visuals" to com.neonroutine.ui.theme.ThemePreset.entries.filter {
                            it.designStyle == com.neonroutine.ui.theme.DesignStyle.GLASSMORPHISM
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    designGroups.forEach { (groupName, presets) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                groupName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            presets.forEach { preset ->
                                val isSelected = themePreset == preset
                                val primaryColor = remember(preset.primaryHex) {
                                    try { Color(android.graphics.Color.parseColor(preset.primaryHex)) } catch(e:Exception){ Color.Gray }
                                }
                                val bgColor = remember(preset.backgroundHex) {
                                    try { Color(android.graphics.Color.parseColor(preset.backgroundHex)) } catch(e:Exception){ Color.Black }
                                }
                                val surfaceColor = remember(preset.surfaceHex) {
                                    try { Color(android.graphics.Color.parseColor(preset.surfaceHex)) } catch(e:Exception){ Color.DarkGray }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(76.dp)
                                        .then(
                                            if (preset.designStyle == com.neonroutine.ui.theme.DesignStyle.GLASSMORPHISM) {
                                                Modifier.glassPanel(shape = RoundedCornerShape(16.dp), color = surfaceColor).glassHover()
                                            } else {
                                                Modifier.clip(if (preset.designStyle == com.neonroutine.ui.theme.DesignStyle.BRUTAL_MINIMAL)
                                                    RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp))
                                                    .background(bgColor)
                                            }
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.dp, primaryColor,
                                                if (preset.designStyle == com.neonroutine.ui.theme.DesignStyle.BRUTAL_MINIMAL)
                                                    RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
                                            ) else if (preset.designStyle == com.neonroutine.ui.theme.DesignStyle.BRUTAL_MINIMAL)
                                                Modifier.border(1.dp, primaryColor.copy(alpha=0.6f), RoundedCornerShape(0.dp))
                                            else Modifier
                                        )
                                        .clickable { themePrefs.setThemePreset(preset) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Fake mini card preview
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(
                                                        when (preset.designStyle) {
                                                            com.neonroutine.ui.theme.DesignStyle.NEON_GLOW -> RoundedCornerShape(12.dp)
                                                            com.neonroutine.ui.theme.DesignStyle.SOFT_PASTEL -> RoundedCornerShape(8.dp)
                                                            com.neonroutine.ui.theme.DesignStyle.BRUTAL_MINIMAL -> RoundedCornerShape(0.dp)
                                                            com.neonroutine.ui.theme.DesignStyle.GLASSMORPHISM -> RoundedCornerShape(16.dp)
                                                        }
                                                    )
                                                    .background(surfaceColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(primaryColor)
                                                )
                                            }
                                            Column {
                                                androidx.compose.material3.Text(
                                                    preset.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (preset.isDark) Color.White else Color(android.graphics.Color.parseColor(preset.primaryHex))
                                                )
                                                androidx.compose.material3.Text(
                                                    preset.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (preset.isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = "Active",
                                                    tint = if (preset.isDark) Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Home Screen & Widget Customization ──────────────────────────────
            SectionHeader("Home Screen & Widgets")

            SettingCard(
                icon = Icons.Filled.Widgets,
                title = "Widget Text",
                subtitle = "Customize the header text shown on your home screen widgets"
            ) {
                var editingWidgetTitle by remember { mutableStateOf(widgetTitle) }
                var editingWidgetSubtitle by remember { mutableStateOf(widgetSubtitle) }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editingWidgetTitle,
                        onValueChange = {
                            editingWidgetTitle = it
                            appPrefs.setWidgetTitle(it)
                            com.neonroutine.widget.WidgetUpdater.updateAllWidgetsAsync(context)
                        },
                        label = { Text("Widget Header Title") },
                        placeholder = { Text("e.g. ⚡ My Habits") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )

                    OutlinedTextField(
                        value = editingWidgetSubtitle,
                        onValueChange = {
                            editingWidgetSubtitle = it
                            appPrefs.setWidgetSubtitle(it)
                            com.neonroutine.widget.WidgetUpdater.updateAllWidgetsAsync(context)
                        },
                        label = { Text("Widget Subtitle") },
                        placeholder = { Text("e.g. Today's Habits") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                }
            }

            SettingCard(
                icon = Icons.Filled.Home,
                title = "Home Greeting & Motivation",
                subtitle = "Personalize the greeting and quote shown on the Home tab"
            ) {
                var editingGreeting by remember { mutableStateOf(homeGreeting) }
                var editingQuote by remember { mutableStateOf(motivationQuote) }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editingGreeting,
                        onValueChange = {
                            editingGreeting = it
                            appPrefs.setHomeGreeting(it)
                        },
                        label = { Text("Home Greeting Text") },
                        placeholder = { Text("e.g. Let's crush it today! 🔥") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )

                    OutlinedTextField(
                        value = editingQuote,
                        onValueChange = {
                            editingQuote = it
                            appPrefs.setMotivationQuote(it)
                        },
                        label = { Text("Motivation Quote") },
                        placeholder = { Text("e.g. Small steps every day.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                }
            }

            // Notifications section
            SectionHeader("Notifications")

            SettingCard(
                icon = Icons.Filled.Notifications,
                title = "Daily Reminders",
                subtitle = "Manage habits notification settings"
            ) {
                val notifPrefs = (context.applicationContext as NeonRoutineApp).notificationPreferences
                val isEnabled by notifPrefs.notificationsEnabled.collectAsState()
                val reminderTime by notifPrefs.reminderTime.collectAsState()

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Notifications", style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.Switch(
                            checked = isEnabled,
                            onCheckedChange = { 
                                notifPrefs.setNotificationsEnabled(it)
                                com.neonroutine.notifications.scheduleNotifications(context)
                            }
                        )
                    }

                    if (isEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Time", style = MaterialTheme.typography.bodyMedium)
                            
                            // Simple mock time picker for now - cycles hours
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable {
                                        val parts = reminderTime.split(":")
                                        var hr = parts[0].toIntOrNull() ?: 9
                                        hr = (hr + 1) % 24
                                        val newTime = String.format(java.util.Locale.ROOT, "%02d:00", hr)
                                        notifPrefs.setReminderTime(newTime)
                                        com.neonroutine.notifications.scheduleNotifications(context)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                val displayTime = if (reminderTime.startsWith("12")) "12 PM"
                                else if (reminderTime.startsWith("00")) "12 AM"
                                else {
                                    val h = reminderTime.substring(0, 2).toIntOrNull() ?: 9
                                    if (h > 12) "${h - 12} PM" else "$h AM"
                                }
                                Text(displayTime, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            // Data section
            SectionHeader("Backup & Data Management")

            // Complete Full Backup (.neonbak / .zip with all tasks + memories & photos)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBackingUp) {
                        isBackingUp = true
                        scope.launch {
                            try {
                                val backupFile = com.neonroutine.util.BackupRestoreUtil.createFullBackupArchive(context, viewModel)
                                isBackingUp = false
                                com.neonroutine.util.BackupRestoreUtil.shareBackupFile(context, backupFile)
                            } catch (e: Exception) {
                                isBackingUp = false
                                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📦", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create Complete Backup (.neonbak)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Bundles all habits, statistics, notes, and photos into a single shareable backup archive",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }

            // Restore Complete Backup (.neonbak / .zip)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isRestoring) {
                        fullRestoreLauncher.launch("*/*")
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📥", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Restore Complete Backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Select a .neonbak / .zip file to restore tasks, history, and all memory photos",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }

            // Quick JSON Export
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            val jsonData = viewModel.exportAllData()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, jsonData)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Data (JSON)"))
                        }
                    },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📄", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Quick Export (JSON Text Only)", style = MaterialTheme.typography.titleSmall)
                        Text("Save habit definitions and history as lightweight JSON text", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Quick JSON Import
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { importLauncher.launch("application/json") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Quick Import (JSON Text Only)", style = MaterialTheme.typography.titleSmall)
                        Text("Restore from a JSON text backup file", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Clear all data
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Clear All Data", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Permanently resets all tasks, history, and data", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                    }
                }
            }

            // About
            SectionHeader("About")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NeonRoutine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Version 2.2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Modern Gamified Habit Tracking System", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Donate
            SectionHeader("Support Us")
            SettingCard(
                icon = Icons.Filled.Favorite,
                title = "Donate",
                subtitle = "Support NeonRoutine development"
            ) {
                Button(
                    onClick = {
                        val uri = Uri.parse("upi://pay?pa=raman8@fam&pn=NeonTimer&cu=INR")
                        val customIntent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(customIntent)
                        } catch (e: Exception) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("UPI ID", "raman8@fam")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "UPI ID copied: raman8@fam", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Donate via UPI (raman8@fam)")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Clear data confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all tasks and entries. Export your data first if you want to keep it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                    Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val designStyle = LocalDesignStyle.current
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (designStyle == DesignStyle.GLASSMORPHISM) {
                    Modifier.glassPanel(shape = RoundedCornerShape(12.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        title, 
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}


