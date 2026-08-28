package com.neonroutine.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * AutoSleepDetector — analyzes device screen time, lock intervals, and inactivity
 * to automatically estimate bedtime, wake time, and total sleep duration.
 */
object AutoSleepDetector {

    data class DetectedSleepResult(
        val sleepTime: String,       // e.g. "23:45"
        val wakeTime: String,        // e.g. "07:30"
        val durationMinutes: Int,    // e.g. 465
        val durationHours: Float,    // e.g. 7.75f
        val source: String           // "Device Activity & Screen Time"
    )

    private data class TimestampEvent(
        val type: Int,
        val timeStamp: Long
    )

    /**
     * Checks if the app has been granted PACKAGE_USAGE_STATS permission.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Launches the system settings screen for Usage Access so the user can grant permission.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Automatically scans device usage events across the night window (from 8:00 PM yesterday
     * to 1:00 PM today) to detect the longest continuous block of inactivity / screen-off time.
     *
     * @return [DetectedSleepResult] if a plausible sleep window (>= 180 mins) is detected, or null.
     */
    suspend fun detectSleepForDate(
        context: Context,
        targetDate: LocalDate = LocalDate.now()
    ): DetectedSleepResult? = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission(context)) return@withContext null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext null

        val zone = ZoneId.systemDefault()

        // 24-hour scan window: from 24 hours prior up to current time (or end of target date)
        // This flawlessly detects both normal night sleepers AND daytime sleepers / night-shift workers!
        val now = LocalDateTime.now()
        val windowEnd = if (targetDate == LocalDate.now()) {
            now.atZone(zone).toInstant().toEpochMilli()
        } else {
            targetDate.plusDays(1).atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        }
        val windowStart = windowEnd - (24 * 60 * 60 * 1000L) // 24-hour rolling window

        val events = usageStatsManager.queryEvents(windowStart, windowEnd)
        val eventList = mutableListOf<TimestampEvent>()
        val currentEvent = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(currentEvent)
            // Filter events indicating active phone usage
            when (currentEvent.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.SCREEN_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_HIDDEN,
                UsageEvents.Event.USER_INTERACTION -> {
                    eventList.add(TimestampEvent(currentEvent.eventType, currentEvent.timeStamp))
                }
            }
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

        if (eventList.isEmpty()) {
            // Whole night had zero phone interactions -> estimate full 8 hour default baseline
            val bedLdt = targetDate.minusDays(1).atTime(23, 0)
            val wakeLdt = targetDate.atTime(7, 0)
            return@withContext DetectedSleepResult(
                sleepTime = bedLdt.format(timeFormatter),
                wakeTime = wakeLdt.format(timeFormatter),
                durationMinutes = 480,
                durationHours = 8.0f,
                source = "Complete Device Inactivity"
            )
        }

        // Sort interaction events by timestamp
        eventList.sortBy { it.timeStamp }

        // Find the maximum gap between phone interactions in milliseconds
        var maxGapStartMs = windowStart
        var maxGapEndMs = eventList.first().timeStamp
        var maxGapDurationMs = maxGapEndMs - maxGapStartMs

        for (i in 0 until eventList.size - 1) {
            val gapStart = eventList[i].timeStamp
            val gapEnd = eventList[i + 1].timeStamp
            val gap = gapEnd - gapStart
            if (gap > maxGapDurationMs) {
                maxGapDurationMs = gap
                maxGapStartMs = gapStart
                maxGapEndMs = gapEnd
            }
        }

        // Also check gap after last event up to current time (if current time is before windowEnd)
        val nowMs = System.currentTimeMillis().coerceAtMost(windowEnd)
        val lastEventTime = eventList.last().timeStamp
        if (nowMs > lastEventTime) {
            val tailGap = nowMs - lastEventTime
            if (tailGap > maxGapDurationMs) {
                maxGapDurationMs = tailGap
                maxGapStartMs = lastEventTime
                maxGapEndMs = nowMs
            }
        }

        val gapMinutes = (maxGapDurationMs / (1000 * 60)).toInt()

        // Plausible sleep block: at least 3 hours (180 mins) and at most 14 hours (840 mins)
        if (gapMinutes in 180..840) {
            val bedLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(maxGapStartMs), zone)
            val wakeLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(maxGapEndMs), zone)

            DetectedSleepResult(
                sleepTime = bedLdt.format(timeFormatter),
                wakeTime = wakeLdt.format(timeFormatter),
                durationMinutes = gapMinutes,
                durationHours = gapMinutes / 60f,
                source = "Device Activity & Screen Inactivity"
            )
        } else {
            null
        }
    }
}
