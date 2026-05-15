package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.BackendConnectionError
import androidx.compose.runtime.Immutable
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot
import com.hjw.qbremote.data.DailyUploadTrackingSnapshot
import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.TorrentRepository
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.GlobalSpeedLimits
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.Job

data class MainUiState(
    val settings: ConnectionSettings = ConnectionSettings(),
    val serverProfiles: List<ServerProfile> = emptyList(),
    val activeServerProfileId: String? = null,
    val activeCapabilities: ServerCapabilities = defaultCapabilitiesFor(ServerBackendType.QBITTORRENT),
    val aggregateOnlineServerCount: Int = 0,
    val isConnecting: Boolean = false,
    val isManualRefreshing: Boolean = false,
    val connected: Boolean = false,
    val serverVersion: String = "-",
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val detailHash: String = "",
    val detailLoading: Boolean = false,
    val detailProperties: TorrentProperties? = null,
    val detailFiles: List<TorrentFileInfo> = emptyList(),
    val detailTrackers: List<TorrentTracker> = emptyList(),
    val categoryOptions: List<String> = emptyList(),
    val tagOptions: List<String> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<DailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryUploadRecord> = emptyList(),
    val dashboardServerSnapshots: List<CachedDashboardServerSnapshot> = emptyList(),
    val serverDashboardPreferences: Map<String, ServerDashboardPreferences> = emptyMap(),
    val selectedDashboardProfileId: String? = null,
    val dashboardSessionToken: Long = 0L,
    val dashboardRefreshHoldProfileId: String? = null,
    val dashboardAggregate: DashboardAggregateState = DashboardAggregateState(),
    val dashboardCacheHydrated: Boolean = false,
    val hasDashboardSnapshot: Boolean = false,
    val startupRestoreComplete: Boolean = false,
    val refreshScene: RefreshScene = RefreshScene.DASHBOARD,
    val pendingActionKeys: Set<String> = emptySet(),
    val pendingBackendRepair: PendingBackendRepair? = null,
    val sharedMagnetUrl: String = "",
    val errorMessage: String? = null,
    val showGlobalSpeedLimitDialog: Boolean = false,
    val globalSpeedLimits: GlobalSpeedLimits? = null,
    val globalSpeedLimitLoading: Boolean = false,
    val speedLimitTargetProfileId: String? = null,
)

internal data class DashboardReorderHoldReleaseResult(
    val nextHeldProfileId: String? = null,
    val profileIdToRefreshImmediately: String? = null,
)

internal fun shouldSkipRefreshForDashboardReorderHold(
    heldProfileId: String?,
    profileId: String,
): Boolean {
    val normalizedHeldProfileId = heldProfileId?.trim().orEmpty()
    val normalizedProfileId = profileId.trim()
    return normalizedHeldProfileId.isNotBlank() &&
        normalizedProfileId.isNotBlank() &&
        normalizedHeldProfileId == normalizedProfileId
}

internal fun releaseDashboardReorderHold(
    state: MainUiState,
): DashboardReorderHoldReleaseResult {
    val heldProfileId = state.dashboardRefreshHoldProfileId?.trim().orEmpty()
    if (heldProfileId.isBlank()) return DashboardReorderHoldReleaseResult()
    return DashboardReorderHoldReleaseResult(
        nextHeldProfileId = null,
        profileIdToRefreshImmediately = heldProfileId,
    )
}

internal sealed interface DashboardSnapshotRefreshResult {
    val profile: ServerProfile
    val previousSnapshot: CachedDashboardServerSnapshot?

    data class Fresh(
        override val profile: ServerProfile,
        val settings: ConnectionSettings,
        val fetched: com.hjw.qbremote.data.DashboardSnapshotFetchResult,
        override val previousSnapshot: CachedDashboardServerSnapshot?,
    ) : DashboardSnapshotRefreshResult

    data class Failure(
        override val profile: ServerProfile,
        val error: Throwable,
        override val previousSnapshot: CachedDashboardServerSnapshot?,
    ) : DashboardSnapshotRefreshResult
}

internal data class DashboardStatsRefreshInput(
    val profile: ServerProfile,
    val settings: ConnectionSettings,
    val torrents: List<TorrentInfo>,
    val baseSnapshot: CachedDashboardServerSnapshot,
)

internal fun buildPendingActionKey(
    profileId: String,
    hash: String,
): String {
    return "${profileId.trim()}|${hash.trim()}"
}

internal fun shouldApplyActiveProfileAsyncResult(
    requestedProfileId: String,
    requestVersion: Long,
    activeProfileId: String?,
    activeRequestVersion: Long,
): Boolean {
    val normalizedProfileId = requestedProfileId.trim()
    return normalizedProfileId.isNotBlank() &&
        activeProfileId == normalizedProfileId &&
        activeRequestVersion == requestVersion
}

internal fun buildDailyUploadTrackingScopeKey(
    activeProfileId: String?,
    settings: ConnectionSettings,
): String {
    val normalizedProfileId = activeProfileId.orEmpty().trim()
    if (normalizedProfileId.isNotBlank()) {
        return "profile:$normalizedProfileId"
    }

    val host = settings.host.trim().lowercase()
    return if (host.isNotBlank()) {
        "server:${settings.useHttps}|$host|${settings.port}"
    } else {
        "default"
    }
}

internal fun normalizeProfileIdsForRefresh(
    profiles: List<ServerProfile>,
): List<String> {
    return profiles
        .map { profile -> profile.id.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

internal fun resolvePreferredProfileId(
    availableIds: List<String>,
    primaryCandidate: String?,
    secondaryCandidate: String?,
): String? {
    if (availableIds.isEmpty()) return null
    val availableIdSet = availableIds.toHashSet()
    return primaryCandidate?.takeIf { it in availableIdSet }
        ?: secondaryCandidate?.takeIf { it in availableIdSet }
        ?: availableIds.firstOrNull()
}

internal fun filterDashboardPreferencesForProfiles(
    preferences: Map<String, ServerDashboardPreferences>,
    profiles: List<ServerProfile>,
): Map<String, ServerDashboardPreferences> {
    if (preferences.isEmpty() || profiles.isEmpty()) return emptyMap()
    val profileIdSet = profiles.mapTo(mutableSetOf()) { profile -> profile.id }
    return preferences.filterKeys { profileId -> profileId in profileIdSet }
}

internal fun resolveSelectedDashboardProfileId(
    activeProfileId: String?,
    selectedDashboardProfileId: String?,
    snapshots: List<CachedDashboardServerSnapshot>,
): String? {
    return resolvePreferredProfileId(
        availableIds = snapshots.map { snapshot -> snapshot.profileId },
        primaryCandidate = activeProfileId,
        secondaryCandidate = selectedDashboardProfileId,
    )
}

internal fun applyDashboardSnapshotsToState(
    current: MainUiState,
    orderedSnapshots: List<CachedDashboardServerSnapshot>,
    aggregate: DashboardAggregateState,
): MainUiState {
    return current.copy(
        dashboardServerSnapshots = orderedSnapshots,
        selectedDashboardProfileId = resolveSelectedDashboardProfileId(
            activeProfileId = current.activeServerProfileId,
            selectedDashboardProfileId = current.selectedDashboardProfileId,
            snapshots = orderedSnapshots,
        ),
        dashboardAggregate = aggregate.copy(
            chartTransferInfo = current.dashboardAggregate.chartTransferInfo,
        ),
        aggregateOnlineServerCount = orderedSnapshots.count { !it.isStale },
    )
}

internal fun restoreHomeRealtimeSpeedSeries(
    snapshot: HomeAggregateSpeedHistorySnapshot,
    maxPoints: Int,
): List<RealtimeSpeedPoint> {
    if (maxPoints <= 0) return emptyList()
    return snapshot.points
        .map { point ->
            RealtimeSpeedPoint(
                timestamp = point.timestamp.coerceAtLeast(0L),
                uploadSpeed = point.uploadSpeed.coerceAtLeast(0L),
                downloadSpeed = point.downloadSpeed.coerceAtLeast(0L),
                onlineServerCount = point.onlineServerCount.coerceAtLeast(0),
            )
        }
        .sortedBy { point -> point.timestamp }
        .toList()
        .takeLast(maxPoints)
}

internal fun buildHomeRealtimeSpeedScopeKey(
    profileIds: List<String>,
    fallbackScopeKey: String,
): String {
    val profileSetKey = profileIds
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .joinToString(",")
    return if (profileSetKey.isNotBlank()) {
        "profiles:$profileSetKey"
    } else {
        "fallback:${fallbackScopeKey.trim()}"
    }
}

internal fun restoreHomeRealtimeSpeedSeriesForScope(
    snapshot: HomeAggregateSpeedHistorySnapshot?,
    scopeKey: String,
    maxPoints: Int,
): List<RealtimeSpeedPoint> {
    if (snapshot == null) return emptyList()
    val normalizedScopeKey = scopeKey.trim()
    if (normalizedScopeKey.isBlank() || snapshot.scopeKey != normalizedScopeKey) return emptyList()
    return restoreHomeRealtimeSpeedSeries(snapshot, maxPoints)
}

internal fun resolveHomeSpeedRefreshIntervalSeconds(scene: RefreshScene): Int? {
    return if (scene == RefreshScene.DASHBOARD) 3 else null
}

internal fun buildHomeChartTransferInfo(
    transferInfos: Collection<TransferInfo>,
): TransferInfo {
    return transferInfos.fold(TransferInfo()) { acc, transferInfo ->
        TransferInfo(
            downloadSpeed = acc.downloadSpeed + transferInfo.downloadSpeed,
            uploadSpeed = acc.uploadSpeed + transferInfo.uploadSpeed,
            downloadedTotal = acc.downloadedTotal + transferInfo.downloadedTotal,
            uploadedTotal = acc.uploadedTotal + transferInfo.uploadedTotal,
            downloadRateLimit = acc.downloadRateLimit + transferInfo.downloadRateLimit,
            uploadRateLimit = acc.uploadRateLimit + transferInfo.uploadRateLimit,
            freeSpaceOnDisk = acc.freeSpaceOnDisk + transferInfo.freeSpaceOnDisk,
            dhtNodes = acc.dhtNodes + transferInfo.dhtNodes,
        )
    }
}

internal fun applyHomeChartRefreshToAggregate(
    aggregate: DashboardAggregateState,
    chartTransferInfo: TransferInfo,
    chartSeries: List<RealtimeSpeedPoint>,
): DashboardAggregateState {
    return aggregate.copy(
        chartTransferInfo = chartTransferInfo,
        realtimeSpeedSeries = chartSeries,
    )
}

internal fun prepareConnectingProfileState(
    current: MainUiState,
    settings: ConnectionSettings,
    profileId: String,
    capabilities: ServerCapabilities,
): MainUiState {
    return current.copy(
        settings = settings,
        activeServerProfileId = profileId,
        selectedDashboardProfileId = profileId,
        dashboardSessionToken = current.dashboardSessionToken + 1L,
        activeCapabilities = capabilities,
        isConnecting = true,
        connected = false,
        pendingBackendRepair = null,
        errorMessage = null,
        serverVersion = "-",
        transferInfo = TransferInfo(),
        torrents = emptyList(),
        dailyTagUploadDate = "",
        dailyTagUploadStats = emptyList(),
        dailyCountryUploadDate = "",
        dailyCountryUploadStats = emptyList(),
        categoryOptions = emptyList(),
        tagOptions = emptyList(),
        detailHash = "",
        detailLoading = false,
        detailProperties = null,
        detailFiles = emptyList(),
        detailTrackers = emptyList(),
        pendingActionKeys = emptySet(),
    )
}

