package com.hjw.qbremote.data

import com.google.gson.JsonObject
import com.hjw.qbremote.data.model.AddTorrentRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransmissionBackendActionSupportTest {

    @Test
    fun resolveTransmissionRenamePath_usesCurrentTorrentName() {
        assertEquals(
            "Ubuntu.iso",
            resolveTransmissionRenamePath(currentTorrentName = " Ubuntu.iso "),
        )
    }

    @Test
    fun resolveTransmissionRenamePath_rejectsBlankCurrentName() {
        val error = runCatching {
            resolveTransmissionRenamePath(currentTorrentName = " ")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun buildTransmissionAddTorrentArguments_appliesPerTorrentSpeedLimits() {
        val args = buildTransmissionAddTorrentArguments(
            request = AddTorrentRequest(
                savePath = "/downloads/linux",
                paused = true,
                tags = "linux, iso",
                uploadLimitBytes = 64L * 1024L,
                downloadLimitBytes = 512L * 1024L,
            ),
            common = mutableMapOf("filename" to "magnet:?xt=urn:btih:test"),
        )

        assertEquals("/downloads/linux", args.requireString("download-dir"))
        assertEquals(true, args.requireBoolean("paused"))
        assertEquals(listOf("linux", "iso"), args.requireStringList("labels"))
        assertEquals(true, args.requireBoolean("uploadLimited"))
        assertEquals(64, args.requireInt("uploadLimit"))
        assertEquals(true, args.requireBoolean("downloadLimited"))
        assertEquals(512, args.requireInt("downloadLimit"))
    }

    @Test
    fun buildTransmissionAddTorrentArguments_omitsUnlimitedSpeedFlags() {
        val args = buildTransmissionAddTorrentArguments(
            request = AddTorrentRequest(
                uploadLimitBytes = -1L,
                downloadLimitBytes = -1L,
            ),
            common = mutableMapOf("filename" to "magnet:?xt=urn:btih:test"),
        )

        assertFalse(args.has("uploadLimited"))
        assertFalse(args.has("uploadLimit"))
        assertFalse(args.has("downloadLimited"))
        assertFalse(args.has("downloadLimit"))
    }

    private fun JsonObject.requireString(key: String): String = get(key).asString

    private fun JsonObject.requireBoolean(key: String): Boolean = get(key).asBoolean

    private fun JsonObject.requireInt(key: String): Int = get(key).asInt

    private fun JsonObject.requireStringList(key: String): List<String> {
        return getAsJsonArray(key).map { it.asString }
    }
}
