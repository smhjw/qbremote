package com.hjw.qbremote.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.hjw.qbremote.data.CachedDashboardServerSnapshot

internal data class VerticalReorderSession<T>(
    val items: List<T>,
    val indexByItem: Map<T, Int>,
    val startIndex: Int,
    val slotTops: List<Float>,
    val slotCenters: List<Float>,
    val minOffset: Float,
    val maxOffset: Float,
)

internal data class VerticalReorderDragState<T>(
    val item: T,
    val session: VerticalReorderSession<T>,
    val offsetY: Float,
    val targetIndex: Int,
)

internal const val ReorderDraggedScale = 1.005f
internal const val ReorderDraggedShadow = 6f
internal const val ReorderSettlingShadow = 4f
internal const val ReorderSelectedShadow = 10f
internal const val ReorderCollapsedShadow = 4f
internal val ReorderScaleAnimationSpec = spring<Float>(
    dampingRatio = 1f,
    stiffness = 900f,
)
internal val ReorderSiblingOffsetAnimationSpec = spring<Float>(
    dampingRatio = 1f,
    stiffness = 900f,
)
internal val ReorderSettleAnimationSpec = tween<Float>(
    durationMillis = 140,
    easing = FastOutSlowInEasing,
)

private const val ReorderEnterThresholdFraction = 0.35f
private const val ReorderExitThresholdFraction = 0.82f

internal fun <T> buildVerticalReorderSession(
    items: List<T>,
    startIndex: Int,
    slotTops: List<Float>,
    slotHeights: List<Float>,
    edgeSlackPx: Float,
): VerticalReorderSession<T> {
    require(items.size == slotTops.size && items.size == slotHeights.size)
    val slotCenters = slotTops.indices.map { index ->
        slotTops[index] + (slotHeights[index] / 2f)
    }
    val startCenter = slotCenters[startIndex]
    val minOffset = (slotCenters.minOrNull() ?: startCenter) - startCenter - edgeSlackPx
    val maxOffset = (slotCenters.maxOrNull() ?: startCenter) - startCenter + edgeSlackPx
    return VerticalReorderSession(
        items = items,
        indexByItem = items.mapIndexed { index, item -> item to index }.toMap(),
        startIndex = startIndex,
        slotTops = slotTops,
        slotCenters = slotCenters,
        minOffset = minOffset,
        maxOffset = maxOffset,
    )
}

internal fun <T> resolveVerticalReorderTargetIndex(
    session: VerticalReorderSession<T>,
    dragOffsetY: Float,
): Int {
    val currentCenter = session.slotCenters[session.startIndex] + dragOffsetY
    var targetIndex = session.startIndex

    if (isMovingTowardHigherReorderIndex(session, currentCenter)) {
        for (index in session.startIndex until session.slotCenters.lastIndex) {
            if (
                hasCrossedReorderThreshold(
                    currentCenter = currentCenter,
                    fromCenter = session.slotCenters[index],
                    toCenter = session.slotCenters[index + 1],
                )
            ) {
                targetIndex = index + 1
            } else {
                break
            }
        }
    } else {
        for (index in (session.startIndex - 1) downTo 0) {
            if (
                hasCrossedReorderThreshold(
                    currentCenter = currentCenter,
                    fromCenter = session.slotCenters[index + 1],
                    toCenter = session.slotCenters[index],
                )
            ) {
                targetIndex = index
            } else {
                break
            }
        }
    }
    return targetIndex
}

internal fun <T> resolveVerticalReorderTargetIndexWithHysteresis(
    session: VerticalReorderSession<T>,
    dragOffsetY: Float,
    previousTargetIndex: Int,
): Int {
    val resolvedTargetIndex = resolveVerticalReorderTargetIndex(session, dragOffsetY)
    if (resolvedTargetIndex == previousTargetIndex) return resolvedTargetIndex
    if (previousTargetIndex !in session.items.indices || previousTargetIndex == session.startIndex) {
        return resolvedTargetIndex
    }

    val isRetreatingTowardStart = when {
        session.startIndex < previousTargetIndex -> resolvedTargetIndex < previousTargetIndex
        session.startIndex > previousTargetIndex -> resolvedTargetIndex > previousTargetIndex
        else -> false
    }
    if (!isRetreatingTowardStart) return resolvedTargetIndex

    val currentCenter = session.slotCenters[session.startIndex] + dragOffsetY
    var heldTargetIndex = previousTargetIndex
    while (heldTargetIndex != resolvedTargetIndex) {
        val nextIndexTowardStart = if (heldTargetIndex > session.startIndex) {
            heldTargetIndex - 1
        } else {
            heldTargetIndex + 1
        }
        if (
            !hasCrossedReorderExitThreshold(
                currentCenter = currentCenter,
                fromCenter = session.slotCenters[heldTargetIndex],
                toCenter = session.slotCenters[nextIndexTowardStart],
            )
        ) {
            return heldTargetIndex
        }
        heldTargetIndex = nextIndexTowardStart
    }
    return resolvedTargetIndex
}

internal fun <T> createVerticalReorderDragState(
    session: VerticalReorderSession<T>,
    item: T,
): VerticalReorderDragState<T> {
    require(session.items.getOrNull(session.startIndex) == item) {
        "Dragged item must match the session start index item."
    }
    return VerticalReorderDragState(
        item = item,
        session = session,
        offsetY = 0f,
        targetIndex = session.startIndex,
    )
}

internal fun <T> applyVerticalReorderDragDelta(
    state: VerticalReorderDragState<T>,
    deltaY: Float,
): VerticalReorderDragState<T> {
    val nextOffsetY = (state.offsetY + deltaY)
        .coerceIn(state.session.minOffset, state.session.maxOffset)
    val nextTargetIndex = resolveVerticalReorderTargetIndexWithHysteresis(
        session = state.session,
        dragOffsetY = nextOffsetY,
        previousTargetIndex = state.targetIndex,
    )
    return state.copy(
        offsetY = nextOffsetY,
        targetIndex = nextTargetIndex,
    )
}

internal fun <T> resolveVerticalReorderFinalTargetIndex(
    state: VerticalReorderDragState<T>,
    commit: Boolean,
): Int {
    if (!commit) return state.session.startIndex
    return if (state.targetIndex in state.session.items.indices) {
        state.targetIndex
    } else {
        state.session.startIndex
    }
}

internal fun <T> resolveVerticalReorderRestingOffset(
    state: VerticalReorderDragState<T>,
    commit: Boolean,
): Float {
    val finalTargetIndex = resolveVerticalReorderFinalTargetIndex(
        state = state,
        commit = commit,
    )
    if (finalTargetIndex !in state.session.slotTops.indices) return 0f
    return state.session.slotTops[finalTargetIndex] - state.session.slotTops[state.session.startIndex]
}

internal fun <T> calculateVerticalReorderSiblingOffset(
    item: T,
    dragState: VerticalReorderDragState<T>?,
): Float {
    if (dragState == null || dragState.item == item) return 0f
    val dragSession = dragState.session
    if (dragState.targetIndex !in dragSession.items.indices) return 0f
    val originalIndex = dragSession.indexByItem[item] ?: return 0f
    if (dragSession.startIndex == dragState.targetIndex) return 0f
    val reorderedItems = moveItemInList(
        items = dragSession.items,
        fromIndex = dragSession.startIndex,
        toIndex = dragState.targetIndex,
    )
    val targetSlotIndex = reorderedItems.indexOf(item)
    if (targetSlotIndex < 0) return 0f
    return dragSession.slotTops[targetSlotIndex] - dragSession.slotTops[originalIndex]
}

internal fun <T> reconcileReorderableItemOrder(
    currentOrder: List<T>,
    availableItems: List<T>,
): List<T> {
    if (availableItems.isEmpty()) return emptyList()
    if (currentOrder.isEmpty()) return availableItems
    val availableSet = availableItems.toHashSet()
    val retained = currentOrder.filter { it in availableSet }
    if (retained.size == availableItems.size && retained == availableItems) {
        return retained
    }
    val missing = availableItems.filterNot { it in retained }
    return retained + missing
}

internal fun orderDashboardServerSnapshots(
    snapshots: List<CachedDashboardServerSnapshot>,
    orderedProfileIds: List<String>,
): List<CachedDashboardServerSnapshot> {
    if (snapshots.isEmpty()) return emptyList()
    val reconciledIds = reconcileReorderableItemOrder(
        currentOrder = orderedProfileIds,
        availableItems = snapshots.map { it.profileId },
    )
    val snapshotByProfileId = snapshots.associateBy { it.profileId }
    return reconciledIds.mapNotNull(snapshotByProfileId::get)
}

internal fun buildHomeServerStackReorderSession(
    orderedProfileIds: List<String>,
    startIndex: Int,
    exposedStepPx: Float,
    edgeSlackPx: Float,
): VerticalReorderSession<String> {
    require(exposedStepPx > 0f) { "Home server stack exposed step must be positive." }
    val slotTops = orderedProfileIds.indices.map { index ->
        (orderedProfileIds.lastIndex - index) * exposedStepPx
    }
    val slotHeights = List(orderedProfileIds.size) { exposedStepPx }
    return buildVerticalReorderSession(
        items = orderedProfileIds,
        startIndex = startIndex,
        slotTops = slotTops,
        slotHeights = slotHeights,
        edgeSlackPx = edgeSlackPx,
    )
}

internal fun calculateServerStackSiblingOffset(
    profileId: String,
    draggingProfileId: String?,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<String>?,
): Float {
    return calculateVerticalReorderSiblingOffset(
        item = profileId,
        draggingItem = draggingProfileId,
        draggingTargetIndex = draggingTargetIndex,
        dragSession = dragSession,
    )
}

internal fun calculateDashboardSiblingOffset(
    card: DashboardDisplayCardItem,
    draggingCard: DashboardDisplayCardItem?,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<DashboardDisplayCardItem>?,
): Float {
    return calculateVerticalReorderSiblingOffset(
        item = card,
        draggingItem = draggingCard,
        draggingTargetIndex = draggingTargetIndex,
        dragSession = dragSession,
    )
}

internal fun shouldEnablePageListScroll(
    draggingServerProfileId: String?,
    draggingDashboardCard: DashboardDisplayCardItem?,
    settlingServerProfileId: String? = null,
    settlingDashboardCard: DashboardDisplayCardItem? = null,
): Boolean {
    return draggingServerProfileId == null &&
        draggingDashboardCard == null &&
        settlingServerProfileId == null &&
        settlingDashboardCard == null
}

private fun <T> calculateVerticalReorderSiblingOffset(
    item: T,
    draggingItem: T?,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<T>?,
): Float {
    if (draggingItem == null || dragSession == null) return 0f
    return calculateVerticalReorderSiblingOffset(
        item = item,
        dragState = VerticalReorderDragState(
            item = draggingItem,
            session = dragSession,
            offsetY = 0f,
            targetIndex = draggingTargetIndex,
        ),
    )
}

private fun <T> moveItemInList(
    items: List<T>,
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
        return items
    }
    return items.toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}

private fun <T> isMovingTowardHigherReorderIndex(
    session: VerticalReorderSession<T>,
    currentCenter: Float,
): Boolean {
    val startCenter = session.slotCenters[session.startIndex]
    val visualOrderAscending = (session.slotCenters.lastOrNull() ?: startCenter) >=
        (session.slotCenters.firstOrNull() ?: startCenter)
    return if (visualOrderAscending) {
        currentCenter >= startCenter
    } else {
        currentCenter <= startCenter
    }
}

private fun hasCrossedReorderThreshold(
    currentCenter: Float,
    fromCenter: Float,
    toCenter: Float,
): Boolean {
    val threshold = fromCenter + ((toCenter - fromCenter) * ReorderEnterThresholdFraction)
    return if (toCenter >= fromCenter) {
        currentCenter >= threshold
    } else {
        currentCenter <= threshold
    }
}

private fun hasCrossedReorderExitThreshold(
    currentCenter: Float,
    fromCenter: Float,
    toCenter: Float,
): Boolean {
    val threshold = fromCenter + ((toCenter - fromCenter) * ReorderExitThresholdFraction)
    return if (toCenter >= fromCenter) {
        currentCenter >= threshold
    } else {
        currentCenter <= threshold
    }
}
