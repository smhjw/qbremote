package com.hjw.qbremote.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletServerStackCardPresentationSupportTest {

    @Test
    fun resolveWalletServerStackCardPresentation_draggingCardUsesExpandedTopStyle() {
        val presentation = resolveWalletServerStackCardPresentation(
            selected = false,
            isDragging = true,
            isSettling = false,
        )

        assertTrue(presentation.showExpandedLayout)
        assertEquals(16.dp, presentation.horizontalPadding)
        assertEquals(14.dp, presentation.verticalPadding)
        assertEquals(0.28f, presentation.borderAlpha)
    }

    @Test
    fun resolveWalletServerStackCardPresentation_settlingCardUsesExpandedTopStyle() {
        val presentation = resolveWalletServerStackCardPresentation(
            selected = false,
            isDragging = false,
            isSettling = true,
        )

        assertTrue(presentation.showExpandedLayout)
        assertEquals(16.dp, presentation.horizontalPadding)
        assertEquals(14.dp, presentation.verticalPadding)
        assertEquals(0.28f, presentation.borderAlpha)
    }

    @Test
    fun resolveWalletServerStackCardPresentation_idleSecondaryCardStaysCollapsed() {
        val presentation = resolveWalletServerStackCardPresentation(
            selected = false,
            isDragging = false,
            isSettling = false,
        )

        assertFalse(presentation.showExpandedLayout)
        assertEquals(15.dp, presentation.horizontalPadding)
        assertEquals(10.dp, presentation.verticalPadding)
        assertEquals(0.14f, presentation.borderAlpha)
    }
}
