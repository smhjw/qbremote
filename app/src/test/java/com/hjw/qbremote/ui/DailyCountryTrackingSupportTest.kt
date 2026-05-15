package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.TorrentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCountryTrackingSupportTest {

    @Test
    fun resolveTrackedCountryHashes_marksUploadingTorrentAsSamplingCandidate() {
        val resolution = resolveTrackedCountryHashes(
            torrents = listOf(
                TorrentInfo(
                    hash = "hash-1",
                    name = "Ubuntu ISO",
                    uploaded = 2_048L,
                    uploadSpeed = 512L,
                ),
            ),
            lastSeenByTorrent = emptyMap(),
            activeHashes = emptyMap(),
            now = 10_000L,
            ttlMs = 1_500L,
        )

        assertEquals(listOf("hash-1"), resolution.candidateHashes)
        assertEquals(2_048L, resolution.lastSeenByTorrent.getValue("hash-1"))
        assertTrue(resolution.activeHashes.getValue("hash-1") > 10_000L)
    }
}
