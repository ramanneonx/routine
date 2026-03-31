package com.neonroutine.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import com.neonroutine.ui.theme.ThemePreset

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themePreset = MutableStateFlow(
        try { ThemePreset.valueOf(prefs.getString("theme_preset", ThemePreset.DEFAULT.name) ?: ThemePreset.DEFAULT.name) }
        catch (e: Exception) { ThemePreset.DEFAULT }
    )
    val themePreset: StateFlow<ThemePreset> = _themePreset

    // 0 = Light, 1 = Dark, 2 = System
    private val _themeMode = MutableStateFlow(prefs.getInt("theme_mode", 2))
    val themeMode: StateFlow<Int> = _themeMode

    fun setThemePreset(preset: ThemePreset) {
        prefs.edit().putString("theme_preset", preset.name).apply()
        _themePreset.value = preset
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _themeMode.value = mode
    }
}
