package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot
import com.hjw.qbremote.data.TorrentRepository
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.DailyCountryUploadStats
import com.hjw.qbremote.data.model.TorrentInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.Locale

internal const val COUNTRY_TRACKER_SAMPLE_INTERVAL_MS = 5_000L
internal const val COUNTRY_TRACKER_ACTIVE_TTL_MS = 20_000L

internal class DailyCountryUploadTracker(
    private val connectionStore: ConnectionStore,
    private val repository: TorrentRepository,
) {
    val mutex = Mutex()

    private var scopeKey: String? = null
    private var trackingDate: LocalDate? = null
    private val totalsByCode = mutableMapOf<String, Long>()
    private val peerSnapshots = mutableMapOf<String, CountryPeerSnapshot>()
    private val lastSeenByTorrent = mutableMapOf<String, Long>()
    private val activeHashes = mutableMapOf<String, Long>()

    fun reset() {
        scopeKey = null
        trackingDate = null
        totalsByCode.clear()
        peerSnapshots.clear()
        lastSeenByTorrent.clear()
        activeHashes.clear()
    }

    suspend fun sample(
        profileId: String,
        key: String,
        torrents: List<TorrentInfo>,
    ): DailyCountryUploadStats {
        ensureLoaded(key)
        val today = LocalDate.now()
        if (trackingDate != today) {
            trackingDate = today
            totalsByCode.clear()
            peerSnapshots.clear()
            lastSeenByTorrent.clear()
            activeHashes.clear()
        }

        val candidateHashes = collectTrackedHashes(torrents)

        val samples = if (candidateHashes.isNotEmpty()) {
            repository.fetchCountryPeerSnapshots(profileId, candidateHashes).getOrElse { emptyList() }
        } else {
            emptyList()
        }

        val currentPeerSnapshots = samples.associateBy { it.key }
        val fallbackNames = samples
            .groupBy { it.countryCode.trim().uppercase(Locale.US) }
            .mapValues { (_, snapshots) ->
                snapshots.firstNotNullOfOrNull { it.countryName.trim().takeIf { name -> name.isNotBlank() } }.orEmpty()
            }

        samples.forEach { snapshot ->
            val countryCode = snapshot.countryCode.trim().uppercase(Locale.US)
            if (countryCode.isBlank()) return@forEach
            val previousSnapshot = peerSnapshots[snapshot.key]
            val previousUploaded = previousSnapshot?.uploadedBytes?.coerceAtLeast(0L)
            val currentUploaded = snapshot.uploadedBytes.coerceAtLeast(0L)
            val delta = when {
                previousUploaded == null -> 0L
                currentUploaded < previousUploaded -> currentUploaded
                else -> currentUploaded - previousUploaded
            }
            if (delta <= 0L) return@forEach
            totalsByCode[countryCode] = (totalsByCode[countryCode] ?: 0L) + delta
        }

        peerSnapshots.keys.retainAll(currentPeerSnapshots.keys)
        peerSnapshots.putAll(currentPeerSnapshots)

        connectionStore.saveDailyCountryUploadTrackingSnapshot(
            scopeKey = key,
            snapshot = DailyCountryUploadTrackingSnapshot(
                date = today.toString(),
                totalsByCountry = totalsByCode.toMap(),
                peerSnapshots = peerSnapshots.toMap(),
                lastSeenByTorrent = lastSeenByTorrent.toMap(),
            ),
        )

        return DailyCountryUploadStats(
            dateLabel = today.toString(),
            countries = totalsByCode
                .filterValues { it > 0L }
                .entries
                .sortedByDescending { it.value }
                .map { (countryCode, uploadedBytes) ->
                    CountryUploadRecord(
                        countryCode = countryCode,
                        countryName = fallbackNames[countryCode].orEmpty(),
                        uploadedBytes = uploadedBytes,
                    )
                },
        )
    }

    suspend fun withLock(block: suspend () -> Unit) {
        mutex.withLock { block() }
    }

    private suspend fun ensureLoaded(key: String) {
        if (scopeKey == key) return

        scopeKey = key
        trackingDate = null
        totalsByCode.clear()
        peerSnapshots.clear()
        lastSeenByTorrent.clear()
        activeHashes.clear()

        val snapshot = connectionStore.loadDailyCountryUploadTrackingSnapshot(key) ?: return
        trackingDate = runCatching {
            snapshot.date.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        }.getOrNull()
        totalsByCode.putAll(snapshot.totalsByCountry)
        peerSnapshots.putAll(snapshot.peerSnapshots)
        lastSeenByTorrent.putAll(snapshot.lastSeenByTorrent)
    }

    private fun collectTrackedHashes(torrents: List<TorrentInfo>): List<String> {
        val now = System.currentTimeMillis()
        val result = resolveTrackedCountryHashes(
            torrents = torrents,
            lastSeenByTorrent = lastSeenByTorrent,
            activeHashes = activeHashes,
            now = now,
            ttlMs = COUNTRY_TRACKER_ACTIVE_TTL_MS,
        )
        lastSeenByTorrent.clear()
        lastSeenByTorrent.putAll(result.lastSeenByTorrent)
        activeHashes.clear()
        activeHashes.putAll(result.activeHashes)
        return result.candidateHashes
    }
}
