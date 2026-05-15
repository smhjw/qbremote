package com.hjw.qbremote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor

@Composable
internal fun TorrentCard(
    torrent: TorrentInfo,
    crossSeedCount: Int,
    isPending: Boolean,
    onOpenDetails: () -> Unit,
) {
    val effectiveState = effectiveTorrentState(torrent)
    val stateLabel = localizedTorrentStateLabel(effectiveState)
    val categoryText = normalizeCategoryLabel(
        category = torrent.category,
        noCategoryText = stringResource(R.string.no_category),
    )
    val tagsText = compactTagsLabel(
        tags = torrent.tags,
        noTagsText = stringResource(R.string.no_tags),
    )
    val activeAgoText = formatActiveAgo(torrent.lastActivity)
    val addedOnText = formatAddedOn(torrent.addedOn)
    val savePathText = torrent.savePath.ifBlank { "-" }
    val stateStyle = torrentStateStyle(effectiveState)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPending) { onOpenDetails() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, stateStyle.borderColor.copy(alpha = 0.58f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassSubtleContainerColor(),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = torrent.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                )
            }

            TorrentMetaHeaderRow(
                tagsText = tagsText,
                crossSeedCount = crossSeedCount,
                stateLabel = stateLabel,
                stateStyle = stateStyle,
                addedOnText = addedOnText,
                activeAgoText = activeAgoText,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LinearProgressIndicator(
                    progress = { torrent.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f),
                    color = stateStyle.progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = formatPercent(torrent.progress),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = stateStyle.progressColor,
                )
            }

            TorrentQuickStatsRow(
                torrent = torrent,
                categoryText = categoryText,
                savePathText = savePathText,
                minHeight = 96.dp,
            )
        }
    }
}

@Composable
internal fun TorrentOperationDetailCard(
    torrent: TorrentInfo,
    crossSeedCount: Int,
    isPending: Boolean,
    capabilities: ServerCapabilities,
    detailLoading: Boolean,
    detailProperties: TorrentProperties?,
    detailFiles: List<TorrentFileInfo>,
    detailTrackers: List<TorrentTracker>,
    magnetUri: String,
    categoryOptions: List<String>,
    tagOptions: List<String>,
    deleteFilesDefault: Boolean,
    deleteFilesWhenNoSeeders: Boolean,
    onCopyHash: () -> Unit,
    onCopyMagnet: (String) -> Unit,
    onExportTorrent: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onSetLocation: (String) -> Unit,
    onSetCategory: (String) -> Unit,
    onSetTags: (String, String) -> Unit,
    onSetSpeedLimit: (String, String) -> Unit,
    onSetShareRatio: (String) -> Unit,
    onReannounce: () -> Unit,
    onRecheck: () -> Unit,
    onCopyTracker: (TorrentTracker) -> Unit,
    onEditTracker: (TorrentTracker, String) -> Unit,
    onDeleteTracker: (TorrentTracker) -> Unit,
) {
    var showDeleteDialog by remember(torrent.hash) { mutableStateOf(false) }
    var deleteFilesChecked by remember(torrent.hash) { mutableStateOf(false) }
    var renameText by remember(torrent.hash) { mutableStateOf(torrent.name) }
    var locationText by remember(torrent.hash) {
        mutableStateOf(detailProperties?.savePath?.takeIf { it.isNotBlank() } ?: torrent.savePath)
    }
    var categoryTextInput by remember(torrent.hash) { mutableStateOf(torrent.category) }
    var tagsTextInput by remember(torrent.hash) { mutableStateOf(torrent.tags) }
    var downloadLimitText by remember(torrent.hash) { mutableStateOf("") }
    var uploadLimitText by remember(torrent.hash) { mutableStateOf("") }
    var ratioText by remember(torrent.hash) { mutableStateOf(formatRatio(torrent.ratio)) }
    var selectedTab by rememberSaveable(torrent.hash) { mutableIntStateOf(0) }
    var trackersPasskeyVisible by rememberSaveable(torrent.hash) { mutableStateOf(false) }
    var editingTracker by remember(torrent.hash) { mutableStateOf<TorrentTracker?>(null) }
    var editingTrackerUrl by remember(torrent.hash) { mutableStateOf("") }
    var pendingDeleteTracker by remember(torrent.hash) { mutableStateOf<TorrentTracker?>(null) }
    var fileBrowserPath by rememberSaveable(torrent.hash) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(torrent.hash, detailProperties?.downloadLimit, detailProperties?.uploadLimit) {
        val dl = detailProperties?.downloadLimit ?: 0L
        val up = detailProperties?.uploadLimit ?: 0L
        downloadLimitText = if (dl > 0L) (dl / 1024L).toString() else ""
        uploadLimitText = if (up > 0L) (up / 1024L).toString() else ""
    }
    LaunchedEffect(torrent.hash, detailProperties?.shareRatio) {
        val ratio = detailProperties?.shareRatio
        if (ratio != null && ratio >= 0.0 && ratio.isFinite()) {
            ratioText = formatRatio(ratio)
        }
    }

    val fileTree = remember(torrent.hash, detailFiles) { buildTorrentFileTree(detailFiles) }
    val fileBrowserSelection = remember(fileTree, fileBrowserPath) {
        resolveTorrentFileBrowserSelection(
            root = fileTree,
            pathSegments = fileBrowserPath,
        )
    }
    LaunchedEffect(fileBrowserSelection.pathSegments) {
        if (fileBrowserSelection.pathSegments != fileBrowserPath) {
            fileBrowserPath = fileBrowserSelection.pathSegments
        }
    }

    val effectiveState = effectiveTorrentState(torrent)
    val paused = isPausedState(effectiveState)
    val peerOverviewItems = listOf(
        stringResource(R.string.detail_peer_seeders_label) to
            stringResource(R.string.detail_peer_seeders_value, torrent.seeders, torrent.numComplete),
        stringResource(R.string.detail_peer_leechers_label) to
            stringResource(R.string.detail_peer_leechers_value, torrent.leechers, torrent.numIncomplete),
        stringResource(R.string.detail_peer_cross_seed_label) to
            stringResource(R.string.detail_peer_cross_seed_value, crossSeedCount),
        stringResource(R.string.detail_peer_ratio_label) to formatRatio(torrent.ratio),
        stringResource(R.string.detail_peer_activity_label) to formatActiveAgo(torrent.lastActivity),
    )
    val hasMutableTrackers = remember(detailTrackers) {
        detailTrackers.any { tracker -> isMutableTrackerUrl(tracker.url) }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.42f)),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = torrent.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            TabRow(selectedTabIndex = selectedTab) {
                listOf(
                    stringResource(R.string.tab_info),
                    stringResource(R.string.tab_trackers),
                    stringResource(R.string.tab_peers),
                    stringResource(R.string.tab_files),
                ).forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    TorrentDetailInfoTab(
                        torrent = torrent,
                        capabilities = capabilities,
                        isPending = isPending,
                        magnetUri = magnetUri,
                        categoryOptions = categoryOptions,
                        tagOptions = tagOptions,
                        paused = paused,
                        renameText = renameText,
                        locationText = locationText,
                        categoryTextInput = categoryTextInput,
                        tagsTextInput = tagsTextInput,
                        downloadLimitText = downloadLimitText,
                        uploadLimitText = uploadLimitText,
                        ratioText = ratioText,
                        onRenameTextChange = { renameText = it },
                        onLocationTextChange = { locationText = it },
                        onCategoryTextChange = { categoryTextInput = it },
                        onTagsTextChange = { tagsTextInput = it },
                        onDownloadLimitTextChange = { downloadLimitText = it },
                        onUploadLimitTextChange = { uploadLimitText = it },
                        onRatioTextChange = { ratioText = it },
                        onCopyHash = onCopyHash,
                        onCopyMagnet = { onCopyMagnet(magnetUri) },
                        onExportTorrent = onExportTorrent,
                        onRename = { onRename(renameText.trim()) },
                        onSetLocation = { onSetLocation(locationText.trim()) },
                        onSetCategory = { onSetCategory(categoryTextInput.trim()) },
                        onSetTags = { onSetTags(torrent.tags, tagsTextInput.trim()) },
                        onSetSpeedLimit = { onSetSpeedLimit(downloadLimitText, uploadLimitText) },
                        onSetShareRatio = { onSetShareRatio(ratioText.trim()) },
                        onReannounce = onReannounce,
                        onRecheck = onRecheck,
                        onPauseResume = { if (paused) onResume() else onPause() },
                        onRequestDelete = {
                            deleteFilesChecked = deleteFilesDefault ||
                                (deleteFilesWhenNoSeeders && torrent.seeders <= 0)
                            showDeleteDialog = true
                        },
                    )
                }
                1 -> {
                    TorrentDetailTrackersTab(
                        detailLoading = detailLoading,
                        detailTrackers = detailTrackers,
                        hasMutableTrackers = hasMutableTrackers,
                        trackersPasskeyVisible = trackersPasskeyVisible,
                        isPending = isPending,
                        supportsTrackerMutation = capabilities.supportsTrackerMutation,
                        onTogglePasskeyVisibility = {
                            trackersPasskeyVisible = !trackersPasskeyVisible
                        },
                        onCopyTracker = onCopyTracker,
                        onEditTracker = { tracker, trackerUrl ->
                            editingTracker = tracker
                            editingTrackerUrl = trackerUrl
                        },
                        onDeleteTracker = { tracker ->
                            pendingDeleteTracker = tracker
                        },
                    )
                }
                2 -> {
                    TorrentDetailPeersTab(
                        peerOverviewItems = peerOverviewItems,
                    )
                }
                3 -> {
                    TorrentDetailFilesTab(
                        detailLoading = detailLoading,
                        detailFiles = detailFiles,
                        fileBrowserPath = fileBrowserPath,
                        fileBrowserSelection = fileBrowserSelection,
                        onOpenRoot = { fileBrowserPath = emptyList() },
                        onOpenPathSegment = { index ->
                            fileBrowserPath = fileBrowserPath.take(index + 1)
                        },
                        onOpenDirectory = { node ->
                            if (node.isDirectory) {
                                fileBrowserPath = node.pathSegments
                            }
                        },
                    )
                }
            }
        }
    }

    TorrentDeleteDialog(
        visible = showDeleteDialog,
        deleteFilesChecked = deleteFilesChecked,
        isPending = isPending,
        onDeleteFilesCheckedChange = { deleteFilesChecked = it },
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            showDeleteDialog = false
            onDelete(deleteFilesChecked)
        },
    )

    val editingTrackerState = editingTracker
    TorrentEditTrackerDialog(
        tracker = editingTrackerState,
        trackerUrl = editingTrackerUrl,
        isPending = isPending,
        onTrackerUrlChange = { editingTrackerUrl = it },
        onDismiss = { editingTracker = null },
        onConfirm = { tracker ->
            editingTracker = null
            onEditTracker(tracker, editingTrackerUrl.trim())
        },
    )

    val deleteTrackerState = pendingDeleteTracker
    TorrentDeleteTrackerDialog(
        tracker = deleteTrackerState,
        isPending = isPending,
        onDismiss = { pendingDeleteTracker = null },
        onConfirm = { tracker ->
            pendingDeleteTracker = null
            onDeleteTracker(tracker)
        },
    )
}

internal enum class TorrentDetailPrimaryAction {
    ExportTorrent,
    Reannounce,
    Recheck,
}

internal fun torrentDetailPrimaryActions(capabilities: ServerCapabilities): List<TorrentDetailPrimaryAction> =
    buildList {
        if (capabilities.supportsExportTorrent) add(TorrentDetailPrimaryAction.ExportTorrent)
        if (capabilities.supportsReannounce) add(TorrentDetailPrimaryAction.Reannounce)
        if (capabilities.supportsRecheck) add(TorrentDetailPrimaryAction.Recheck)
    }

@Composable
private fun TorrentDetailInfoTab(
    torrent: TorrentInfo,
    capabilities: ServerCapabilities,
    isPending: Boolean,
    magnetUri: String,
    categoryOptions: List<String>,
    tagOptions: List<String>,
    paused: Boolean,
    renameText: String,
    locationText: String,
    categoryTextInput: String,
    tagsTextInput: String,
    downloadLimitText: String,
    uploadLimitText: String,
    ratioText: String,
    onRenameTextChange: (String) -> Unit,
    onLocationTextChange: (String) -> Unit,
    onCategoryTextChange: (String) -> Unit,
    onTagsTextChange: (String) -> Unit,
    onDownloadLimitTextChange: (String) -> Unit,
    onUploadLimitTextChange: (String) -> Unit,
    onRatioTextChange: (String) -> Unit,
    onCopyHash: () -> Unit,
    onCopyMagnet: () -> Unit,
    onExportTorrent: () -> Unit,
    onRename: () -> Unit,
    onSetLocation: () -> Unit,
    onSetCategory: () -> Unit,
    onSetTags: () -> Unit,
    onSetSpeedLimit: () -> Unit,
    onSetShareRatio: () -> Unit,
    onReannounce: () -> Unit,
    onRecheck: () -> Unit,
    onPauseResume: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(R.string.detail_section_basic),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DetailReadonlyActionRow(
            label = stringResource(R.string.detail_hash_label),
            value = torrent.hash.ifBlank { "-" },
            actionText = stringResource(R.string.copy),
            enabled = !isPending && torrent.hash.isNotBlank(),
            onAction = onCopyHash,
        )
        DetailReadonlyActionRow(
            label = stringResource(R.string.detail_magnet_label),
            value = magnetUri.ifBlank { "-" },
            actionText = stringResource(R.string.copy),
            enabled = !isPending && magnetUri.isNotBlank(),
            onAction = onCopyMagnet,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            items(
                items = torrentDetailPrimaryActions(capabilities),
                key = { action -> action.name },
            ) { action ->
                val accentColor = when (action) {
                    TorrentDetailPrimaryAction.ExportTorrent -> MaterialTheme.colorScheme.secondary
                    TorrentDetailPrimaryAction.Reannounce -> Color(0xFF4C8DFF)
                    TorrentDetailPrimaryAction.Recheck -> Color(0xFFF3A53C)
                }
                val label = when (action) {
                    TorrentDetailPrimaryAction.ExportTorrent -> stringResource(R.string.detail_export_torrent)
                    TorrentDetailPrimaryAction.Reannounce -> stringResource(R.string.detail_reannounce)
                    TorrentDetailPrimaryAction.Recheck -> stringResource(R.string.detail_recheck)
                }
                val clickAction = when (action) {
                    TorrentDetailPrimaryAction.ExportTorrent -> onExportTorrent
                    TorrentDetailPrimaryAction.Reannounce -> onReannounce
                    TorrentDetailPrimaryAction.Recheck -> onRecheck
                }
                DetailInlineActionButton(
                    text = label,
                    enabled = !isPending && torrent.hash.isNotBlank(),
                    accentColor = accentColor,
                    onClick = clickAction,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
        if (capabilities.supportsRename) {
            Text(
                stringResource(R.string.detail_section_name),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionInputRow(
                label = stringResource(R.string.detail_new_name_label),
                value = renameText,
                onValueChange = onRenameTextChange,
                actionText = stringResource(R.string.detail_action_change),
                enabled = !isPending,
                onAction = onRename,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
        }
        Text(
            stringResource(R.string.detail_section_path),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.detail_set_path_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ActionInputRow(
            label = stringResource(R.string.detail_save_path_label),
            value = locationText,
            onValueChange = onLocationTextChange,
            actionText = stringResource(R.string.detail_action_change),
            enabled = !isPending,
            onAction = onSetLocation,
        )
        if (capabilities.supportsCategories) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
            Text(
                stringResource(R.string.detail_section_category),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (categoryOptions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    items(categoryOptions, key = { it }) { option ->
                        TorrentMetaChip(
                            text = option,
                            containerColor = if (option == categoryTextInput) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                            contentColor = Color(0xFFEAF0FF),
                            onClick = { onCategoryTextChange(option) },
                        )
                    }
                }
            }
            ActionInputRow(
                label = stringResource(R.string.detail_category_label),
                value = categoryTextInput,
                onValueChange = onCategoryTextChange,
                actionText = stringResource(R.string.detail_action_change),
                enabled = !isPending,
                onAction = onSetCategory,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
        Text(
            stringResource(R.string.detail_section_tags),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tagOptions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                items(tagOptions, key = { it }) { option ->
                    val selected = parseTags(tagsTextInput).contains(option)
                    TorrentMetaChip(
                        text = option,
                        containerColor = if (selected) Color(0xFF5D7CFF) else Color(0xFF4D4D4D),
                        contentColor = Color(0xFFEAF0FF),
                        onClick = { onTagsTextChange(toggleTag(tagsTextInput, option)) },
                    )
                }
            }
        }
        ActionInputRow(
            label = stringResource(R.string.detail_tags_label),
            value = tagsTextInput,
            onValueChange = onTagsTextChange,
            actionText = stringResource(R.string.detail_action_change),
            enabled = !isPending,
            onAction = onSetTags,
        )
        if (capabilities.supportsPerTorrentSpeedLimit) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
            Text(
                stringResource(R.string.detail_section_speed_limit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = downloadLimitText,
                    onValueChange = onDownloadLimitTextChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.detail_download_kb_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isPending,
                )
                OutlinedTextField(
                    value = uploadLimitText,
                    onValueChange = onUploadLimitTextChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.detail_upload_kb_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isPending,
                )
                TextButton(
                    onClick = onSetSpeedLimit,
                    enabled = !isPending,
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                    ),
                ) {
                    Text(stringResource(R.string.detail_action_apply))
                }
            }
        }
        if (capabilities.supportsShareRatio) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
            Text(
                stringResource(R.string.detail_section_ratio),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionInputRow(
                label = stringResource(R.string.detail_ratio_label),
                value = ratioText,
                onValueChange = onRatioTextChange,
                actionText = stringResource(R.string.detail_action_apply),
                enabled = !isPending,
                onAction = onSetShareRatio,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(
                onClick = onPauseResume,
                enabled = !isPending,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            ) {
                Text(if (paused) stringResource(R.string.resume) else stringResource(R.string.pause))
            }
            TextButton(
                onClick = onRequestDelete,
                enabled = !isPending,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun TorrentDeleteDialog(
    visible: Boolean,
    deleteFilesChecked: Boolean,
    isPending: Boolean,
    onDeleteFilesCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PanelShape,
        containerColor = qbGlassStrongContainerColor(),
        title = { Text(stringResource(R.string.delete_torrent_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.delete_torrent_desc))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = deleteFilesChecked,
                        onCheckedChange = onDeleteFilesCheckedChange,
                    )
                    Text(stringResource(R.string.delete_files))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isPending,
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TorrentEditTrackerDialog(
    tracker: TorrentTracker?,
    trackerUrl: String,
    isPending: Boolean,
    onTrackerUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (TorrentTracker) -> Unit,
) {
    if (tracker == null) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PanelShape,
        containerColor = qbGlassStrongContainerColor(),
        title = { Text(stringResource(R.string.detail_tracker_edit_title)) },
        text = {
            OutlinedTextField(
                value = trackerUrl,
                onValueChange = onTrackerUrlChange,
                label = { Text(stringResource(R.string.detail_tracker_url_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isPending,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tracker) },
                enabled = !isPending && trackerUrl.trim().isNotBlank(),
            ) {
                Text(stringResource(R.string.server_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TorrentDeleteTrackerDialog(
    tracker: TorrentTracker?,
    isPending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TorrentTracker) -> Unit,
) {
    if (tracker == null) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PanelShape,
        containerColor = qbGlassStrongContainerColor(),
        title = { Text(stringResource(R.string.detail_tracker_delete_title)) },
        text = { Text(stringResource(R.string.detail_tracker_delete_desc)) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tracker) },
                enabled = !isPending,
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TorrentDetailTrackersTab(
    detailLoading: Boolean,
    detailTrackers: List<TorrentTracker>,
    hasMutableTrackers: Boolean,
    trackersPasskeyVisible: Boolean,
    isPending: Boolean,
    supportsTrackerMutation: Boolean,
    onTogglePasskeyVisibility: () -> Unit,
    onCopyTracker: (TorrentTracker) -> Unit,
    onEditTracker: (TorrentTracker, String) -> Unit,
    onDeleteTracker: (TorrentTracker) -> Unit,
) {
    if (detailLoading && detailTrackers.isEmpty()) {
        Text(
            stringResource(R.string.loading),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TorrentMetaChip(
            text = pluralStringResource(
                R.plurals.detail_tracker_count,
                detailTrackers.size,
                detailTrackers.size,
            ),
            containerColor = Color(0xFF6C3FD3),
            contentColor = Color.White,
        )
        if (hasMutableTrackers) {
            TextButton(onClick = onTogglePasskeyVisibility) {
                Icon(
                    painter = painterResource(
                        id = if (trackersPasskeyVisible) R.drawable.ic_password_hidden else R.drawable.ic_password_visible,
                    ),
                    contentDescription = stringResource(
                        if (trackersPasskeyVisible) R.string.detail_hide_passkey else R.string.detail_show_passkey,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (detailTrackers.isEmpty()) {
        Text(
            text = stringResource(R.string.no_tracker_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        detailTrackers.forEach { tracker ->
            val trackerUrl = tracker.url
            val isMutableTracker = isMutableTrackerUrl(trackerUrl)
            val canMutateTracker =
                !isPending &&
                    supportsTrackerMutation &&
                    isMutableTracker &&
                    trackerUrl.isNotBlank()
            TrackerInfoCard(
                tracker = tracker,
                displayUrl = when {
                    !isMutableTracker -> trackerUrl.ifBlank { "-" }
                    trackersPasskeyVisible -> trackerUrl.ifBlank { "-" }
                    else -> maskTrackerUrl(trackerUrl)
                },
                allowMutation = supportsTrackerMutation && isMutableTracker,
                onCopy = if (!isPending && isMutableTracker && trackerUrl.isNotBlank()) {
                    { onCopyTracker(tracker) }
                } else {
                    null
                },
                onEdit = if (canMutateTracker) {
                    { onEditTracker(tracker, trackerUrl) }
                } else {
                    null
                },
                onDelete = if (canMutateTracker) {
                    { onDeleteTracker(tracker) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun TorrentDetailPeersTab(
    peerOverviewItems: List<Pair<String, String>>,
) {
    TorrentUnifiedInfoPanel(
        items = peerOverviewItems,
        style = TorrentUnifiedInfoPanelStyle.Summary,
    )
}

@Composable
private fun TorrentDetailFilesTab(
    detailLoading: Boolean,
    detailFiles: List<TorrentFileInfo>,
    fileBrowserPath: List<String>,
    fileBrowserSelection: TorrentFileBrowserSelection,
    onOpenRoot: () -> Unit,
    onOpenPathSegment: (Int) -> Unit,
    onOpenDirectory: (TorrentFileTreeNode) -> Unit,
) {
    if (detailLoading) {
        Text(
            stringResource(R.string.loading_files),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (detailFiles.isEmpty()) {
        Text(
            stringResource(R.string.no_file_details),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            item {
                TorrentMetaChip(
                    text = stringResource(R.string.detail_files_root),
                    containerColor = if (fileBrowserPath.isEmpty()) Color(0xFF4469FF) else Color(0xFF2E3340),
                    contentColor = Color.White,
                    onClick = onOpenRoot,
                )
            }
            fileBrowserPath.forEachIndexed { index, segment ->
                item {
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                item {
                    TorrentMetaChip(
                        text = segment,
                        containerColor = if (index == fileBrowserPath.lastIndex) Color(0xFF5D7CFF) else Color(0xFF2E3340),
                        contentColor = Color.White,
                        onClick = { onOpenPathSegment(index) },
                    )
                }
            }
        }
        fileBrowserSelection.node.children.forEach { node ->
            TorrentFileBrowserNodeCard(
                node = node,
                onOpenDirectory = {
                    if (node.isDirectory) {
                        onOpenDirectory(node)
                    }
                },
            )
        }
    }
}

internal data class TorrentFileTreeNode(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val size: Long,
    val progress: Float,
    val fileCount: Int,
    val orderIndex: Int,
    val pathSegments: List<String>,
    val children: List<TorrentFileTreeNode> = emptyList(),
)

internal data class TorrentFileBrowserSelection(
    val node: TorrentFileTreeNode,
    val pathSegments: List<String>,
)

private class MutableTorrentFileTreeNode(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val orderIndex: Int,
    val pathSegments: List<String>,
) {
    val children = linkedMapOf<String, MutableTorrentFileTreeNode>()
    var size: Long = 0L
    var weightedProgress: Double = 0.0
    var fileCount: Int = 0
}

internal fun buildTorrentFileTree(files: List<TorrentFileInfo>): TorrentFileTreeNode {
    val root = MutableTorrentFileTreeNode(
        name = "",
        fullPath = "",
        isDirectory = true,
        orderIndex = Int.MIN_VALUE,
        pathSegments = emptyList(),
    )
    files.forEachIndexed { fallbackIndex, file ->
        val cleanPath = file.name.trim().replace('\\', '/')
        if (cleanPath.isBlank()) return@forEachIndexed
        val segments = cleanPath.split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) return@forEachIndexed
        val clampedProgress = file.progress.coerceIn(0f, 1f).toDouble()
        val weightedProgress = file.size.toDouble() * clampedProgress
        var current = root
        current.size += file.size
        current.weightedProgress += weightedProgress
        current.fileCount += 1
        segments.forEachIndexed { index, segment ->
            val isFile = index == segments.lastIndex
            val currentPathSegments = segments.take(index + 1)
            val fullPath = currentPathSegments.joinToString("/")
            val child = current.children.getOrPut(segment) {
                MutableTorrentFileTreeNode(
                    name = segment,
                    fullPath = fullPath,
                    isDirectory = !isFile,
                    orderIndex = if (file.index >= 0) file.index else fallbackIndex,
                    pathSegments = currentPathSegments,
                )
            }
            if (isFile) {
                child.size = file.size
                child.weightedProgress = weightedProgress
                child.fileCount = 1
            } else {
                child.size += file.size
                child.weightedProgress += weightedProgress
                child.fileCount += 1
            }
            current = child
        }
    }
    return root.toImmutableNode()
}

private fun MutableTorrentFileTreeNode.toImmutableNode(): TorrentFileTreeNode {
    val safeSize = size.coerceAtLeast(0L)
    val progress = if (safeSize > 0L) {
        (weightedProgress / safeSize.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    return TorrentFileTreeNode(
        name = name,
        fullPath = fullPath,
        isDirectory = isDirectory,
        size = safeSize,
        progress = progress,
        fileCount = fileCount.coerceAtLeast(if (isDirectory) 0 else 1),
        orderIndex = orderIndex,
        pathSegments = pathSegments,
        children = children.values
            .sortedWith(
                compareBy<MutableTorrentFileTreeNode> { !it.isDirectory }
                    .thenBy { it.orderIndex }
                    .thenBy { it.name.lowercase() },
            )
            .map { it.toImmutableNode() },
    )
}

private fun resolveTorrentFileTreeNode(
    root: TorrentFileTreeNode,
    pathSegments: List<String>,
): TorrentFileTreeNode? {
    var current = root
    pathSegments.forEach { segment ->
        current = current.children.firstOrNull { it.name == segment } ?: return null
    }
    return current
}

internal fun resolveTorrentFileBrowserSelection(
    root: TorrentFileTreeNode,
    pathSegments: List<String>,
): TorrentFileBrowserSelection {
    val resolvedNode = resolveTorrentFileTreeNode(root, pathSegments)
    return if (resolvedNode != null) {
        TorrentFileBrowserSelection(
            node = resolvedNode,
            pathSegments = pathSegments,
        )
    } else {
        TorrentFileBrowserSelection(
            node = root,
            pathSegments = emptyList(),
        )
    }
}

@Composable
private fun TorrentFileBrowserNodeCard(
    node: TorrentFileTreeNode,
    onOpenDirectory: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = node.isDirectory, onClick = onOpenDirectory),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.28f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassSubtleContainerColor(),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                TorrentMetaChip(
                    text = stringResource(
                        if (node.isDirectory) {
                            R.string.detail_files_folder_chip
                        } else {
                            R.string.detail_files_file_chip
                        }
                    ),
                    containerColor = if (node.isDirectory) Color(0xFF2D74F7) else Color(0xFF3E4656),
                    contentColor = Color.White,
                )
                Text(
                    text = node.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (node.isDirectory) {
                    pluralStringResource(
                        R.plurals.detail_files_folder_summary,
                        node.fileCount,
                        node.fileCount,
                        formatBytes(node.size),
                        formatPercent(node.progress),
                    )
                } else {
                    stringResource(
                        R.string.detail_files_file_summary,
                        formatBytes(node.size),
                        formatPercent(node.progress),
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { node.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (node.isDirectory) Color(0xFF4C8DFF) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailInlineActionButton(
    text: String,
    enabled: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.background(
            color = accentColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
        ),
    ) {
        Text(
            text = text,
            color = accentColor,
        )
    }
}

@Composable
internal fun ActionInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    actionText: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(label) },
            enabled = enabled,
        )
        TextButton(
            onClick = onAction,
            enabled = enabled,
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
            ),
        ) {
            Text(actionText)
        }
    }
}

@Composable
internal fun DetailReadonlyActionRow(
    label: String,
    value: String,
    actionText: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.weight(1f),
            singleLine = true,
            readOnly = true,
            label = { Text(label) },
        )
        TextButton(
            onClick = onAction,
            enabled = enabled,
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
            ),
        ) {
            Text(actionText)
        }
    }
}
