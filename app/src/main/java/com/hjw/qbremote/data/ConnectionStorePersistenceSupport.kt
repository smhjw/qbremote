package com.hjw.qbremote.data

import com.hjw.qbremote.data.model.TorrentInfo

internal const val MAX_PERSISTED_DASHBOARD_TORRENTS = 400
internal const val MAX_PERSISTED_DASHBOARD_JSON_CHARS = 1_000_000
internal const val MAX_FETCH_DASHBOARD_TORRENTS = 4_000

internal fun <T> upsertNormalizedEntryIfChanged(
    entries: Map<String, T>,
    rawKey: String,
    value: T,
): Map<String, T>? {
    val normalizedKey = rawKey.trim()
    if (normalizedKey.isBlank()) return null
    if (entries[normalizedKey] == value) return null
    return entries.toMutableMap().apply {
        this[normalizedKey] = value
    }
}

internal fun normalizeHomeAggregateSpeedHistoryForPersistence(
    scopeKey: String,
    snapshot: HomeAggregateSpeedHistorySnapshot,
): HomeAggregateSpeedHistorySnapshot? {
    val normalizedScopeKey = scopeKey.trim()
    if (normalizedScopeKey.isBlank()) return null
    val normalizedPoints = snapshot.points
        .map { point ->
            point.copy(
                timestamp = point.timestamp.coerceAtLeast(0L),
                uploadSpeed = point.uploadSpeed.coerceAtLeast(0L),
                downloadSpeed = point.downloadSpeed.coerceAtLeast(0L),
                onlineServerCount = point.onlineServerCount.coerceAtLeast(0),
            )
        }
        .sortedBy { it.timestamp }
    if (normalizedPoints.isEmpty()) return null
    return HomeAggregateSpeedHistorySnapshot(
        scopeKey = normalizedScopeKey,
        points = normalizedPoints,
    )
}

internal fun sanitizeDashboardCacheForPersistence(
    snapshot: DashboardCacheSnapshot,
): DashboardCacheSnapshot {
    return snapshot.copy(
        torrents = sanitizeDashboardTorrentsForPersistence(snapshot.torrents),
    )
}

internal fun sanitizeDashboardServerSnapshotForPersistence(
    snapshot: CachedDashboardServerSnapshot,
): CachedDashboardServerSnapshot {
    return snapshot.copy(
        torrents = sanitizeDashboardTorrentsForPersistence(snapshot.torrents),
    )
}

internal fun isOversizedDashboardPersistencePayload(raw: String?): Boolean {
    return raw?.length?.let { length -> length > MAX_PERSISTED_DASHBOARD_JSON_CHARS } ?: false
}

private fun sanitizeDashboardTorrentsForPersistence(
    torrents: List<TorrentInfo>,
): List<TorrentInfo> {
    return if (torrents.size > MAX_PERSISTED_DASHBOARD_TORRENTS) {
        emptyList()
    } else {
        torrents
    }
}
