package com.neonroutine.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class HabitCategory(val label: String, val emoji: String) {
    HEALTH("Health", "💪"),
    MIND("Mind", "🧠"),
    DISCIPLINE("Discipline", "🔥"),
    CUSTOM("Custom", "⭐")
}

@Serializable
enum class CompletionState {
    NONE, COMPLETED, PARTIAL, MISSED
}
