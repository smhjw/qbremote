package com.hjw.qbremote.ui

import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsCacheSupportTest {

    @Test
    fun pruneCachedProfileSettings_keepsOnlyAvailableProfiles() {
        val pruned = pruneCachedProfileSettings(
            cache = mapOf(
                "alpha" to ConnectionSettings(host = "alpha"),
                "beta" to ConnectionSettings(host = "beta"),
                "orphan" to ConnectionSettings(host = "orphan"),
            ),
            profiles = listOf(
                ServerProfile("beta", "Beta", ServerBackendType.QBITTORRENT, "beta", 8080, false, "admin", 5),
                ServerProfile("alpha", "Alpha", ServerBackendType.QBITTORRENT, "alpha", 8080, false, "admin", 5),
            ),
        )

        assertEquals(
            mapOf(
                "alpha" to ConnectionSettings(host = "alpha"),
                "beta" to ConnectionSettings(host = "beta"),
            ),
            pruned,
        )
    }

    @Test
    fun cacheProfileSettings_normalizesProfileIdAndUpsertsValue() {
        val updated = cacheProfileSettings(
            cache = mapOf("alpha" to ConnectionSettings(host = "old")),
            profileId = " alpha ",
            settings = ConnectionSettings(host = "new"),
        )

        assertEquals(
            mapOf("alpha" to ConnectionSettings(host = "new")),
            updated,
        )
    }

    @Test
    fun settingsBelongToProfile_rejectsSettingsFromAnotherServer() {
        val profile = ServerProfile(
            id = "tr",
            name = "Transmission",
            backendType = ServerBackendType.TRANSMISSION,
            host = "tr.example.com",
            port = 9091,
            useHttps = false,
            username = "tr-user",
            refreshSeconds = 15,
        )

        assertTrue(
            settingsBelongToProfile(
                profile = profile,
                settings = ConnectionSettings(
                    host = " tr.example.com ",
                    port = 9091,
                    useHttps = false,
                    username = "tr-user",
                    serverBackendType = ServerBackendType.TRANSMISSION,
                    refreshSeconds = 15,
                ),
            ),
        )
        assertFalse(
            settingsBelongToProfile(
                profile = profile,
                settings = ConnectionSettings(
                    host = "qb.example.com",
                    port = 8080,
                    useHttps = true,
                    username = "qb-user",
                    serverBackendType = ServerBackendType.QBITTORRENT,
                    refreshSeconds = 5,
                ),
            ),
        )
    }

    @Test
    fun resolveActiveOrCachedProfileSettings_prefersActiveUiSettingsOverStaleCache() {
        val profile = ServerProfile(
            id = "alpha",
            name = "Alpha",
            backendType = ServerBackendType.QBITTORRENT,
            host = "alpha.example.com",
            port = 8080,
            useHttps = false,
            username = "admin",
            refreshSeconds = 5,
        )
        val currentSettings = ConnectionSettings(
            host = "alpha.example.com",
            port = 8080,
            useHttps = false,
            username = "admin",
            serverBackendType = ServerBackendType.QBITTORRENT,
            refreshSeconds = 5,
            appTheme = AppTheme.CUSTOM,
            customBackgroundImagePath = "/files/theme/new.jpg",
            customBackgroundToneIsLight = true,
        )
        val cachedSettings = currentSettings.copy(
            appTheme = AppTheme.DARK,
            customBackgroundImagePath = "/files/theme/old.jpg",
            customBackgroundToneIsLight = false,
        )

        val resolved = resolveActiveOrCachedProfileSettings(
            profileId = "alpha",
            activeProfileId = "alpha",
            activeProfile = profile,
            currentSettings = currentSettings,
            cachedSettings = cachedSettings,
        )

        assertEquals(currentSettings, resolved)
    }

    @Test
    fun resolveActiveOrCachedProfileSettings_fallsBackToCacheForInactiveOrMismatchedState() {
        val profile = ServerProfile(
            id = "alpha",
            name = "Alpha",
            backendType = ServerBackendType.QBITTORRENT,
            host = "alpha.example.com",
            port = 8080,
            useHttps = false,
            username = "admin",
            refreshSeconds = 5,
        )
        val cachedSettings = ConnectionSettings(
            host = "alpha.example.com",
            port = 8080,
            useHttps = false,
            username = "admin",
            serverBackendType = ServerBackendType.QBITTORRENT,
            refreshSeconds = 5,
            appTheme = AppTheme.LIGHT,
        )
        val currentSettings = ConnectionSettings(
            host = "beta.example.com",
            port = 9091,
            useHttps = true,
            username = "other",
            serverBackendType = ServerBackendType.TRANSMISSION,
            refreshSeconds = 15,
            appTheme = AppTheme.CUSTOM,
            customBackgroundImagePath = "/files/theme/new.jpg",
        )

        val resolved = resolveActiveOrCachedProfileSettings(
            profileId = "alpha",
            activeProfileId = "alpha",
            activeProfile = profile,
            currentSettings = currentSettings,
            cachedSettings = cachedSettings,
        )

        assertEquals(cachedSettings, resolved)
    }
}
