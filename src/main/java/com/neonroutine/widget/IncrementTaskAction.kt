package com.neonroutine.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
// updateAll handled via WidgetUpdater
import com.neonroutine.data.db.AppDatabase
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Entry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class IncrementTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[ActionParameters.Key<String>("taskId")] ?: return
        val database = AppDatabase.getInstance(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 1. Update DB
        val existingEntry = database.entryDao().getEntry(taskId, todayStr) // Use getEntry instead of Once
        val task = database.taskDao().getTaskById(taskId) ?: return // Use getTaskById

        val currentValues = try {
            val json = existingEntry?.valuesJson ?: "{}"
            kotlinx.serialization.json.Json.decodeFromString<Map<String, Int>>(json)
        } catch (e: Exception) { emptyMap() }

        // Increment the primary value (usually target quantity)
        val newValue = (currentValues["value"] ?: 0) + 1
        val updatedValues = currentValues + ("value" to newValue)
        val updatedValuesJson = kotlinx.serialization.json.Json.encodeToString(updatedValues)

        // If we reached target, mark as completed
        val newState = if (newValue >= task.targetQuantity) {
            CompletionState.COMPLETED
        } else if (newValue > 0) {
            CompletionState.PARTIAL
        } else {
            CompletionState.NONE
        }

        if (existingEntry != null) {
            database.entryDao().updateEntry(existingEntry.copy(
                valuesJson = updatedValuesJson,
                completionState = newState,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            database.entryDao().insertEntry(Entry(
                taskId = taskId,
                date = todayStr,
                valuesJson = updatedValuesJson,
                completionState = newState,
                updatedAt = System.currentTimeMillis()
            ))
        }

        // Refresh ALL widgets immediately with fresh data
        WidgetUpdater.updateAllWidgets(context)
    }
}
