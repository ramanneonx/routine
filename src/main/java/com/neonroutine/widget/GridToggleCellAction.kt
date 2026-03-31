package com.neonroutine.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.neonroutine.data.db.AppDatabase
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Entry

/**
 * ActionCallback for the Weekly Grid Widget.
 * Receives taskId + dateStr, cycles the completion state,
 * and immediately refreshes all widgets.
 */
class GridToggleCellAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[ActionParameters.Key<String>("taskId")] ?: return
        val dateStr = parameters[ActionParameters.Key<String>("dateStr")] ?: return
        val currentStateStr = parameters[ActionParameters.Key<String>("currentState")]
            ?: CompletionState.NONE.name
        val currentState = try {
            CompletionState.valueOf(currentStateStr)
        } catch (_: Exception) {
            CompletionState.NONE
        }

        // Cycle: NONE → COMPLETED → NONE  (simple 2-state for widget)
        val newState = when (currentState) {
            CompletionState.NONE    -> CompletionState.COMPLETED
            CompletionState.COMPLETED -> CompletionState.NONE
            CompletionState.PARTIAL   -> CompletionState.COMPLETED
            CompletionState.MISSED    -> CompletionState.NONE
        }

        val database = AppDatabase.getInstance(context)
        val existing = database.entryDao().getEntry(taskId, dateStr)

        if (existing != null) {
            database.entryDao().updateEntry(
                existing.copy(completionState = newState, updatedAt = System.currentTimeMillis())
            )
        } else {
            database.entryDao().insertEntry(
                Entry(
                    taskId = taskId,
                    date = dateStr,
                    completionState = newState,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Refresh every widget on home screen immediately
        WidgetUpdater.updateAllWidgets(context)
    }
}
