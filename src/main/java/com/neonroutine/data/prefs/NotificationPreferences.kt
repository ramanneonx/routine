package com.neonroutine.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _reminderTime = MutableStateFlow(prefs.getString("reminder_time", "09:00") ?: "09:00")
    val reminderTime: StateFlow<String> = _reminderTime

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    // time is expected in HH:mm format
    fun setReminderTime(time: String) {
        prefs.edit().putString("reminder_time", time).apply()
        _reminderTime.value = time
    }
}
