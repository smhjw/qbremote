package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class VerticalReorderDragStateSupportTest {

    @Test
    fun createVerticalReorderDragState_startsAtOriginalSlot() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 1,
            slotCenters = listOf(100f, 200f, 300f),
        )

        val state = createVerticalReorderDragState(
            session = session,
            item = "B",
        )

        assertEquals("B", state.item)
        assertEquals(0f, state.offsetY)
        assertEquals(1, state.targetIndex)
    }

    @Test
    fun applyVerticalReorderDragDelta_updatesTargetIndexAndClampsOffset() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )
        val startState = createVerticalReorderDragState(
            session = session,
            item = "A",
        )

        val state = applyVerticalReorderDragDelta(
            state = startState,
            deltaY = 60f,
        )

        assertEquals(60f, state.offsetY)
        assertEquals(1, state.targetIndex)
    }

    @Test
    fun resolveVerticalReorderRestingOffset_returnsFinalSlotTranslationWithoutSecondSettleDelta() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )
        val dragged = createVerticalReorderDragState(
            session = session,
            item = "A",
        ).copy(
            offsetY = 72f,
            targetIndex = 1,
        )

        val restingOffset = resolveVerticalReorderRestingOffset(
            state = dragged,
            commit = true,
        )

        assertEquals(100f, restingOffset)
    }

    @Test
    fun calculateVerticalReorderSiblingOffset_usesTargetIndexFromDragState() {
        val session = buildTestSession(
            items = listOf("A", "B", "C"),
            startIndex = 0,
            slotCenters = listOf(100f, 200f, 300f),
        )
        val dragState = createVerticalReorderDragState(
            session = session,
            item = "A",
        ).copy(targetIndex = 1)

        val offset = calculateVerticalReorderSiblingOffset(
            item = "B",
            dragState = dragState,
        )

        assertEquals(-100f, offset)
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
