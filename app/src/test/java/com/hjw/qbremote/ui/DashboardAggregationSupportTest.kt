package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardAggregationSupportTest {

    @Test
    fun buildDashboardAggregateFromSnapshots_allSnapshotsStale_zeroesTransferSpeed() {
        val snapshots = listOf(
            CachedDashboardServerSnapshot(
                profileId = "a",
                backendType = ServerBackendType.QBITTORRENT,
                transferInfo = TransferInfo(uploadSpeed = 120L, downloadSpeed = 340L),
                isStale = true,
            ),
            CachedDashboardServerSnapshot(
                profileId = "b",
                backendType = ServerBackendType.QBITTORRENT,
                transferInfo = TransferInfo(uploadSpeed = 80L, downloadSpeed = 160L),
                isStale = true,
            ),
        )

        val aggregate = buildDashboardAggregateFromSnapshots(snapshots)

        assertEquals(0L, aggregate.transferInfo.uploadSpeed)
        assertEquals(0L, aggregate.transferInfo.downloadSpeed)
    }

    @Test
    fun buildDashboardAggregateFromSnapshots_liveSnapshotStillContributesTransferSpeed() {
        val snapshots = listOf(
            CachedDashboardServerSnapshot(
                profileId = "stale",
                backendType = ServerBackendType.QBITTORRENT,
                transferInfo = TransferInfo(uploadSpeed = 999L, downloadSpeed = 999L),
                isStale = true,
            ),
            CachedDashboardServerSnapshot(
                profileId = "live",
                backendType = ServerBackendType.QBITTORRENT,
                transferInfo = TransferInfo(uploadSpeed = 12L, downloadSpeed = 34L),
                isStale = false,
            ),
        )

        val aggregate = buildDashboardAggregateFromSnapshots(snapshots)

        assertEquals(12L, aggregate.transferInfo.uploadSpeed)
        assertEquals(34L, aggregate.transferInfo.downloadSpeed)
    }
}
