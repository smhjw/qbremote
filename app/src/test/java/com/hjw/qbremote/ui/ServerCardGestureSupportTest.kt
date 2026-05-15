package com.hjw.qbremote.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerCardGestureSupportTest {

    @Test
    fun resolveServerCardClickSuppressionTimestamp_returnsTimestampOnlyAfterMeaningfulDrag() {
        assertEquals(
            1_000L,
            resolveServerCardClickSuppressionTimestamp(
                dragDistanceSinceStart = 12f,
                clickSuppressionThresholdPx = 6f,
                currentTimeMillis = 1_000L,
            ),
        )
        assertEquals(
            0L,
            resolveServerCardClickSuppressionTimestamp(
                dragDistanceSinceStart = 4f,
                clickSuppressionThresholdPx = 6f,
                currentTimeMillis = 1_000L,
            ),
        )
    }

    @Test
    fun shouldSuppressServerCardClick_onlyBlocksClicksInsideSuppressionWindow() {
        assertTrue(
            shouldSuppressServerCardClick(
                lastDragFinishedAt = 1_000L,
                currentTimeMillis = 1_120L,
            ),
        )
        assertFalse(
            shouldSuppressServerCardClick(
                lastDragFinishedAt = 1_000L,
                currentTimeMillis = 1_141L,
            ),
        )
        assertFalse(
            shouldSuppressServerCardClick(
                lastDragFinishedAt = 0L,
                currentTimeMillis = 1_050L,
            ),
        )
    }
}
