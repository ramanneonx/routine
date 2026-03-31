package com.neonroutine

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neonroutine.ui.navigation.AppNavigation
import com.neonroutine.ui.theme.TimetableTheme
import com.neonroutine.ui.viewmodel.TaskViewModel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force GPU hardware acceleration for 60/120 FPS rendering
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        // Install splash BEFORE super.onCreate for instant dark boot
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Use the app-wide singleton so Settings changes are immediately reflected here
            val themePrefs = (application as NeonRoutineApp).themePreferences
            val themePreset by themePrefs.themePreset.collectAsState()
            val themeMode by themePrefs.themeMode.collectAsState()

            val context = LocalContext.current
            val taskViewModel: TaskViewModel = viewModel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(context, "Notifications are needed for reminders to work!", Toast.LENGTH_LONG).show()
                    }
                }
                
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(Unit) {
                taskViewModel.uiEvent.collect { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val userDarkPref = when (themeMode) {
                0    -> false
                1    -> true
                else -> isSystemDark
            }
            // For Neon Glow presets use user preference; for Pastel/Brutal the preset itself defines light/dark
            val darkTheme = if (themePreset.designStyle == com.neonroutine.ui.theme.DesignStyle.NEON_GLOW) {
                userDarkPref
            } else {
                themePreset.isDark
            }

            TimetableTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themePreset = themePreset
            ) {
                AppNavigation()
            }
        }
    }
}
