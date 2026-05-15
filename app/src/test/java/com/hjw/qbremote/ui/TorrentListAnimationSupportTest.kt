package com.hjw.qbremote.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentListAnimationSupportTest {

    @Test
    fun shouldAnimateTorrentPlacement_returnsFalse_forSameOrder() {
        val previousKeys = listOf("a", "b", "c")
        val currentKeys = listOf("a", "b", "c")

        assertFalse(shouldAnimateTorrentPlacement(previousKeys, currentKeys))
    }

    @Test
    fun shouldAnimateTorrentPlacement_returnsTrue_whenOnlyOrderChanges() {
        val previousKeys = listOf("a", "b", "c")
        val currentKeys = listOf("b", "a", "c")

        assertTrue(shouldAnimateTorrentPlacement(previousKeys, currentKeys))
    }

    @Test
    fun shouldAnimateTorrentPlacement_returnsFalse_forSpeedSorts() {
        val previousKeys = listOf("a", "b", "c")
        val currentKeys = listOf("b", "a", "c")

        assertFalse(
            shouldAnimateTorrentPlacement(
                previousKeys = previousKeys,
                currentKeys = currentKeys,
                sortOption = TorrentListSortOption.UPLOAD_SPEED,
            )
        )
        assertFalse(
            shouldAnimateTorrentPlacement(
                previousKeys = previousKeys,
                currentKeys = currentKeys,
                sortOption = TorrentListSortOption.DOWNLOAD_SPEED,
            )
        )
    }

    @Test
    fun shouldAnimateTorrentPlacement_returnsFalse_whenItemsAddedOrRemoved() {
        val previousKeys = listOf("a", "b", "c")
        val currentKeys = listOf("a", "c")

        assertFalse(shouldAnimateTorrentPlacement(previousKeys, currentKeys))
    }

    @Test
    fun shouldAnimateTorrentPlacement_returnsFalse_whenFilteringChangesButCountSame() {
        val previousKeys = listOf("a", "b", "c")
        val currentKeys = listOf("a", "d", "c")

        assertFalse(shouldAnimateTorrentPlacement(previousKeys, currentKeys))
    }

    @Test
    fun shouldAnimateTorrentPlacement_returnsFalse_whenDuplicateKeyCountsChange() {
        val previousKeys = listOf("a", "a", "b")
        val currentKeys = listOf("a", "b", "b")

        assertFalse(shouldAnimateTorrentPlacement(previousKeys, currentKeys))
    }
}
