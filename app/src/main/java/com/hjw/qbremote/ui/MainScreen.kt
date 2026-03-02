package com.hjw.qbremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.R
import com.hjw.qbremote.data.ChartSortMode
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import java.net.URI

private data class SiteChartEntry(
    val site: String,
    val torrentCount: Int,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val totalSpeed: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val visibleTorrents = remember(
        state.torrents,
        state.selectedFilter,
        state.selectedSort,
        state.sortDescending,
    ) {
        val filtered = state.torrents.filter { state.selectedFilter.matches(it) }
        sortTorrents(filtered, state.selectedSort, state.sortDescending)
    }
    val groupedTorrents = remember(visibleTorrents, state.settings.enableServerGrouping) {
        if (!state.settings.enableServerGrouping) {
            emptyList()
        } else {
            visibleTorrents
                .groupBy { trackerSiteName(it.tracker) }
                .toList()
                .sortedByDescending { (_, torrents) -> torrents.size }
        }
    }
    val chartEntries = remember(state.torrents, state.settings.chartSortMode) {
        buildSiteChartEntries(state.torrents, state.settings.chartSortMode)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.top_title)) },
                actions = {
                    TextButton(onClick = { showSettingsDialog = true }) {
                        Text(stringResource(R.string.settings_title))
                    }
                },
            )
        },
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
                        showTotals = state.settings.showSpeedTotals,
                        onRefresh = viewModel::refresh,
                    )
                }

                if (state.settings.showChartPanel) {
                    item {
                        ChartPanelCard(
                            entries = chartEntries,
                            chartSortMode = state.settings.chartSortMode,
                            showSiteName = state.settings.chartShowSiteName,
                        )
                    }
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

                if (state.settings.enableServerGrouping) {
                    groupedTorrents.forEach { (site, siteTorrents) ->
                        item(key = "group_header_$site") {
                            Text(
                                text = stringResource(R.string.site_group_header, site, siteTorrents.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }

                        items(
                            items = siteTorrents,
                            key = { "${site}_${it.hash.ifBlank { it.name }}" },
                        ) { torrent ->
                            TorrentCard(
                                torrent = torrent,
                                isPending = state.pendingHashes.contains(torrent.hash),
                                deleteFilesDefault = state.settings.deleteFilesDefault,
                                deleteFilesWhenNoSeeders = state.settings.deleteFilesWhenNoSeeders,
                                onPause = { viewModel.pauseTorrent(torrent.hash) },
                                onResume = { viewModel.resumeTorrent(torrent.hash) },
                                onDelete = { deleteFiles ->
                                    viewModel.deleteTorrent(torrent.hash, deleteFiles)
                                },
                            )
                        }
                    }
                } else {
                    items(
                        items = visibleTorrents,
                        key = { it.hash.ifBlank { it.name } },
                    ) { torrent ->
                        TorrentCard(
                            torrent = torrent,
                            isPending = state.pendingHashes.contains(torrent.hash),
                            deleteFilesDefault = state.settings.deleteFilesDefault,
                            deleteFilesWhenNoSeeders = state.settings.deleteFilesWhenNoSeeders,
                            onPause = { viewModel.pauseTorrent(torrent.hash) },
                            onResume = { viewModel.resumeTorrent(torrent.hash) },
                            onDelete = { deleteFiles ->
                                viewModel.deleteTorrent(torrent.hash, deleteFiles)
                            },
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.connect_first_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = state.settings,
            onDismiss = { showSettingsDialog = false },
            onAppLanguageChange = viewModel::updateAppLanguage,
            onShowSpeedTotalsChange = viewModel::updateShowSpeedTotals,
            onEnableServerGroupingChange = viewModel::updateEnableServerGrouping,
            onShowChartPanelChange = viewModel::updateShowChartPanel,
            onChartShowSiteNameChange = viewModel::updateChartShowSiteName,
            onChartSortModeChange = viewModel::updateChartSortMode,
            onDeleteFilesWhenNoSeedersChange = viewModel::updateDeleteFilesWhenNoSeeders,
            onDeleteFilesDefaultChange = viewModel::updateDeleteFilesDefault,
        )
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
            Text(stringResource(R.string.connection_title), fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.host,
                onValueChange = onHostChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_host_label)) },
                placeholder = { Text(stringResource(R.string.connection_host_hint)) },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.width(120.dp),
                    value = if (state.settings.port == 0) "" else state.settings.port.toString(),
                    onValueChange = onPortChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_port_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.settings.refreshSeconds.toString(),
                    onValueChange = onRefreshSecondsChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_refresh_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.username,
                onValueChange = onUserChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_username_label)) },
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.settings.password,
                onValueChange = onPasswordChange,
                singleLine = true,
                label = { Text(stringResource(R.string.connection_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.connection_https_label))
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
                        Text(
                            if (state.isConnecting) {
                                stringResource(R.string.connecting)
                            } else {
                                stringResource(R.string.connect)
                            }
                        )
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
    showTotals: Boolean,
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
                Text(stringResource(R.string.global_transfer_title), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onRefresh, enabled = !refreshing) {
                        Text(
                            if (refreshing) {
                                stringResource(R.string.refreshing)
                            } else {
                                stringResource(R.string.refresh)
                            }
                        )
                    }
                }
            }
            Text(stringResource(R.string.global_down_fmt, formatSpeed(transferInfo.downloadSpeed)))
            Text(stringResource(R.string.global_up_fmt, formatSpeed(transferInfo.uploadSpeed)))
            if (showTotals) {
                Text(stringResource(R.string.global_total_down_fmt, formatBytes(transferInfo.downloadedTotal)))
                Text(stringResource(R.string.global_total_up_fmt, formatBytes(transferInfo.uploadedTotal)))
            }
            Text(stringResource(R.string.global_dht_torrents_fmt, transferInfo.dhtNodes, torrentCount))
        }
    }
}

@Composable
private fun ChartPanelCard(
    entries: List<SiteChartEntry>,
    chartSortMode: ChartSortMode,
    showSiteName: Boolean,
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
            Text(
                text = stringResource(R.string.chart_panel_title_fmt, chartSortModeLabel(chartSortMode)),
                fontWeight = FontWeight.SemiBold,
            )

            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.chart_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val topEntries = entries.take(10)
            val maxMetric = topEntries.maxOfOrNull { chartMetric(it, chartSortMode) }?.coerceAtLeast(1L) ?: 1L
            topEntries.forEachIndexed { index, entry ->
                val metric = chartMetric(entry, chartSortMode)
                val label = if (showSiteName) entry.site else "#${index + 1}"
                val metricText = chartMetricText(entry, chartSortMode)
                val progress = (metric.toFloat() / maxMetric.toFloat()).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Column(modifier = Modifier.weight(0.55f)) {
                        Text(text = metricText, style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
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
                label = { Text(filterLabel(filter)) },
            )
        }
    }
}

@Composable
private fun TorrentCard(
    torrent: TorrentInfo,
    isPending: Boolean,
    deleteFilesDefault: Boolean,
    deleteFilesWhenNoSeeders: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    var showDeleteDialog by remember(torrent.hash) { mutableStateOf(false) }
    var deleteFilesChecked by remember(torrent.hash) { mutableStateOf(false) }
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
                    label = { Text(localizedTorrentStateLabel(torrent.state)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                )

                val tagLabel = torrent.tags.ifBlank { stringResource(R.string.no_tags) }
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
                        Text(stringResource(R.string.pause))
                    }
                }
                if (canResume) {
                    TextButton(onClick = onResume, enabled = !isPending) {
                        Text(stringResource(R.string.resume))
                    }
                }
                TextButton(
                    onClick = {
                        deleteFilesChecked = deleteFilesDefault ||
                            (deleteFilesWhenNoSeeders && torrent.seeders <= 0)
                        showDeleteDialog = true
                    },
                    enabled = !isPending,
                ) {
                    Text(stringResource(R.string.delete))
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.site_fmt, trackerSiteName(torrent.tracker)))
            Text(stringResource(R.string.size_downloaded_fmt, formatBytes(torrent.size), formatBytes(torrent.downloaded)))
            Text(stringResource(R.string.up_down_fmt, formatSpeed(torrent.uploadSpeed), formatSpeed(torrent.downloadSpeed)))
            Text(stringResource(R.string.seed_leech_complete_fmt, torrent.seeders, torrent.leechers, torrent.numComplete, torrent.numIncomplete))
            Text(stringResource(R.string.added_fmt, formatAddedOn(torrent.addedOn)))
            if (torrent.lastActivity > 0) {
                Text(stringResource(R.string.last_activity_fmt, formatAddedOn(torrent.lastActivity)))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
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
                            onCheckedChange = { deleteFilesChecked = it },
                        )
                        Text(stringResource(R.string.delete_files))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(deleteFilesChecked)
                    },
                    enabled = !isPending,
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
            Text(stringResource(R.string.sort_title), fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onToggleDirection) {
                    Text(if (descending) stringResource(R.string.sort_desc) else stringResource(R.string.sort_asc))
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
                    label = { Text(sortLabel(sort)) },
                )
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    settings: ConnectionSettings,
    onDismiss: () -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowSpeedTotalsChange: (Boolean) -> Unit,
    onEnableServerGroupingChange: (Boolean) -> Unit,
    onShowChartPanelChange: (Boolean) -> Unit,
    onChartShowSiteNameChange: (Boolean) -> Unit,
    onChartSortModeChange: (ChartSortMode) -> Unit,
    onDeleteFilesWhenNoSeedersChange: (Boolean) -> Unit,
    onDeleteFilesDefaultChange: (Boolean) -> Unit,
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showChartSortMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(appLanguageLabel(settings.appLanguage))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                        ) {
                            AppLanguage.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(appLanguageLabel(language)) },
                                    onClick = {
                                        onAppLanguageChange(language)
                                        showLanguageMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_speed_totals),
                    checked = settings.showSpeedTotals,
                    onCheckedChange = onShowSpeedTotalsChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_enable_server_grouping),
                    checked = settings.enableServerGrouping,
                    onCheckedChange = onEnableServerGroupingChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_chart_panel),
                    checked = settings.showChartPanel,
                    onCheckedChange = onShowChartPanelChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_site_name),
                    checked = settings.chartShowSiteName,
                    onCheckedChange = onChartShowSiteNameChange,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_chart_sort_mode),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { showChartSortMenu = true }) {
                            Text(chartSortModeLabel(settings.chartSortMode))
                        }
                        DropdownMenu(
                            expanded = showChartSortMenu,
                            onDismissRequest = { showChartSortMenu = false },
                        ) {
                            ChartSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(chartSortModeLabel(mode)) },
                                    onClick = {
                                        onChartSortModeChange(mode)
                                        showChartSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                SettingSwitchRow(
                    title = stringResource(R.string.settings_delete_when_no_seeders),
                    checked = settings.deleteFilesWhenNoSeeders,
                    onCheckedChange = onDeleteFilesWhenNoSeedersChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_delete_by_default),
                    checked = settings.deleteFilesDefault,
                    onCheckedChange = onDeleteFilesDefaultChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun filterLabel(filter: TorrentFilter): String {
    return when (filter) {
        TorrentFilter.ALL -> stringResource(R.string.filter_all)
        TorrentFilter.DOWNLOADING -> stringResource(R.string.filter_downloading)
        TorrentFilter.SEEDING -> stringResource(R.string.filter_seeding)
        TorrentFilter.PAUSED -> stringResource(R.string.filter_paused)
        TorrentFilter.COMPLETED -> stringResource(R.string.filter_completed)
        TorrentFilter.ERROR -> stringResource(R.string.filter_error)
    }
}

@Composable
private fun sortLabel(sort: TorrentSort): String {
    return when (sort) {
        TorrentSort.ACTIVITY_TIME -> stringResource(R.string.sort_activity_time)
        TorrentSort.ADDED_TIME -> stringResource(R.string.sort_added_time)
        TorrentSort.DOWNLOAD_SPEED -> stringResource(R.string.sort_download_speed)
        TorrentSort.UPLOAD_SPEED -> stringResource(R.string.sort_upload_speed)
    }
}

@Composable
private fun localizedTorrentStateLabel(state: String): String {
    return when (state.lowercase()) {
        "downloading", "stalleddl", "forceddl", "metadl" -> stringResource(R.string.state_downloading)
        "uploading", "stalledup", "forcedup" -> stringResource(R.string.state_seeding)
        "pauseddl", "pausedup" -> stringResource(R.string.state_paused)
        "error", "missingfiles" -> stringResource(R.string.state_error)
        "queueddl", "queuedup" -> stringResource(R.string.state_queued)
        else -> state
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

private fun buildSiteChartEntries(
    torrents: List<TorrentInfo>,
    mode: ChartSortMode,
): List<SiteChartEntry> {
    val grouped = torrents.groupBy { trackerSiteName(it.tracker) }
    return grouped.map { (site, list) ->
        val down = list.sumOf { it.downloadSpeed }
        val up = list.sumOf { it.uploadSpeed }
        SiteChartEntry(
            site = site,
            torrentCount = list.size,
            downloadSpeed = down,
            uploadSpeed = up,
            totalSpeed = down + up,
        )
    }.sortedByDescending { chartMetric(it, mode) }
}

private fun chartMetric(entry: SiteChartEntry, mode: ChartSortMode): Long {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> entry.totalSpeed
        ChartSortMode.DOWNLOAD_SPEED -> entry.downloadSpeed
        ChartSortMode.UPLOAD_SPEED -> entry.uploadSpeed
        ChartSortMode.TORRENT_COUNT -> entry.torrentCount.toLong()
    }
}

@Composable
private fun chartMetricText(entry: SiteChartEntry, mode: ChartSortMode): String {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> stringResource(
            R.string.chart_metric_total_fmt,
            formatSpeed(entry.totalSpeed)
        )
        ChartSortMode.DOWNLOAD_SPEED -> stringResource(
            R.string.chart_metric_down_fmt,
            formatSpeed(entry.downloadSpeed)
        )
        ChartSortMode.UPLOAD_SPEED -> stringResource(
            R.string.chart_metric_up_fmt,
            formatSpeed(entry.uploadSpeed)
        )
        ChartSortMode.TORRENT_COUNT -> stringResource(
            R.string.chart_metric_torrents_fmt,
            entry.torrentCount
        )
    }
}

@Composable
private fun chartSortModeLabel(mode: ChartSortMode): String {
    return when (mode) {
        ChartSortMode.TOTAL_SPEED -> stringResource(R.string.chart_sort_total_speed)
        ChartSortMode.DOWNLOAD_SPEED -> stringResource(R.string.chart_sort_download_speed)
        ChartSortMode.UPLOAD_SPEED -> stringResource(R.string.chart_sort_upload_speed)
        ChartSortMode.TORRENT_COUNT -> stringResource(R.string.chart_sort_torrent_count)
    }
}

@Composable
private fun appLanguageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        AppLanguage.ZH_CN -> stringResource(R.string.settings_language_zh_cn)
        AppLanguage.EN -> stringResource(R.string.settings_language_en)
    }
}

private fun trackerSiteName(tracker: String): String {
    val trimmed = tracker.trim()
    if (trimmed.isBlank()) return "Unknown"

    return runCatching {
        URI(trimmed).host.orEmpty().ifBlank { "Unknown" }
    }.getOrElse {
        trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .ifBlank { "Unknown" }
    }
}
