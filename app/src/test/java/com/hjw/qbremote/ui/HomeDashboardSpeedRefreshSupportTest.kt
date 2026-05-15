package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HomeDashboardSpeedRefreshSupportTest {

    @Test
    fun resolveHomeSpeedRefreshIntervalSeconds_usesThreeSecondsOnlyOnDashboard() {
        assertEquals(3, resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.DASHBOARD))
        assertNull(resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.SERVER))
        assertNull(resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.TORRENT_DETAIL))
        assertNull(resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.SETTINGS))
    }

    @Test
    fun countryTrackerSampleInterval_isNotMoreFrequentThanFiveSeconds() {
        assertEquals(5_000L, COUNTRY_TRACKER_SAMPLE_INTERVAL_MS)
    }

    @Test
    fun mergeTransferInfoIntoDashboardSnapshots_updatesOnlyTransferFields() {
        val original = listOf(
            CachedDashboardServerSnapshot(
                profileId = "a",
                profileName = "Alpha",
                backendType = ServerBackendType.QBITTORRENT,
                host = "alpha.local",
                port = 8080,
                useHttps = true,
                serverVersion = "1.0",
                transferInfo = TransferInfo(uploadSpeed = 1L, downloadSpeed = 2L, uploadedTotal = 3L),
                lastUpdatedAt = 1_000L,
                errorMessage = "stale",
                isStale = true,
            ),
            CachedDashboardServerSnapshot(
                profileId = "b",
                profileName = "Beta",
                backendType = ServerBackendType.TRANSMISSION,
                host = "beta.local",
                port = 9091,
                useHttps = false,
                serverVersion = "2.0",
                transferInfo = TransferInfo(uploadSpeed = 3L, downloadSpeed = 4L, uploadedTotal = 5L),
                lastUpdatedAt = 2_000L,
            ),
        )

        val merged = mergeTransferInfoIntoDashboardSnapshots(
            snapshots = original,
            transferInfoByProfileId = mapOf(
                "a" to TransferInfo(uploadSpeed = 9L, downloadSpeed = 10L, uploadedTotal = 11L),
            ),
        )

        val updatedAlpha = merged.first { it.profileId == "a" }
        val untouchedBeta = merged.first { it.profileId == "b" }

        assertEquals(listOf("a", "b"), merged.map { it.profileId })
        assertEquals(9L, updatedAlpha.transferInfo.uploadSpeed)
        assertEquals(10L, updatedAlpha.transferInfo.downloadSpeed)
        assertEquals(11L, updatedAlpha.transferInfo.uploadedTotal)
        assertEquals(original[0], updatedAlpha.copy(transferInfo = original[0].transferInfo))

        assertEquals(original[1], untouchedBeta)
    }

    @Test
    fun buildHomeChartTransferInfo_sumsSuccessfulProfileTransferInfoOnly() {
        val aggregate = buildHomeChartTransferInfo(
            transferInfos = listOf(
                TransferInfo(uploadSpeed = 5L, downloadSpeed = 7L),
                TransferInfo(uploadSpeed = 11L, downloadSpeed = 13L),
            ),
        )

        assertEquals(16L, aggregate.uploadSpeed)
        assertEquals(20L, aggregate.downloadSpeed)
    }

    @Test
    fun applyHomeChartRefreshToAggregate_preservesSummaryTransferInfo() {
        val summaryTransferInfo = TransferInfo(uploadSpeed = 100L, downloadSpeed = 200L)
        val chartTransferInfo = TransferInfo(uploadSpeed = 30L, downloadSpeed = 40L)
        val chartSeries = listOf(
            RealtimeSpeedPoint(timestamp = 1L, uploadSpeed = 30L, downloadSpeed = 40L, onlineServerCount = 1),
        )

        val updated = applyHomeChartRefreshToAggregate(
            aggregate = DashboardAggregateState(
                transferInfo = summaryTransferInfo,
                realtimeSpeedSeries = emptyList(),
            ),
            chartTransferInfo = chartTransferInfo,
            chartSeries = chartSeries,
        )

        assertSame(summaryTransferInfo, updated.transferInfo)
        assertSame(chartTransferInfo, updated.chartTransferInfo)
        assertEquals(chartSeries, updated.realtimeSpeedSeries)
    }
}
