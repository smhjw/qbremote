package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLayoutFollowupSupportTest {

    @Test
    fun buildCompactCountrySummaryItems_keepsTopThreeAsSeparateInlineItems() {
        val entries = listOf(
            ResolvedDashboardBarEntry(label = "US", value = 1_200_000_000L, valueText = "1.2 GB"),
            ResolvedDashboardBarEntry(label = "JP", value = 860_000_000L, valueText = "860 MB"),
            ResolvedDashboardBarEntry(label = "SG", value = 430_000_000L, valueText = "430 MB"),
            ResolvedDashboardBarEntry(label = "DE", value = 120_000_000L, valueText = "120 MB"),
        )

        val summaryItems = buildCompactCountrySummaryItems(entries)

        assertEquals(
            listOf(
                CompactCountrySummaryItem(labelText = "US", valueText = "1.2 GB"),
                CompactCountrySummaryItem(labelText = "JP", valueText = "860 MB"),
                CompactCountrySummaryItem(labelText = "SG", valueText = "430 MB"),
            ),
            summaryItems,
        )
    }

    @Test
    fun buildDashboardDisplayCards_keepsDistributionChartsAsSeparateDisplayItems() {
        val displayCards = buildDashboardDisplayCards(
            listOf(
                DashboardChartCard.COUNTRY_FLOW,
                DashboardChartCard.CATEGORY_SHARE,
                DashboardChartCard.TAG_UPLOAD,
                DashboardChartCard.DAILY_UPLOAD,
            ),
        )

        assertEquals(4, displayCards.size)
        assertEquals(DashboardChartCard.COUNTRY_FLOW, displayCards[0].owner)
        assertEquals(listOf(DashboardChartCard.COUNTRY_FLOW), displayCards[0].representedCards)
        assertEquals(DashboardChartCard.CATEGORY_SHARE, displayCards[1].owner)
        assertEquals(listOf(DashboardChartCard.CATEGORY_SHARE), displayCards[1].representedCards)
        assertEquals(DashboardChartCard.TAG_UPLOAD, displayCards[2].owner)
        assertEquals(listOf(DashboardChartCard.TAG_UPLOAD), displayCards[2].representedCards)
        assertEquals(DashboardChartCard.DAILY_UPLOAD, displayCards[3].owner)
        assertEquals(listOf(DashboardChartCard.DAILY_UPLOAD), displayCards[3].representedCards)
    }

    @Test
    fun reorderDashboardCardOrderForDisplay_movesOnlyTheSelectedDistributionCard() {
        val order = listOf(
            DashboardChartCard.COUNTRY_FLOW,
            DashboardChartCard.CATEGORY_SHARE,
            DashboardChartCard.TAG_UPLOAD,
            DashboardChartCard.DAILY_UPLOAD,
        )
        val displayCards = buildDashboardDisplayCards(order)

        val reordered = reorderDashboardCardOrderForDisplay(
            order = order,
            displayCards = displayCards,
            owner = DashboardChartCard.TAG_UPLOAD,
            targetIndex = 1,
        )

        assertEquals(
            listOf(
                DashboardChartCard.COUNTRY_FLOW,
                DashboardChartCard.TAG_UPLOAD,
                DashboardChartCard.CATEGORY_SHARE,
                DashboardChartCard.DAILY_UPLOAD,
            ),
            reordered,
        )
    }

    @Test
    fun applyDashboardDisplayCardVisibility_hidesOnlyTheSelectedDistributionCard() {
        val visibleKeys = linkedSetOf(
            DashboardChartCard.COUNTRY_FLOW.storageKey,
            DashboardChartCard.CATEGORY_SHARE.storageKey,
            DashboardChartCard.TAG_UPLOAD.storageKey,
            DashboardChartCard.DAILY_UPLOAD.storageKey,
        )
        val displayCard = DashboardDisplayCardItem(
            owner = DashboardChartCard.TAG_UPLOAD,
            representedCards = listOf(DashboardChartCard.TAG_UPLOAD),
        )

        val updated = applyDashboardDisplayCardVisibility(
            visibleKeys = visibleKeys,
            displayCard = displayCard,
            visible = false,
        )

        assertEquals(
            linkedSetOf(
                DashboardChartCard.COUNTRY_FLOW.storageKey,
                DashboardChartCard.CATEGORY_SHARE.storageKey,
                DashboardChartCard.DAILY_UPLOAD.storageKey,
            ),
            updated,
        )
    }

    @Test
    fun buildDashboardDisplayCards_keepsSingleDistributionCardStableWhenOnlyOneVisible() {
        val displayCards = buildDashboardDisplayCards(
            listOf(
                DashboardChartCard.COUNTRY_FLOW,
                DashboardChartCard.TAG_UPLOAD,
            ),
        )

        assertEquals(
            listOf(
                DashboardChartCard.COUNTRY_FLOW,
                DashboardChartCard.TAG_UPLOAD,
            ),
            displayCards.map { it.owner },
        )
        assertEquals(
            listOf(
                listOf(DashboardChartCard.COUNTRY_FLOW),
                listOf(DashboardChartCard.TAG_UPLOAD),
            ),
            displayCards.map { it.representedCards },
        )
    }
}
