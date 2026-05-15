package com.hjw.qbremote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.DailyCountryUploadStats
import com.hjw.qbremote.data.model.GlobalSpeedLimits
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo

// =============================================================================
// Page-level state selectors — 每个页面只收自己需要的 state slice，
// 避免速度/统计数据更新导致全屏重组
// =============================================================================

internal data class DashboardPageState(
    val connected: Boolean,
    val isManualRefreshing: Boolean,
    val hasDashboardSnapshot: Boolean,
    val startupRestoreComplete: Boolean,
    val showHomeAggregateDashboard: Boolean,
    val showServerStackHint: Boolean,
    val serverProfiles: List<ServerProfile>,
    val activeServerProfileId: String?,
    val serverVersion: String,
    val settings: ConnectionSettings,
    val activeCapabilities: ServerCapabilities,
    val transferInfo: TransferInfo,
    val torrents: List<TorrentInfo>,
    val dashboardAggregate: DashboardAggregateState,
    val dashboardServerSnapshots: List<com.hjw.qbremote.data.CachedDashboardServerSnapshot>,
    val showGlobalSpeedLimitDialog: Boolean,
    val globalSpeedLimits: GlobalSpeedLimits?,
    val globalSpeedLimitLoading: Boolean,
    val speedLimitTargetProfileId: String?,
    val errorMessage: String?,
    val refreshScene: RefreshScene,
)

internal data class TorrentListPageState(
    val connected: Boolean,
    val serverProfiles: List<ServerProfile>,
    val activeServerProfileId: String?,
    val settings: ConnectionSettings,
    val activeCapabilities: ServerCapabilities,
    val torrents: List<TorrentInfo>,
    val categoryOptions: List<String>,
    val tagOptions: List<String>,
    val errorMessage: String?,
    val pendingActionKeys: Set<String>,
)

internal data class TorrentDetailPageState(
    val connected: Boolean,
    val activeServerProfileId: String?,
    val detailHash: String,
    val detailLoading: Boolean,
    val detailProperties: TorrentProperties?,
    val detailFiles: List<TorrentFileInfo>,
    val detailTrackers: List<TorrentTracker>,
    val categoryOptions: List<String>,
    val tagOptions: List<String>,
    val deleteFilesDefault: Boolean,
    val deleteFilesWhenNoSeeders: Boolean,
    val errorMessage: String?,
    val activeCapabilities: ServerCapabilities,
)

internal data class SettingsPageState(
    val connected: Boolean,
    val isManualRefreshing: Boolean,
    val settings: ConnectionSettings,
    val serverProfiles: List<ServerProfile>,
    val activeServerProfileId: String?,
    val errorMessage: String?,
)

@Composable
internal fun rememberDashboardPageState(state: MainUiState): State<DashboardPageState> {
    return remember {
        derivedStateOf {
            DashboardPageState(
                connected = state.connected,
                isManualRefreshing = state.isManualRefreshing,
                hasDashboardSnapshot = state.hasDashboardSnapshot,
                startupRestoreComplete = state.startupRestoreComplete,
                showHomeAggregateDashboard = state.serverProfiles.size > 1,
                showServerStackHint = state.settings.homeTorrentEntryHintDismissed,
                serverProfiles = state.serverProfiles,
                activeServerProfileId = state.activeServerProfileId,
                serverVersion = state.serverVersion,
                settings = state.settings,
                activeCapabilities = state.activeCapabilities,
                transferInfo = state.transferInfo,
                torrents = state.torrents,
                dashboardAggregate = state.dashboardAggregate,
                dashboardServerSnapshots = state.dashboardServerSnapshots,
                showGlobalSpeedLimitDialog = state.showGlobalSpeedLimitDialog,
                globalSpeedLimits = state.globalSpeedLimits,
                globalSpeedLimitLoading = state.globalSpeedLimitLoading,
                speedLimitTargetProfileId = state.speedLimitTargetProfileId,
                errorMessage = state.errorMessage,
                refreshScene = state.refreshScene,
            )
        }
    }
}

@Composable
internal fun rememberTorrentListPageState(state: MainUiState): State<TorrentListPageState> {
    return remember {
        derivedStateOf {
            TorrentListPageState(
                connected = state.connected,
                serverProfiles = state.serverProfiles,
                activeServerProfileId = state.activeServerProfileId,
                settings = state.settings,
                activeCapabilities = state.activeCapabilities,
                torrents = state.torrents,
                categoryOptions = state.categoryOptions,
                tagOptions = state.tagOptions,
                errorMessage = state.errorMessage,
                pendingActionKeys = state.pendingActionKeys,
            )
        }
    }
}

@Composable
internal fun rememberTorrentDetailPageState(state: MainUiState): State<TorrentDetailPageState> {
    return remember {
        derivedStateOf {
            TorrentDetailPageState(
                connected = state.connected,
                activeServerProfileId = state.activeServerProfileId,
                detailHash = state.detailHash,
                detailLoading = state.detailLoading,
                detailProperties = state.detailProperties,
                detailFiles = state.detailFiles,
                detailTrackers = state.detailTrackers,
                categoryOptions = state.categoryOptions,
                tagOptions = state.tagOptions,
                deleteFilesDefault = state.settings.deleteFilesDefault,
                deleteFilesWhenNoSeeders = state.settings.deleteFilesWhenNoSeeders,
                errorMessage = state.errorMessage,
                activeCapabilities = state.activeCapabilities,
            )
        }
    }
}

@Composable
internal fun rememberSettingsPageState(state: MainUiState): State<SettingsPageState> {
    return remember {
        derivedStateOf {
            SettingsPageState(
                connected = state.connected,
                isManualRefreshing = state.isManualRefreshing,
                settings = state.settings,
                serverProfiles = state.serverProfiles,
                activeServerProfileId = state.activeServerProfileId,
                errorMessage = state.errorMessage,
            )
        }
    }
}
