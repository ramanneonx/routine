package com.neonroutine.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.neonroutine.data.model.Task
import kotlinx.serialization.json.Json
import java.util.Calendar

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"

    fun scheduleRemindersForTask(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminders: List<String> = try {
            Json.decodeFromString(task.remindersJson)
        } catch (e: Exception) {
            emptyList()
        }

        // Cancel existing alarms for this task first
        cancelRemindersForTask(context, task.id)

        reminders.forEachIndexed { index, timeStr ->
            // timeStr format: "HH:mm" or "HH:mm|Custom Message"
            // Must split on FIRST colon only to get hour, then parse remainder for minutes
            val pipeIndex = timeStr.indexOf('|')
            val timePart = if (pipeIndex >= 0) timeStr.substring(0, pipeIndex) else timeStr
            val customMsg = if (pipeIndex >= 0) timeStr.substring(pipeIndex + 1) else ""

            val colonIndex = timePart.indexOf(':')
            if (colonIndex < 0) return@forEachIndexed

            val hr = timePart.substring(0, colonIndex).toIntOrNull() ?: return@forEachIndexed
            val min = timePart.substring(colonIndex + 1).toIntOrNull() ?: return@forEachIndexed

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hr)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("REMINDER_MSG", customMsg)
                putExtra("REMINDER_HOUR", hr)
                putExtra("REMINDER_MIN", min)
                putExtra("REMINDER_INDEX", index)
            }

            val reqCode = (task.id.hashCode() and 0x7FFFFFFF) + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                        // setAlarmClock absolutely guarantees exact timing by bypassing all Doze and battery restrictions
                        val acInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                        alarmManager.setAlarmClock(acInfo, pendingIntent)
                        Log.d(TAG, "Scheduled exact AlarmClock for task=${task.title} at $hr:$min reqCode=$reqCode")
                    }
                    else -> {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled alarm (legacy) for task=${task.title} at $hr:$min reqCode=$reqCode")
                    }
                }
            } catch (e: SecurityException) {
                // SCHEDULE_EXACT_ALARM denied — use inexact as last resort
                Log.w(TAG, "Exact alarm permission denied, using inexact fallback: ${e.message}")
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
    }

    fun cancelRemindersForTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel up to 10 reminder slots per task
        for (i in 0..9) {
            val reqCode = (taskId.hashCode() and 0x7FFFFFFF) + i
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled alarm taskId=$taskId reqCode=$reqCode")
            }
        }
    }
}
