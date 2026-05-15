package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val HOME_REALTIME_SPEED_MIN_SAMPLE_INTERVAL_MS = 1_000L
internal const val HOME_REALTIME_SPEED_MAX_POINTS = 60

internal class RealtimeSpeedTracker(
    private val connectionStore: ConnectionStore,
) {
    val mutex = Mutex()
    val series = mutableListOf<RealtimeSpeedPoint>()
    private var restored = false
    private var scopeKey: String? = null

    suspend fun sampleLocked(
        transferInfo: TransferInfo,
        onlineServerCount: Int,
        key: String,
    ): List<RealtimeSpeedPoint> {
        scopeKey = key
        restored = true
        val nextPoint = RealtimeSpeedPoint(
            timestamp = System.currentTimeMillis(),
            uploadSpeed = transferInfo.uploadSpeed.coerceAtLeast(0L),
            downloadSpeed = transferInfo.downloadSpeed.coerceAtLeast(0L),
            onlineServerCount = onlineServerCount.coerceAtLeast(0),
        )
        val lastPoint = series.lastOrNull()
        if (
            lastPoint != null &&
            nextPoint.timestamp - lastPoint.timestamp < HOME_REALTIME_SPEED_MIN_SAMPLE_INTERVAL_MS
        ) {
            series[series.lastIndex] = nextPoint
        } else {
            series += nextPoint
        }
        while (series.size > HOME_REALTIME_SPEED_MAX_POINTS) {
            series.removeAt(0)
        }
        persistLocked(key, series.toList())
        return series.toList()
    }

    suspend fun clearLocked(key: String) {
        scopeKey = key
        restored = true
        series.clear()
        persistLocked(key, emptyList())
    }

    suspend fun resetLocked(clearPersisted: Boolean) {
        val previousKey = scopeKey
        series.clear()
        restored = false
        scopeKey = null
        if (clearPersisted && !previousKey.isNullOrBlank()) {
            persistLocked(previousKey, emptyList())
        }
    }

    suspend fun ensureLoadedLocked(key: String) {
        if (scopeKey != key) {
            scopeKey = key
            restored = false
            series.clear()
        }
        if (restored) return

        val snapshot = connectionStore.loadHomeAggregateSpeedHistorySnapshot(key)
        val restoredPoints = restoreHomeRealtimeSpeedSeriesForScope(
            snapshot = snapshot,
            scopeKey = key,
            maxPoints = HOME_REALTIME_SPEED_MAX_POINTS,
        )
        series.clear()
        series.addAll(restoredPoints)
        restored = true
    }

    fun resolveScopeKey(snapshots: List<CachedDashboardServerSnapshot>, fallbackKey: String): String {
        return buildHomeRealtimeSpeedScopeKey(
            profileIds = snapshots.map { it.profileId },
            fallbackScopeKey = fallbackKey,
        )
    }

    suspend fun withLock(block: suspend () -> Unit) {
        mutex.withLock { block() }
    }

    suspend fun <T> withLockReturning(block: suspend () -> T): T {
        return mutex.withLock { block() }
    }

    fun currentScopeKey(): String? = scopeKey

    private suspend fun persistLocked(key: String, pointsSnapshot: List<RealtimeSpeedPoint>) {
        connectionStore.saveHomeAggregateSpeedHistorySnapshot(
            scopeKey = key,
            snapshot = HomeAggregateSpeedHistorySnapshot(
                scopeKey = key,
                points = pointsSnapshot.map { point ->
                    HomeSpeedHistoryPoint(
                        timestamp = point.timestamp,
                        uploadSpeed = point.uploadSpeed,
                        downloadSpeed = point.downloadSpeed,
                        onlineServerCount = point.onlineServerCount,
                    )
                },
            ),
        )
    }
}
