package com.kkfittracking.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

/** Steps and distance of a whole day, from every app and device that writes to Health Connect. */
data class DailyActivity(val steps: Long, val meters: Double?)

/**
 * Reads the daily steps and distance that Samsung Health, Google Fit, the watch and other apps
 * store in Health Connect. Health Connect removes the double counting when the phone and the watch
 * both count the same steps. Nothing is written.
 */
class HealthConnect(private val context: Context) {
    enum class Status { AVAILABLE, NEEDS_UPDATE, UNAVAILABLE }

    /** The permissions to ask for. */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun status(): Status = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Status.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NEEDS_UPDATE
        else -> Status.UNAVAILABLE
    }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasPermissions(): Boolean =
        status() == Status.AVAILABLE && client.permissionController.getGrantedPermissions().containsAll(permissions)

    /** The day's totals, or null when Health Connect cannot be read. */
    suspend fun dailyActivity(date: LocalDate): DailyActivity? = runCatching {
        val zone = ZoneId.systemDefault()
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    date.atStartOfDay(zone).toInstant(),
                    date.plusDays(1).atStartOfDay(zone).toInstant(),
                ),
            ),
        )
        DailyActivity(steps = result[StepsRecord.COUNT_TOTAL] ?: 0, meters = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters)
    }.getOrNull()

    companion object {
        /** Where Health Connect is installed or updated on Android 13 and older. */
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
