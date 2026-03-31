package com.neonroutine.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.neonroutine.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Boot completed — re-scheduling all reminders")

            // Re-schedule WorkManager periodic notifications (daily summary etc.)
            scheduleNotifications(context)

            // Re-schedule per-task exact alarms
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val activeTasks = db.taskDao().getAllActiveTasksOnce()
                Log.d("BootReceiver", "Re-scheduling ${activeTasks.size} task reminders")
                activeTasks.forEach { task ->
                    NotificationScheduler.scheduleRemindersForTask(context, task)
                }
            }
        }
    }
}
