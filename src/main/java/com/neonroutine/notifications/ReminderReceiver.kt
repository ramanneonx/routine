package com.neonroutine.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.neonroutine.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val taskId = intent.getStringExtra("TASK_ID") ?: run {
            pendingResult.finish()
            return
        }
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Your Habit"
        val customMsg = intent.getStringExtra("REMINDER_MSG") ?: ""
        val hour = intent.getIntExtra("REMINDER_HOUR", -1)
        val min = intent.getIntExtra("REMINDER_MIN", -1)
        val reminderIndex = intent.getIntExtra("REMINDER_INDEX", 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task != null && task.isScheduledForDate(LocalDate.now())) {
                    createNotificationChannels(context)

                    val message = if (customMsg.isNotBlank()) customMsg
                                  else "Stay consistent! Complete it now to keep your streak going."

                    showNotification(
                        context,
                        channelId = DAILY_REMINDER_CHANNEL,
                        title = "⏰ Time for $taskTitle",
                        message = message,
                        id = taskId.hashCode() + reminderIndex
                    )
                }

                // Re-schedule next day's alarm (setExactAndAllowWhileIdle is one-shot)
                if (hour >= 0 && min >= 0) {
                    rescheduleNextDay(context, intent, taskId, taskTitle, customMsg, hour, min, reminderIndex)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun rescheduleNextDay(
        context: Context,
        originalIntent: Intent,
        taskId: String,
        taskTitle: String,
        customMsg: String,
        hour: Int,
        min: Int,
        reminderIndex: Int
    ) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, 1) // always next day
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", taskTitle)
            putExtra("REMINDER_MSG", customMsg)
            putExtra("REMINDER_HOUR", hour)
            putExtra("REMINDER_MIN", min)
            putExtra("REMINDER_INDEX", reminderIndex)
        }

        val reqCode = (taskId.hashCode() and 0x7FFFFFFF) + reminderIndex
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("ReminderReceiver", "Re-scheduled next alarm for $taskTitle at $hour:$min tomorrow")
        } catch (e: SecurityException) {
            Log.w("ReminderReceiver", "Could not re-schedule exact alarm: ${e.message}")
        }
    }
}
