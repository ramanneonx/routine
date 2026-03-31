package com.neonroutine

import com.neonroutine.widget.WidgetUpdater

import android.app.Application
import com.neonroutine.data.db.AppDatabase
import com.neonroutine.data.prefs.ThemePreferences
import com.neonroutine.data.prefs.NotificationPreferences
import com.neonroutine.data.repository.RoutineRepository
import com.neonroutine.notifications.scheduleNotifications

class NeonRoutineApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { RoutineRepository(database.taskDao(), database.entryDao()) }
    // Singleton ThemePreferences – shared across MainActivity and SettingsScreen
    val themePreferences by lazy { ThemePreferences(this) }
    val notificationPreferences by lazy { NotificationPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        WidgetUpdater.schedulePeriodicSync(this)
        
        // Setup crash logger to launch CrashActivity
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = android.util.Log.getStackTraceString(throwable)
                val intent = android.content.Intent(applicationContext, CrashActivity::class.java).apply {
                    putExtra("EXTRA_CRASH_INFO", trace)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        scheduleNotifications(this)
    }
}
