package com.hjw.qbremote.data

import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.ui.RealtimeSpeedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStorePersistenceSupportTest {

    @Test
    fun upsertNormalizedEntryIfChanged_returnsNullWhenEntryIsUnchanged() {
        val snapshot = DashboardCacheSnapshot(
            dailyTagUploadDate = "2026-04-01",
        )

        val updated = upsertNormalizedEntryIfChanged(
            entries = mapOf("scope-a" to snapshot),
            rawKey = " scope-a ",
            value = snapshot,
        )

        assertNull(updated)
    }

    @Test
    fun upsertNormalizedEntryIfChanged_updatesNormalizedKeyWhenEntryChanges() {
        val updated = upsertNormalizedEntryIfChanged(
            entries = emptyMap(),
            rawKey = " scope-a ",
            value = DashboardCacheSnapshot(
                dailyTagUploadDate = "2026-04-01",
            ),
        )

        assertEquals(
            mapOf(
                "scope-a" to DashboardCacheSnapshot(
                    dailyTagUploadDate = "2026-04-01",
                ),
            ),
            updated,
        )
    }

    @Test
    fun normalizeHomeAggregateSpeedHistoryForPersistence_returnsNullForBlankScopeOrEmptyPoints() {
        assertNull(
            normalizeHomeAggregateSpeedHistoryForPersistence(
                scopeKey = " ",
                snapshot = HomeAggregateSpeedHistorySnapshot(
                    points = listOf(
                        HomeSpeedHistoryPoint(timestamp = 1L),
                    ),
                ),
            ),
        )
        assertNull(
            normalizeHomeAggregateSpeedHistoryForPersistence(
                scopeKey = "scope-a",
                snapshot = HomeAggregateSpeedHistorySnapshot(),
            ),
        )
    }

    @Test
    fun normalizeHomeAggregateSpeedHistoryForPersistence_keepsNormalizedScopeAndSortedPoints() {
        val normalized = normalizeHomeAggregateSpeedHistoryForPersistence(
            scopeKey = " scope-a ",
            snapshot = HomeAggregateSpeedHistorySnapshot(
                points = listOf(
                    HomeSpeedHistoryPoint(timestamp = 8L, uploadSpeed = 3L),
                    HomeSpeedHistoryPoint(timestamp = 2L, uploadSpeed = 1L),
                ),
            ),
        )

        assertEquals(
            HomeAggregateSpeedHistorySnapshot(
                scopeKey = "scope-a",
                points = listOf(
                    HomeSpeedHistoryPoint(timestamp = 2L, uploadSpeed = 1L),
                    HomeSpeedHistoryPoint(timestamp = 8L, uploadSpeed = 3L),
                ),
            ),
            normalized,
        )
    }

    @Test
    fun sanitizeDashboardCacheForPersistence_dropsOversizedTorrentLists() {
        val torrents = (0..MAX_PERSISTED_DASHBOARD_TORRENTS).map { index ->
            TorrentInfo(hash = "hash-$index", name = "Torrent $index")
        }

        val sanitized = sanitizeDashboardCacheForPersistence(
            DashboardCacheSnapshot(torrents = torrents),
        )

        assertTrue(sanitized.torrents.isEmpty())
    }

    @Test
    fun sanitizeDashboardServerSnapshotForPersistence_keepsSmallTorrentLists() {
        val torrents = listOf(
            TorrentInfo(hash = "hash-1", name = "Torrent 1"),
            TorrentInfo(hash = "hash-2", name = "Torrent 2"),
        )

        val sanitized = sanitizeDashboardServerSnapshotForPersistence(
            CachedDashboardServerSnapshot(
                profileId = "alpha",
                torrents = torrents,
            ),
        )

        assertSame(torrents, sanitized.torrents)
    }

    @Test
    fun isOversizedDashboardPersistencePayload_flagsLargeRawJson() {
        assertEquals(false, isOversizedDashboardPersistencePayload("x".repeat(MAX_PERSISTED_DASHBOARD_JSON_CHARS)))
        assertEquals(true, isOversizedDashboardPersistencePayload("x".repeat(MAX_PERSISTED_DASHBOARD_JSON_CHARS + 1)))
    }
}
