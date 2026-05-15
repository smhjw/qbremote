package com.hjw.qbremote.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hjw.qbremote.R

@Immutable
internal data class DashboardDisplayCardItem(
    val owner: DashboardChartCard,
    val representedCards: List<DashboardChartCard>,
) {
    init {
        require(representedCards.isNotEmpty())
        require(owner in representedCards)
        require(representedCards.distinct().size == representedCards.size)
    }

    val ownerKey: String
        get() = owner.storageKey

    val representedCardKeys: Set<String>
        get() = representedCards.mapTo(linkedSetOf()) { it.storageKey }
}

@Composable
internal fun dashboardChartCardLabel(card: DashboardChartCard): String {
    return when (card) {
        DashboardChartCard.COUNTRY_FLOW -> stringResource(R.string.dashboard_country_flow_title)
        DashboardChartCard.CATEGORY_SHARE -> stringResource(R.string.dashboard_category_share_title)
        DashboardChartCard.DAILY_UPLOAD -> stringResource(R.string.dashboard_upload_title)
        DashboardChartCard.TAG_UPLOAD -> stringResource(R.string.dashboard_tag_upload_share_title)
        DashboardChartCard.TORRENT_STATE -> stringResource(R.string.dashboard_torrent_state_share_title)
        DashboardChartCard.TRACKER_SITE -> stringResource(R.string.dashboard_tracker_site_share_title)
    }
}

internal fun parseDashboardCardOrder(
    raw: String,
    availableCards: List<DashboardChartCard>,
): List<DashboardChartCard> {
    val availableByKey = availableCards.associateBy { it.storageKey }
    val parsed = raw
        .split(',')
        .mapNotNull { token ->
            availableByKey[token.trim()]
        }
        .distinct()
        .toMutableList()
    availableCards.forEach { card ->
        if (!parsed.contains(card)) {
            parsed += card
        }
    }
    return parsed
}

internal fun serializeDashboardCardOrder(
    order: List<DashboardChartCard>,
    availableCards: List<DashboardChartCard>,
): String {
    return parseDashboardCardOrder(
        order.joinToString(",") { it.storageKey },
        availableCards,
    ).joinToString(",") { it.storageKey }
}

internal fun buildDashboardDisplayCards(
    visibleCards: List<DashboardChartCard>,
): List<DashboardDisplayCardItem> {
    return visibleCards.map { card ->
        DashboardDisplayCardItem(
            owner = card,
            representedCards = listOf(card),
        )
    }
}

internal fun applyDashboardDisplayCardVisibility(
    visibleKeys: Set<String>,
    displayCard: DashboardDisplayCardItem,
    visible: Boolean,
): Set<String> {
    return if (visible) {
        visibleKeys + displayCard.representedCardKeys
    } else {
        visibleKeys - displayCard.representedCardKeys
    }
}

internal fun reorderDashboardCardOrderForDisplay(
    order: List<DashboardChartCard>,
    displayCards: List<DashboardDisplayCardItem>,
    owner: DashboardChartCard,
    targetIndex: Int,
): List<DashboardChartCard> {
    val currentDisplayIndex = displayCards.indexOfFirst { it.owner == owner }
    if (currentDisplayIndex < 0 || targetIndex !in displayCards.indices || currentDisplayIndex == targetIndex) {
        return order
    }

    val movingDisplayCard = displayCards[currentDisplayIndex]
    val movingCards = movingDisplayCard.representedCards.toSet()
    val remainingOrder = order.filterNot { it in movingCards }
    val remainingDisplayCards = displayCards.filterIndexed { index, _ -> index != currentDisplayIndex }

    if (remainingDisplayCards.isEmpty()) return order

    return if (targetIndex >= remainingDisplayCards.size) {
        val anchor = remainingDisplayCards.last().representedCards.last()
        val anchorIndex = remainingOrder.indexOf(anchor)
        if (anchorIndex < 0) {
            order
        } else {
            remainingOrder.toMutableList().apply {
                addAll(anchorIndex + 1, movingDisplayCard.representedCards)
            }
        }
    } else {
        val anchor = remainingDisplayCards[targetIndex].representedCards.first()
        val anchorIndex = remainingOrder.indexOf(anchor)
        if (anchorIndex < 0) {
            order
        } else {
            remainingOrder.toMutableList().apply {
                addAll(anchorIndex, movingDisplayCard.representedCards)
            }
        }
    }
}
