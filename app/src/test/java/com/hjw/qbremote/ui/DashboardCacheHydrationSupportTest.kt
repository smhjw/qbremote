package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.DashboardCacheSnapshot
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardCacheHydrationSupportTest {

    @Test
    fun applyDashboardCacheHydration_clearsSelectedServerFieldsWhenCacheMissing() {
        val current = MainUiState(
            settings = ConnectionSettings(host = "old.example.com"),
            activeServerProfileId = "alpha",
            selectedDashboardProfileId = "beta",
            connected = false,
            transferInfo = TransferInfo(uploadSpeed = 12L, downloadSpeed = 34L),
            torrents = listOf(TorrentInfo(hash = "abc", name = "Old torrent")),
            dailyTagUploadDate = "2026-04-02",
            dailyTagUploadStats = listOf(DailyTagUploadStat(tag = "movies", uploadedBytes = 4096L, torrentCount = 1)),
            dailyCountryUploadDate = "2026-04-02",
            dailyCountryUploadStats = listOf(
                CountryUploadRecord(countryCode = "US", countryName = "United States", uploadedBytes = 2048L),
            ),
            categoryOptions = listOf("movies"),
            tagOptions = listOf("4k"),
            dashboardCacheHydrated = false,
            hasDashboardSnapshot = true,
        )

        val updated = applyDashboardCacheHydration(
            current = current,
            cache = null,
        )

        assertEquals(0L, updated.transferInfo.uploadSpeed)
        assertTrue(updated.torrents.isEmpty())
        assertTrue(updated.dailyTagUploadStats.isEmpty())
        assertTrue(updated.dailyCountryUploadStats.isEmpty())
        assertTrue(updated.categoryOptions.isEmpty())
        assertTrue(updated.tagOptions.isEmpty())
        assertTrue(updated.dashboardCacheHydrated)
        assertFalse(updated.hasDashboardSnapshot)
    }

    @Test
    fun applyDashboardCacheHydration_restoresCachedServerSnapshot() {
        val updated = applyDashboardCacheHydration(
            current = MainUiState(),
            cache = DashboardCacheSnapshot(
                transferInfo = TransferInfo(uploadSpeed = 88L, downloadSpeed = 44L),
                torrents = listOf(TorrentInfo(hash = "hash-1", name = "Cached torrent")),
                dailyTagUploadDate = "2026-04-02",
                dailyTagUploadStats = listOf(
                    CachedDailyTagUploadStat(
                        tag = "linux",
                        uploadedBytes = 8192L,
                        torrentCount = 2,
                        isNoTag = false,
                    ),
                ),
                dailyCountryUploadDate = "2026-04-02",
                dailyCountryUploadStats = listOf(
                    CountryUploadRecord(countryCode = "JP", countryName = "Japan", uploadedBytes = 4096L),
                ),
            ),
        )

        assertEquals(88L, updated.transferInfo.uploadSpeed)
        assertEquals(1, updated.torrents.size)
        assertEquals("2026-04-02", updated.dailyTagUploadDate)
        assertEquals(1, updated.dailyTagUploadStats.size)
        assertEquals("linux", updated.dailyTagUploadStats.first().tag)
        assertEquals("2026-04-02", updated.dailyCountryUploadDate)
        assertEquals("JP", updated.dailyCountryUploadStats.first().countryCode)
        assertTrue(updated.dashboardCacheHydrated)
        assertTrue(updated.hasDashboardSnapshot)
    }
}
