package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerCardReorderSupportTest {

    @Test
    fun buildHomeServerStackReorderSession_usesCompactExposedStepCenters() {
        val session = buildHomeServerStackReorderSession(
            orderedProfileIds = listOf("A", "B"),
            startIndex = 1,
            exposedStepPx = 60f,
            edgeSlackPx = 0f,
        )

        assertEquals(listOf(90f, 30f), session.slotCenters)
    }

    @Test
    fun buildHomeServerStackReorderSession_reordersTopCardAfterCompactDownwardMotion() {
        val session = buildHomeServerStackReorderSession(
            orderedProfileIds = listOf("A", "B"),
            startIndex = 1,
            exposedStepPx = 60f,
            edgeSlackPx = 0f,
        )

        val targetIndex = resolveVerticalReorderTargetIndex(
            session = session,
            dragOffsetY = 25f,
        )

        assertEquals(0, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndex_reordersDownwardBeforeCrossingSiblingCenter() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val targetIndex = resolveVerticalReorderTargetIndex(
            session = session,
            dragOffsetY = 40f,
        )

        assertEquals(1, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndex_keepsOriginalIndexForSmallDownwardMotion() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val targetIndex = resolveVerticalReorderTargetIndex(
            session = session,
            dragOffsetY = 20f,
        )

        assertEquals(0, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndex_reordersUpwardBeforeCrossingSiblingCenter() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 2,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val targetIndex = resolveVerticalReorderTargetIndex(
            session = session,
            dragOffsetY = -40f,
        )

        assertEquals(1, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndex_doesNotSkipPastIntermediateSlot() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val targetIndex = resolveVerticalReorderTargetIndex(
            session = session,
            dragOffsetY = 80f,
        )

        assertEquals(1, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndex_supportsDescendingVisualSlotCenters() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(300f, 200f, 100f),
        )

        val targetIndex = resolveVerticalReorderTargetIndex(
            session = session,
            dragOffsetY = -40f,
        )

        assertEquals(1, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndexWithHysteresis_keepsAdjacentTargetUntilRetreatCrossesExitThreshold() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val targetIndex = resolveVerticalReorderTargetIndexWithHysteresis(
            session = session,
            dragOffsetY = 28f,
            previousTargetIndex = 1,
        )

        assertEquals(1, targetIndex)
    }

    @Test
    fun resolveVerticalReorderTargetIndexWithHysteresis_releasesAdjacentTargetAfterRetreatPastExitThreshold() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val targetIndex = resolveVerticalReorderTargetIndexWithHysteresis(
            session = session,
            dragOffsetY = 18f,
            previousTargetIndex = 1,
        )

        assertEquals(0, targetIndex)
    }

    @Test
    fun resolveHomeServerStackDropPlan_returnsDeferredCommitAndFinalRestingOffset() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )
        val dragState = createVerticalReorderDragState(
            session = session,
            item = "A",
        ).copy(
            offsetY = 60f,
            targetIndex = 1,
        )

        val plan = resolveHomeServerStackDropPlan(
            state = dragState,
            commit = true,
        )

        assertEquals(listOf("B", "A", "C"), plan.reorderedIds)
        assertEquals(1, plan.finalTargetIndex)
        assertEquals(true, plan.shouldCommitReorder)
        assertEquals(100f, plan.finalOffsetY)
    }

    @Test
    fun resolveHomeServerStackDropPlan_revertsToOriginalSlotWhenCancelled() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )
        val dragState = createVerticalReorderDragState(
            session = session,
            item = "A",
        ).copy(
            offsetY = 60f,
            targetIndex = 1,
        )

        val plan = resolveHomeServerStackDropPlan(
            state = dragState,
            commit = false,
        )

        assertEquals(listOf("A", "B", "C"), plan.reorderedIds)
        assertEquals(0, plan.finalTargetIndex)
        assertEquals(false, plan.shouldCommitReorder)
        assertEquals(0f, plan.finalOffsetY)
    }

    private fun buildTestSession(
        items: List<String>,
        startIndex: Int,
        slotCenters: List<Float>,
    ): VerticalReorderSession<String> {
        val slotTops = slotCenters.map { it - 40f }
        return VerticalReorderSession(
            items = items,
            indexByItem = items.mapIndexed { index, item -> item to index }.toMap(),
            startIndex = startIndex,
            slotTops = slotTops,
            slotCenters = slotCenters,
            minOffset = -500f,
            maxOffset = 500f,
        )
    }

}
