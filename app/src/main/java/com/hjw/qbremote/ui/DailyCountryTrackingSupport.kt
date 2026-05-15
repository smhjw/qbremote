package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.TorrentInfo

internal data class CountryTrackingHashResolution(
    val lastSeenByTorrent: Map<String, Long>,
    val activeHashes: Map<String, Long>,
    val candidateHashes: List<String>,
)

internal fun resolveTrackedCountryHashes(
    torrents: List<TorrentInfo>,
    lastSeenByTorrent: Map<String, Long>,
    activeHashes: Map<String, Long>,
    now: Long,
    ttlMs: Long,
): CountryTrackingHashResolution {
    val nextLastSeenByTorrent = lastSeenByTorrent.toMutableMap()
    val nextActiveHashes = activeHashes.toMutableMap()
    val hashesByTrackingKey = torrents.associateBy(::dailyCountryTorrentTrackingKey)
    val normalizedTorrentHashes = torrents
        .map { torrent -> torrent.hash.trim() }
        .filter { hash -> hash.isNotBlank() }
        .toHashSet()

    torrents.forEach { torrent ->
        val trackingKey = dailyCountryTorrentTrackingKey(torrent)
        val hash = torrent.hash.trim()
        if (hash.isBlank()) return@forEach
        val currentUploaded = torrent.uploaded.coerceAtLeast(0L)
        val previousUploaded = nextLastSeenByTorrent[trackingKey]
        nextLastSeenByTorrent[trackingKey] = currentUploaded

        if (previousUploaded == null) {
            if (torrent.uploadSpeed > 0L) {
                nextActiveHashes[hash] = now + ttlMs
            }
            return@forEach
        }

        if (currentUploaded > previousUploaded || torrent.uploadSpeed > 0L) {
            nextActiveHashes[hash] = now + ttlMs
        }
    }

    nextLastSeenByTorrent.keys.retainAll(hashesByTrackingKey.keys)
    nextActiveHashes.entries.removeAll { (hash, expiresAt) ->
        expiresAt < now || hash !in normalizedTorrentHashes
    }

    return CountryTrackingHashResolution(
        lastSeenByTorrent = nextLastSeenByTorrent,
        activeHashes = nextActiveHashes,
        candidateHashes = nextActiveHashes.keys
            .filter { hash -> hash in normalizedTorrentHashes }
            .sorted(),
    )
}

internal fun dailyCountryTorrentTrackingKey(torrent: TorrentInfo): String {
    return torrent.hash.ifBlank {
        "${torrent.name}|${torrent.addedOn}|${torrent.savePath}|${torrent.size}"
    }
}
