package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.DashboardCacheSnapshot
import com.hjw.qbremote.data.model.TransferInfo

internal fun applyDashboardCacheHydration(
    current: MainUiState,
    cache: DashboardCacheSnapshot?,
): MainUiState {
    return if (cache == null) {
        current.copy(
            serverVersion = "-",
            transferInfo = TransferInfo(),
            torrents = emptyList(),
            dailyTagUploadDate = "",
            dailyTagUploadStats = emptyList(),
            dailyCountryUploadDate = "",
            dailyCountryUploadStats = emptyList(),
            categoryOptions = emptyList(),
            tagOptions = emptyList(),
            dashboardCacheHydrated = true,
            hasDashboardSnapshot = false,
        )
    } else {
        current.copy(
            // useAltSpeedLimits 是实时易变状态，不应从持久化缓存恢复
            transferInfo = cache.transferInfo.copy(useAltSpeedLimits = false),
            torrents = cache.torrents,
            dailyTagUploadDate = cache.dailyTagUploadDate,
            dailyTagUploadStats = cache.dailyTagUploadStats.map { stat -> stat.toDailyTagUploadStat() },
            dailyCountryUploadDate = cache.dailyCountryUploadDate,
            dailyCountryUploadStats = cache.dailyCountryUploadStats,
            dashboardCacheHydrated = true,
            hasDashboardSnapshot = true,
        )
    }
}

private fun CachedDailyTagUploadStat.toDailyTagUploadStat(): DailyTagUploadStat {
    return DailyTagUploadStat(
        tag = tag,
        uploadedBytes = uploadedBytes,
        torrentCount = torrentCount,
        isNoTag = isNoTag,
    )
}
