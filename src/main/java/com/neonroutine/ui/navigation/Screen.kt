package com.neonroutine.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.PhotoLibrary

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Today", Icons.Filled.Today)
    data object Grid : Screen("grid", "Grid", Icons.Filled.GridView)
    data object Month : Screen("month", "Month", Icons.Filled.CalendarViewMonth)
    data object Stats : Screen("stats", "Stats", Icons.Filled.BarChart)
    data object Memory : Screen("memory", "Memory", androidx.compose.material.icons.Icons.Filled.PhotoLibrary)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    data object AddTask : Screen("add_task", "Add Habit")
    data object EditTask : Screen("edit_task/{taskId}", "Edit Habit") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }
    data object Timer : Screen("timer/{taskId}/{timerIndex}", "Timer") {
        fun createRoute(taskId: String, timerIndex: Int) = "timer/$taskId/$timerIndex"
    }
    data object Camera : Screen("camera/{taskId}", "Camera") {
        fun createRoute(taskId: String) = "camera/$taskId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Grid, Screen.Month, Screen.Stats, Screen.Memory)
