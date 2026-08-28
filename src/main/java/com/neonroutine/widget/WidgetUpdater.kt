package com.neonroutine.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WidgetUpdater {

    private const val WIDGET_SYNC_WORK = "neonroutine_widget_sync"

    /**
     * Called after every data mutation (suspend — use from coroutine scope).
     * Uses updateAll() which is the safest Glance API.
     * We add a small delay to ensure the database has finished its disk write
     * before we trigger the widget re-fetch.
     */
    suspend fun updateAllWidgets(context: Context) {
        // Essential delay for real-time consistency on some Android OS flavors
        delay(250)
        
        try { RoutineWidget().updateAll(context) } catch (_: Exception) {}
        try { ProgressWidget().updateAll(context) } catch (_: Exception) {}
        try { StreakWidget().updateAll(context) } catch (_: Exception) {}
        try { WeeklyGridWidget().updateAll(context) } catch (_: Exception) {}
    }

    /**
     * Fire-and-forget version for non-coroutine callers (e.g. Settings onValueChange).
     * Uses GlobalScope intentionally — widget updates are app-level operations
     * that should not be tied to any single screen's lifecycle.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun updateAllWidgetsAsync(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            updateAllWidgets(context)
        }
    }

    /** Schedule a periodic background sync every 15 min so widgets stay fresh even when app is closed. */
    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
