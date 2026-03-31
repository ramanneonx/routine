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

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[ActionParameters.Key<String>("taskId")] ?: return
        val currentStateStr = parameters[ActionParameters.Key<String>("currentState")] ?: CompletionState.NONE.name
        val currentState = try { CompletionState.valueOf(currentStateStr) } catch (e: Exception) { CompletionState.NONE }

        val newState = when (currentState) {
            CompletionState.NONE      -> CompletionState.COMPLETED
            CompletionState.COMPLETED -> CompletionState.NONE
            CompletionState.PARTIAL   -> CompletionState.COMPLETED
            CompletionState.MISSED    -> CompletionState.NONE
        }

        val database = AppDatabase.getInstance(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val existing = database.entryDao().getEntry(taskId, todayStr)
        if (existing != null) {
            database.entryDao().updateEntry(
                existing.copy(completionState = newState, updatedAt = System.currentTimeMillis())
            )
        } else {
            database.entryDao().insertEntry(
                Entry(
                    taskId = taskId,
                    date = todayStr,
                    completionState = newState,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Refresh ALL widgets immediately with fresh data
        WidgetUpdater.updateAllWidgets(context)
    }
}
