package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile

internal data class ServerDashboardDisplayInput(
    val selectedDashboardProfileId: String?,
    val activeServerProfileId: String?,
    val serverProfiles: List<ServerProfile>,
    val settingsBackendType: ServerBackendType,
    val dashboardServerSnapshots: List<CachedDashboardServerSnapshot>,
    val serverDashboardPreferences: Map<String, ServerDashboardPreferences>,
)

internal fun buildServerDashboardDisplayInput(
    state: MainUiState,
): ServerDashboardDisplayInput {
    return ServerDashboardDisplayInput(
        selectedDashboardProfileId = state.selectedDashboardProfileId,
        activeServerProfileId = state.activeServerProfileId,
        serverProfiles = state.serverProfiles,
        settingsBackendType = state.settings.serverBackendType,
        dashboardServerSnapshots = state.dashboardServerSnapshots,
        serverDashboardPreferences = state.serverDashboardPreferences,
    )
}

internal fun buildServerDashboardDisplayState(
    input: ServerDashboardDisplayInput,
): ServerDashboardDisplayState {
    val selectedDashboardProfileId = input.selectedDashboardProfileId
        ?: input.activeServerProfileId
        ?: input.serverProfiles.firstOrNull()?.id
    val selectedServerProfile = input.serverProfiles.firstOrNull { it.id == selectedDashboardProfileId }
    val selectedSnapshot = input.dashboardServerSnapshots.firstOrNull { it.profileId == selectedDashboardProfileId }
    return buildServerDashboardDisplayState(
        snapshot = selectedSnapshot,
        backendType = selectedServerProfile?.backendType ?: input.settingsBackendType,
        preferences = selectedServerProfile?.id?.let(input.serverDashboardPreferences::get),
    )
}
