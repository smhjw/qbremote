package com.hjw.qbremote.ui

import com.hjw.qbremote.data.DailyUploadTrackingSnapshot
import com.hjw.qbremote.data.model.TorrentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.time.LocalDate

class DailyUploadTrackingSupportTest {

    @Test
    fun advanceDailyUploadTrackingSnapshot_preservesAccumulatedTotalsAfterTorrentDisappears() {
        val today = LocalDate.of(2026, 4, 2)
        val previousSnapshot = DailyUploadTrackingSnapshot(
            date = today.toString(),
            totalsByTag = mapOf("movies" to 1024L),
            countedTagsByTorrent = mapOf("torrent-1" to listOf("movies")),
            lastSeenByTorrent = mapOf("torrent-1" to 2048L),
        )

        val result = invokeAdvanceDailyUploadTrackingSnapshot(
            previousSnapshot = previousSnapshot,
            today = today,
            torrents = emptyList(),
        )

        val updatedSnapshot = result.first
        val stats = result.second
        assertEquals(1024L, updatedSnapshot.totalsByTag.getValue("movies"))
        assertTrue(updatedSnapshot.lastSeenByTorrent.isEmpty())
        assertEquals(1, stats.size)
        assertEquals("movies", stats.first().tag)
        assertEquals(1024L, stats.first().uploadedBytes)
    }

    @Test
    fun advanceDailyUploadTrackingSnapshot_addsOnlyIncrementalBytesForExistingTorrent() {
        val today = LocalDate.of(2026, 4, 2)
        val previousSnapshot = DailyUploadTrackingSnapshot(
            date = today.toString(),
            totalsByTag = mapOf("linux" to 512L),
            countedTagsByTorrent = mapOf("torrent-1" to listOf("linux")),
            lastSeenByTorrent = mapOf("torrent-1" to 2_048L),
        )

        val result = invokeAdvanceDailyUploadTrackingSnapshot(
            previousSnapshot = previousSnapshot,
            today = today,
            torrents = listOf(
                TorrentInfo(
                    hash = "torrent-1",
                    name = "Ubuntu",
                    uploaded = 3_072L,
                    tags = "linux",
                ),
            ),
        )

        assertEquals(1_536L, result.first.totalsByTag.getValue("linux"))
        assertEquals(1, result.second.size)
        assertEquals(1_536L, result.second.first().uploadedBytes)
    }

    private fun invokeAdvanceDailyUploadTrackingSnapshot(
        previousSnapshot: DailyUploadTrackingSnapshot?,
        today: LocalDate,
        torrents: List<TorrentInfo>,
    ): Pair<DailyUploadTrackingSnapshot, List<DailyTagUploadStat>> {
        val method = findRequiredStaticMethod(
            containerClassName = "com.hjw.qbremote.ui.DailyUploadTrackingSupportKt",
            functionName = "advanceDailyUploadTrackingSnapshot",
            parameterTypes = arrayOf(
                DailyUploadTrackingSnapshot::class.java,
                LocalDate::class.java,
                List::class.java,
            ),
        )
        val result = method.invoke(null, previousSnapshot, today, torrents)
            ?: error("`advanceDailyUploadTrackingSnapshot` returned null.")
        @Suppress("UNCHECKED_CAST")
        return result as? Pair<DailyUploadTrackingSnapshot, List<DailyTagUploadStat>>
            ?: error("`advanceDailyUploadTrackingSnapshot` should return Pair<DailyUploadTrackingSnapshot, List<DailyTagUploadStat>>.")
    }

    private fun findRequiredStaticMethod(
        containerClassName: String,
        functionName: String,
        parameterTypes: Array<Class<*>>,
    ): Method {
        val container = Class.forName(containerClassName)
        return runCatching {
            container.getDeclaredMethod(functionName, *parameterTypes)
        }.getOrElse {
            val signature = parameterTypes.joinToString(", ") { it.simpleName }
            error("Expected `${container.simpleName}.$functionName($signature)` as planned top-level helper.")
        }.also { it.isAccessible = true }
    }
}
