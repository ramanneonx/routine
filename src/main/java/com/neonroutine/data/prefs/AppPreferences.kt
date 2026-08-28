package com.neonroutine.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AppPreferences — central store for user-editable app & widget text preferences.
 * Widgets read directly from SharedPreferences (via companion object) since they run
 * outside Compose and cannot observe StateFlow.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Observable StateFlows (for Compose UI) ─────────────────────────────

    private val _widgetTitle = MutableStateFlow(
        prefs.getString(KEY_WIDGET_TITLE, DEFAULT_WIDGET_TITLE) ?: DEFAULT_WIDGET_TITLE
    )
    val widgetTitle: StateFlow<String> = _widgetTitle

    private val _widgetSubtitle = MutableStateFlow(
        prefs.getString(KEY_WIDGET_SUBTITLE, DEFAULT_WIDGET_SUBTITLE) ?: DEFAULT_WIDGET_SUBTITLE
    )
    val widgetSubtitle: StateFlow<String> = _widgetSubtitle

    private val _homeGreeting = MutableStateFlow(
        prefs.getString(KEY_HOME_GREETING, DEFAULT_HOME_GREETING) ?: DEFAULT_HOME_GREETING
    )
    val homeGreeting: StateFlow<String> = _homeGreeting

    private val _motivationQuote = MutableStateFlow(
        prefs.getString(KEY_MOTIVATION_QUOTE, DEFAULT_MOTIVATION_QUOTE) ?: DEFAULT_MOTIVATION_QUOTE
    )
    val motivationQuote: StateFlow<String> = _motivationQuote

    private val _autoSleepEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SLEEP_ENABLED, false)
    )
    val autoSleepEnabled: StateFlow<Boolean> = _autoSleepEnabled

    // ── Setters ─────────────────────────────────────────────────────────────

    fun setAutoSleepEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SLEEP_ENABLED, value).apply()
        _autoSleepEnabled.value = value
    }

    fun setWidgetTitle(value: String) {
        prefs.edit().putString(KEY_WIDGET_TITLE, value.take(40)).apply()
        _widgetTitle.value = value.take(40)
    }

    fun setWidgetSubtitle(value: String) {
        prefs.edit().putString(KEY_WIDGET_SUBTITLE, value.take(60)).apply()
        _widgetSubtitle.value = value.take(60)
    }

    fun setHomeGreeting(value: String) {
        prefs.edit().putString(KEY_HOME_GREETING, value.take(60)).apply()
        _homeGreeting.value = value.take(60)
    }

    fun setMotivationQuote(value: String) {
        prefs.edit().putString(KEY_MOTIVATION_QUOTE, value.take(120)).apply()
        _motivationQuote.value = value.take(120)
    }

    // ── Companion for static reads (used by Glance Widgets) ─────────────────

    companion object {
        const val PREFS_NAME = "neon_app_prefs"
        const val KEY_WIDGET_TITLE = "widget_title"
        const val KEY_WIDGET_SUBTITLE = "widget_subtitle"
        const val KEY_HOME_GREETING = "home_greeting"
        const val KEY_MOTIVATION_QUOTE = "motivation_quote"
        const val KEY_AUTO_SLEEP_ENABLED = "auto_sleep_enabled"

        const val DEFAULT_WIDGET_TITLE = "⚡ NeonRoutine"
        const val DEFAULT_WIDGET_SUBTITLE = "Today's Habits"
        const val DEFAULT_HOME_GREETING = "Let's build habits! 💪"
        const val DEFAULT_MOTIVATION_QUOTE = "Small steps every day."

        fun readWidgetTitle(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_WIDGET_TITLE, DEFAULT_WIDGET_TITLE) ?: DEFAULT_WIDGET_TITLE

        fun readWidgetSubtitle(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_WIDGET_SUBTITLE, DEFAULT_WIDGET_SUBTITLE) ?: DEFAULT_WIDGET_SUBTITLE

        fun readHomeGreeting(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_HOME_GREETING, DEFAULT_HOME_GREETING) ?: DEFAULT_HOME_GREETING

        fun readMotivationQuote(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MOTIVATION_QUOTE, DEFAULT_MOTIVATION_QUOTE) ?: DEFAULT_MOTIVATION_QUOTE
    }
}
