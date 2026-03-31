package com.neonroutine.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neonroutine.R
import com.neonroutine.NeonRoutineApp
import com.neonroutine.data.db.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val DAILY_REMINDER_CHANNEL = "NeonRoutine_reminders"
const val DAILY_SUMMARY_CHANNEL = "NeonRoutine_summary"

class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        createNotificationChannels(applicationContext)
        val db = AppDatabase.getInstance(applicationContext)
        val tasks = db.taskDao().getAllActiveTasksOnce()
        if (tasks.isNotEmpty()) {
            showNotification(
                applicationContext,
                channelId = DAILY_REMINDER_CHANNEL,
                title = "⏰ Habit Reminder",
                message = "You have ${tasks.size} habits waiting. Keep the streak alive! 🔥",
                id = 1001
            )
        }
        return Result.success()
    }
}

class DailySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        createNotificationChannels(applicationContext)
        val db = AppDatabase.getInstance(applicationContext)
        val tasks = db.taskDao().getAllActiveTasksOnce()
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val entries = db.entryDao().getEntriesInRangeOnce(today, today)
        val completed = entries.count { it.completionState == com.neonroutine.data.model.CompletionState.COMPLETED }
        val total = tasks.size
        val pct = if (total > 0) (completed * 100 / total) else 0

        showNotification(
            applicationContext,
            channelId = DAILY_SUMMARY_CHANNEL,
            title = "📊 Daily Summary",
            message = "You completed $completed/$total habits today — $pct%! ${if (pct >= 80) "🏆 Amazing day!" else if (pct >= 50) "💪 Keep going!" else "Try harder tomorrow!"}",
            id = 1002
        )
        return Result.success()
    }
}

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // HIGH importance = heads-up banners + sound
        manager.createNotificationChannel(
            NotificationChannel(DAILY_REMINDER_CHANNEL, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = "Daily reminders to track habits"
                    enableVibration(true)
                }
        )
        manager.createNotificationChannel(
            NotificationChannel(DAILY_SUMMARY_CHANNEL, "Daily Summary", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "End-of-day habit summary" }
        )
    }
}

fun showNotification(context: Context, channelId: String, title: String, message: String, id: Int) {
    val manager = context.getSystemService(NotificationManager::class.java)
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(com.neonroutine.R.drawable.ic_stat_notification)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    manager.notify(id, notification)
}


fun scheduleNotifications(context: Context) {
    val workManager = WorkManager.getInstance(context)
    val prefs = (context.applicationContext as NeonRoutineApp).notificationPreferences
    
    if (!prefs.notificationsEnabled.value) {
        workManager.cancelAllWork()
        return
    }

    val timeStr = prefs.reminderTime.value
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    if (target.before(now)) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }

    val delay = target.timeInMillis - now.timeInMillis

    val reminderRequest = PeriodicWorkRequestBuilder<HabitReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
    workManager.enqueueUniquePeriodicWork("habit_reminder", ExistingPeriodicWorkPolicy.REPLACE, reminderRequest)

    // Summary at end of day (e.g. 9 PM)
    val summaryTarget = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    if (summaryTarget.before(now)) {
        summaryTarget.add(Calendar.DAY_OF_YEAR, 1)
    }
    val summaryDelay = summaryTarget.timeInMillis - now.timeInMillis
    
    val summaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(summaryDelay, TimeUnit.MILLISECONDS)
        .build()
    workManager.enqueueUniquePeriodicWork("daily_summary", ExistingPeriodicWorkPolicy.REPLACE, summaryRequest)
}
