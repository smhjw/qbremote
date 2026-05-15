package com.hjw.qbremote.ui

import androidx.compose.foundation.lazy.LazyListScope
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.ServerBackendType

internal fun LazyListScope.settingsRootPageContent(
    state: MainUiState,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
    onBackendTypeChange: (ServerBackendType) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onHttpsChange: (Boolean) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefreshSecondsChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    item {
        SettingsPageContent(
            settings = state.settings,
            onAppLanguageChange = onAppLanguageChange,
            onDeleteFilesWhenNoSeedersChange = onDeleteFilesWhenNoSeedersChange,
            onDeleteFilesDefaultChange = onDeleteFilesDefaultChange,
        )
    }
    if (state.serverProfiles.isEmpty()) {
        item {
            ConnectionCard(
                state = state,
                onBackendTypeChange = onBackendTypeChange,
                onHostChange = onHostChange,
                onPortChange = onPortChange,
                onHttpsChange = onHttpsChange,
                onUserChange = onUserChange,
                onPasswordChange = onPasswordChange,
                onRefreshSecondsChange = onRefreshSecondsChange,
                onConnect = onConnect,
            )
        }
    }
}
