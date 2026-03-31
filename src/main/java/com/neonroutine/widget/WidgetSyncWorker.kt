package com.neonroutine.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background worker that refreshes all home-screen widgets every 30 minutes.
 * This compensates for Android's 30-min floor on appwidget-provider updatePeriodMillis.
 */
class WidgetSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            WidgetUpdater.updateAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
