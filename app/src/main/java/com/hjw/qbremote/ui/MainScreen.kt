package com.hjw.qbremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val visibleTorrents = remember(
        state.torrents,
        state.selectedFilter,
        state.selectedSort,
        state.sortDescending,
    ) {
        val filtered = state.torrents.filter { state.selectedFilter.matches(it) }
        sortTorrents(filtered, state.selectedSort, state.sortDescending)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("qB Remote") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ConnectionCard(
                    state = state,
                    onHostChange = viewModel::updateHost,
                    onPortChange = viewModel::updatePort,
                    onHttpsChange = viewModel::updateUseHttps,
                    onUserChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onRefreshSecondsChange = viewModel::updateRefreshSeconds,
                    onConnect = viewModel::connect,
                )
            }

            if (state.connected) {
                item {
                    TransferSummaryCard(
                        transferInfo = state.transferInfo,
                        torrentCount = state.torrents.size,
                        refreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                    )
                }

                item {
                    FilterRow(
                        selected = state.selectedFilter,
                        onSelect = viewModel::setFilter,
                    )
                }

                item {
                    SortRow(
                        selected = state.selectedSort,
                        descending = state.sortDescending,
                        onSelect = viewModel::setSort,
                        onToggleDirection = viewModel::toggleSortDirection,
                    )
                }

                items(
                    items = visibleTorrents,
                    key = { it.hash.ifBlank { it.name } },
                ) { torrent ->
                    TorrentCard(
                        torrent = torrent,
                        isPending = state.pendingHashes.contains(torrent.hash),
                        onPause = { viewModel.pauseTorrent(torrent.hash) },
                        onResume = { viewModel.resumeTorrent(torrent.hash) },
                        onDelete = { deleteFiles ->
                            viewModel.deleteTorrent(torrent.hash, deleteFiles)
                        },
                    )
                }
            } else {
                item {
                    Text(
                        text = "Connect first to load torrent list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    state: MainUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onHttpsChange: (Boolean) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefreshSecondsChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Connection", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.host,
                onValueChange = onHostChange,
                singleLine = true,
                label = { Text("Host / IP") },
                placeholder = { Text("Example: 192.168.1.12") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.width(120.dp),
                    value = if (state.settings.port == 0) "" else state.settings.port.toString(),
                    onValueChange = onPortChange,
                    singleLine = true,
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.settings.refreshSeconds.toString(),
                    onValueChange = onRefreshSecondsChange,
                    singleLine = true,
                    label = { Text("Refresh (s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.username,
                onValueChange = onUserChange,
                singleLine = true,
                label = { Text("Username") },
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.password,
                onValueChange = onPasswordChange,
                singleLine = true,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("HTTPS")
                Switch(
                    checked = state.settings.useHttps,
                    onCheckedChange = onHttpsChange,
                    modifier = Modifier.padding(start = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onConnect, enabled = !state.isConnecting) {
                        Text(if (state.isConnecting) "Connecting..." else "Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferSummaryCard(
    transferInfo: TransferInfo,
    torrentCount: Int,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Global transfer", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onRefresh, enabled = !refreshing) {
                        Text(if (refreshing) "Refreshing..." else "Refresh")
                    }
                }
            }
            Text("Down: ${formatSpeed(transferInfo.downloadSpeed)}")
            Text("Up: ${formatSpeed(transferInfo.uploadSpeed)}")
            Text("Total down: ${formatBytes(transferInfo.downloadedTotal)}")
            Text("Total up: ${formatBytes(transferInfo.uploadedTotal)}")
            Text("DHT: ${transferInfo.dhtNodes}  |  Torrents: $torrentCount")
        }
    }
}

@Composable
private fun FilterRow(
    selected: TorrentFilter,
    onSelect: (TorrentFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(TorrentFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun TorrentCard(
    torrent: TorrentInfo,
    isPending: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    var showDeleteDialog by remember(torrent.hash) { mutableStateOf(false) }
    val paused = isPausedState(torrent.state)
    val canPause = !paused && isActiveTransferState(torrent.state)
    val canResume = paused

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = torrent.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(stateLabel(torrent.state)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                )

                val tagLabel = torrent.tags.ifBlank { "No tags" }
                AssistChip(
                    onClick = {},
                    label = { Text(tagLabel) },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(formatPercent(torrent.progress), fontWeight = FontWeight.Medium)
                }
            }

            LinearProgressIndicator(
                progress = { torrent.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (canPause) {
                    TextButton(onClick = onPause, enabled = !isPending) {
                        Text("Pause")
                    }
                }
                if (canResume) {
                    TextButton(onClick = onResume, enabled = !isPending) {
                        Text("Resume")
                    }
                }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isPending,
                ) {
                    Text("Delete")
                }
            }

            HorizontalDivider()
            Text("Size: ${formatBytes(torrent.size)}  Downloaded: ${formatBytes(torrent.downloaded)}")
            Text("Up ${formatSpeed(torrent.uploadSpeed)}   Down ${formatSpeed(torrent.downloadSpeed)}")
            Text("Seed/Leech: ${torrent.seeders}/${torrent.leechers}   Complete/Incomp: ${torrent.numComplete}/${torrent.numIncomplete}")
            Text("Added: ${formatAddedOn(torrent.addedOn)}")
            if (torrent.lastActivity > 0) {
                Text("Last activity: ${formatAddedOn(torrent.lastActivity)}")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete torrent?") },
            text = {
                Text("You can remove only the task, or remove both task and downloaded files.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(true)
                    },
                    enabled = !isPending,
                ) {
                    Text("Delete files")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(false)
                        },
                        enabled = !isPending,
                    ) {
                        Text("Keep files")
                    }
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
        )
    }
}

@Composable
private fun SortRow(
    selected: TorrentSort,
    descending: Boolean,
    onSelect: (TorrentSort) -> Unit,
    onToggleDirection: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sort", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onToggleDirection) {
                    Text(if (descending) "DESC" else "ASC")
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(TorrentSort.entries) { sort ->
                FilterChip(
                    selected = selected == sort,
                    onClick = { onSelect(sort) },
                    label = { Text(sort.label) },
                )
            }
        }
    }
}

private fun isPausedState(state: String): Boolean {
    return state.lowercase() in setOf("pauseddl", "pausedup")
}

private fun isActiveTransferState(state: String): Boolean {
    return state.lowercase() in setOf(
        "downloading", "forceddl", "stalleddl", "metadl",
        "uploading", "forcedup", "stalledup"
    )
}

private fun sortTorrents(
    torrents: List<TorrentInfo>,
    sort: TorrentSort,
    descending: Boolean,
): List<TorrentInfo> {
    val comparator = when (sort) {
        TorrentSort.ACTIVITY_TIME -> compareBy<TorrentInfo> { it.lastActivity }
        TorrentSort.ADDED_TIME -> compareBy<TorrentInfo> { it.addedOn }
        TorrentSort.DOWNLOAD_SPEED -> compareBy<TorrentInfo> { it.downloadSpeed }
        TorrentSort.UPLOAD_SPEED -> compareBy<TorrentInfo> { it.uploadSpeed }
    }
    return if (descending) torrents.sortedWith(comparator.reversed()) else torrents.sortedWith(comparator)
}
