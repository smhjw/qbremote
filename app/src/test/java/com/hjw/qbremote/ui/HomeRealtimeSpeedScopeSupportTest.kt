package com.hjw.qbremote.ui

import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRealtimeSpeedScopeSupportTest {

    @Test
    fun buildHomeRealtimeSpeedScopeKey_stabilizesProfileSetOrder() {
        val scopeA = buildHomeRealtimeSpeedScopeKey(
            profileIds = listOf("beta", "alpha", "alpha", " gamma "),
            fallbackScopeKey = "profile:active",
        )
        val scopeB = buildHomeRealtimeSpeedScopeKey(
            profileIds = listOf("gamma", "alpha", "beta"),
            fallbackScopeKey = "profile:active",
        )

        assertEquals("profiles:alpha,beta,gamma", scopeA)
        assertEquals(scopeA, scopeB)
    }

    @Test
    fun buildHomeRealtimeSpeedScopeKey_fallsBackWithoutProfiles() {
        val scope = buildHomeRealtimeSpeedScopeKey(
            profileIds = emptyList(),
            fallbackScopeKey = " profile:active ",
        )

        assertEquals("fallback:profile:active", scope)
    }

    @Test
    fun restoreHomeRealtimeSpeedSeriesForScope_ignoresMismatchedSnapshotScope() {
        val snapshot = HomeAggregateSpeedHistorySnapshot(
            scopeKey = "profiles:a,b",
            points = listOf(
                HomeSpeedHistoryPoint(timestamp = 10L, uploadSpeed = 1L, downloadSpeed = 2L, onlineServerCount = 1),
            ),
        )

        val restored = restoreHomeRealtimeSpeedSeriesForScope(
            snapshot = snapshot,
            scopeKey = "profiles:c,d",
            maxPoints = 60,
        )

        assertTrue(restored.isEmpty())
    }
}

