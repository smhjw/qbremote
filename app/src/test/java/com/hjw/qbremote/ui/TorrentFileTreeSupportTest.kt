package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.TorrentFileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentFileTreeSupportTest {

    @Test
    fun resolveTorrentFileBrowserSelection_returnsRequestedNodeForValidPath() {
        val root = buildTorrentFileTree(
            listOf(
                TorrentFileInfo(index = 0, name = "media/movies/a.mkv", size = 100L, progress = 0.5f, priority = 1),
                TorrentFileInfo(index = 1, name = "media/series/b.mkv", size = 80L, progress = 1f, priority = 1),
            ),
        )

        val selection = resolveTorrentFileBrowserSelection(
            root = root,
            pathSegments = listOf("media", "movies"),
        )

        assertEquals(listOf("media", "movies"), selection.pathSegments)
        assertEquals("media/movies", selection.node.fullPath)
        assertTrue(selection.node.isDirectory)
    }

    @Test
    fun resolveTorrentFileBrowserSelection_fallsBackToRootForInvalidPath() {
        val root = buildTorrentFileTree(
            listOf(
                TorrentFileInfo(index = 0, name = "media/movies/a.mkv", size = 100L, progress = 0.5f, priority = 1),
            ),
        )

        val selection = resolveTorrentFileBrowserSelection(
            root = root,
            pathSegments = listOf("media", "missing"),
        )

        assertEquals(emptyList<String>(), selection.pathSegments)
        assertEquals("", selection.node.fullPath)
        assertTrue(selection.node.isDirectory)
    }
}
