package com.neonroutine.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deviceId: String = "",
    val title: String,
    val color: String = "#7F77DD",
    val iconKey: String = "default",
    val category: HabitCategory = HabitCategory.HEALTH,
    val recurrence: Recurrence = Recurrence.DAILY,
    val recurrenceDays: String = "[]",
    val startDate: String = "",
    val endDate: String? = null,
    val columnsJson: String = "[]",
    val targetQuantity: Int = 1,         // e.g., 8 glasses of water
    val pointsValue: Int = 10,            // points earned per full completion
    val isArchived: Boolean = false,
    val remindersJson: String = "[]",     // array of HH:mm|Message strings
    val timersJson: String = "[]",        // array of MM|Label strings max 999 mins
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isScheduledForDate(date: java.time.LocalDate): Boolean {
        // Check start bounds
        if (this.startDate.isNotBlank()) {
            try {
                val start = java.time.LocalDate.parse(this.startDate)
                if (date.isBefore(start)) return false
            } catch(e: Exception) {}
        }
        // Check end bounds
        if (this.endDate != null && this.endDate.isNotBlank()) {
            try {
                val end = java.time.LocalDate.parse(this.endDate)
                if (date.isAfter(end)) return false
            } catch(e: Exception) {}
        }

        return when (this.recurrence) {
            Recurrence.DAILY -> true
            Recurrence.WEEKLY -> {
                if (this.startDate.isNotBlank()) {
                    try {
                        val start = java.time.LocalDate.parse(this.startDate)
                        date.dayOfWeek == start.dayOfWeek
                    } catch(e: Exception) { true }
                } else true
            }
            Recurrence.MONTHLY -> {
                if (this.startDate.isNotBlank()) {
                    try {
                        val start = java.time.LocalDate.parse(this.startDate)
                        date.dayOfMonth == start.dayOfMonth
                    } catch(e: Exception) { true }
                } else true
            }
            Recurrence.CUSTOM -> {
                try {
                    val days = kotlinx.serialization.json.Json.decodeFromString<List<Int>>(this.recurrenceDays)
                    date.dayOfWeek.value in days // 1=Mon..7=Sun
                } catch (e: Exception) { true }
            }
            Recurrence.INTERVAL -> {
                try {
                    val start = if (this.startDate.isNotBlank()) java.time.LocalDate.parse(this.startDate) else date
                    val interval = kotlinx.serialization.json.Json.decodeFromString<List<Int>>(this.recurrenceDays).firstOrNull() ?: 2
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, date)
                    daysBetween >= 0 && daysBetween % interval == 0L
                } catch (e: Exception) { true }
            }
        }
    }
}
