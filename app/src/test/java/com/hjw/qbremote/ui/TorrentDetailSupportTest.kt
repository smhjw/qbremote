package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ServerCapabilities
import org.junit.Assert.assertEquals
import org.junit.Test

class TorrentDetailSupportTest {

    @Test
    fun torrentDetailPrimaryActions_returnsSupportedActionsInStableOrder() {
        val actions = torrentDetailPrimaryActions(
            ServerCapabilities(
                supportsExportTorrent = true,
                supportsReannounce = true,
                supportsRecheck = true,
            ),
        )

        assertEquals(
            listOf(
                TorrentDetailPrimaryAction.ExportTorrent,
                TorrentDetailPrimaryAction.Reannounce,
                TorrentDetailPrimaryAction.Recheck,
            ),
            actions,
        )
    }

    @Test
    fun torrentDetailPrimaryActions_skipsUnsupportedActions() {
        val actions = torrentDetailPrimaryActions(
            ServerCapabilities(
                supportsExportTorrent = false,
                supportsReannounce = true,
                supportsRecheck = false,
            ),
        )

        assertEquals(
            listOf(TorrentDetailPrimaryAction.Reannounce),
            actions,
        )
    }
}
