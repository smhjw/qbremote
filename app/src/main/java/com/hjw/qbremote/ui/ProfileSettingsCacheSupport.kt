package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerProfile

internal fun pruneCachedProfileSettings(
    cache: Map<String, ConnectionSettings>,
    profiles: List<ServerProfile>,
): Map<String, ConnectionSettings> {
    if (cache.isEmpty()) return emptyMap()
    val availableIds = profiles
        .map { profile -> profile.id.trim() }
        .filter { it.isNotBlank() }
        .toSet()
    if (availableIds.isEmpty()) return emptyMap()
    return cache.filterKeys { profileId -> profileId in availableIds }
}

internal fun cacheProfileSettings(
    cache: Map<String, ConnectionSettings>,
    profileId: String,
    settings: ConnectionSettings,
): Map<String, ConnectionSettings> {
    val normalizedProfileId = profileId.trim()
    if (normalizedProfileId.isBlank()) return cache
    if (cache[normalizedProfileId] == settings) return cache
    return cache.toMutableMap().apply {
        this[normalizedProfileId] = settings
    }
}

internal fun resolveActiveOrCachedProfileSettings(
    profileId: String,
    activeProfileId: String?,
    activeProfile: ServerProfile?,
    currentSettings: ConnectionSettings,
    cachedSettings: ConnectionSettings?,
): ConnectionSettings? {
    val normalizedProfileId = profileId.trim()
    if (normalizedProfileId.isBlank()) return null
    val activeSettings = currentSettings.takeIf {
        activeProfileId == normalizedProfileId &&
            activeProfile != null &&
            settingsBelongToProfile(activeProfile, currentSettings)
    }
    return activeSettings ?: cachedSettings
}

internal fun settingsBelongToProfile(
    profile: ServerProfile,
    settings: ConnectionSettings,
): Boolean {
    return profile.host.trim().equals(settings.host.trim(), ignoreCase = true) &&
        profile.port == settings.port &&
        profile.useHttps == settings.useHttps &&
        profile.username.trim() == settings.username.trim() &&
        profile.backendType == settings.serverBackendType &&
        profile.refreshSeconds == settings.refreshSeconds
}
