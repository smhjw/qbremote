package com.hjw.qbremote.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenReorderScrollSupportTest {

    @Test
    fun shouldEnablePageListScroll_returnsFalseWhenServerStackDragIsActive() {
        assertFalse(
            shouldEnablePageListScroll(
                draggingServerProfileId = "server-a",
                draggingDashboardCard = null,
            ),
        )
    }

    @Test
    fun shouldEnablePageListScroll_returnsFalseWhenDashboardCardDragIsActive() {
        assertFalse(
            shouldEnablePageListScroll(
                draggingServerProfileId = null,
                draggingDashboardCard = DashboardDisplayCardItem(
                    owner = DashboardChartCard.COUNTRY_FLOW,
                    representedCards = listOf(DashboardChartCard.COUNTRY_FLOW),
                ),
            ),
        )
    }

    @Test
    fun shouldEnablePageListScroll_returnsTrueWhenNoReorderDragIsActive() {
        assertTrue(
            shouldEnablePageListScroll(
                draggingServerProfileId = null,
                draggingDashboardCard = null,
            ),
        )
    }
}
