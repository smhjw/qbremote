package com.hjw.qbremote.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.hjw.qbremote.R
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentTracker

internal fun LazyListScope.torrentDetailPageContent(
    selectedTorrent: TorrentInfo?,
    selectedTorrentIdentity: String,
    showRestorePlaceholder: Boolean,
    crossSeedCounts: Map<String, Int>,
    state: TorrentDetailPageState,
    isPendingAction: (String) -> Boolean,
    onCopyHash: (String) -> Unit,
    onCopyMagnet: (String) -> Unit,
    onExportTorrent: (String, String) -> Unit,
    onPauseTorrent: (String) -> Unit,
    onResumeTorrent: (String) -> Unit,
    onDeleteTorrent: (String, Boolean) -> Unit,
    onRenameTorrent: (String, String) -> Unit,
    onSetTorrentLocation: (String, String) -> Unit,
    onSetTorrentCategory: (String, String) -> Unit,
    onSetTorrentTags: (String, String, String) -> Unit,
    onSetTorrentSpeedLimit: (String, String, String) -> Unit,
    onSetTorrentShareRatio: (String, String) -> Unit,
    onReannounceTorrent: (String) -> Unit,
    onRecheckTorrent: (String) -> Unit,
    onCopyTracker: (TorrentTracker) -> Unit,
    onEditTracker: (String, TorrentTracker, String) -> Unit,
    onDeleteTracker: (String, TorrentTracker) -> Unit,
) {
    val torrent = selectedTorrent
    if (torrent == null) {
        if (showRestorePlaceholder && selectedTorrentIdentity.isNotBlank()) {
            item {
                PageRestorePlaceholder()
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.torrent_detail_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }

    item(key = "torrent_detail_card") {
        AnimatedContent(
            targetState = torrent.hash,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 120))
            },
            label = "torrentDetailCard",
        ) { targetHash ->
            TorrentOperationDetailCard(
                torrent = torrent,
                crossSeedCount = crossSeedCounts[torrentIdentityKey(torrent)] ?: 0,
                isPending = isPendingAction(targetHash),
                capabilities = state.activeCapabilities,
                detailLoading = state.detailLoading && state.detailHash == targetHash,
                detailProperties = if (state.detailHash == targetHash) state.detailProperties else null,
                detailFiles = if (state.detailHash == targetHash) state.detailFiles else emptyList(),
                detailTrackers = if (state.detailHash == targetHash) state.detailTrackers else emptyList(),
                magnetUri = buildMagnetUri(
                    hash = targetHash,
                    name = torrent.name,
                    trackerUrls = if (state.detailHash == targetHash) {
                        state.detailTrackers.map { tracker -> tracker.url }
                    } else {
                        emptyList()
                    },
                ),
                categoryOptions = state.categoryOptions,
                tagOptions = state.tagOptions,
                deleteFilesDefault = state.deleteFilesDefault,
                deleteFilesWhenNoSeeders = state.deleteFilesWhenNoSeeders,
                onCopyHash = { onCopyHash(targetHash) },
                onCopyMagnet = onCopyMagnet,
                onExportTorrent = { onExportTorrent(targetHash, torrent.name) },
                onPause = { onPauseTorrent(targetHash) },
                onResume = { onResumeTorrent(targetHash) },
                onDelete = { deleteFiles -> onDeleteTorrent(targetHash, deleteFiles) },
                onRename = { onRenameTorrent(targetHash, it) },
                onSetLocation = { onSetTorrentLocation(targetHash, it) },
                onSetCategory = { onSetTorrentCategory(targetHash, it) },
                onSetTags = { oldTags, newTags -> onSetTorrentTags(targetHash, oldTags, newTags) },
                onSetSpeedLimit = { dl, up -> onSetTorrentSpeedLimit(targetHash, dl, up) },
                onSetShareRatio = { ratio -> onSetTorrentShareRatio(targetHash, ratio) },
                onReannounce = { onReannounceTorrent(targetHash) },
                onRecheck = { onRecheckTorrent(targetHash) },
                onCopyTracker = onCopyTracker,
                onEditTracker = { tracker, newUrl -> onEditTracker(targetHash, tracker, newUrl) },
                onDeleteTracker = { tracker -> onDeleteTracker(targetHash, tracker) },
            )
        }
    }
}
