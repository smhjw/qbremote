package com.hjw.qbremote.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.R
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import com.hjw.qbremote.ui.theme.qbGlassCardColors
import com.hjw.qbremote.ui.theme.qbGlassChipColor
import com.hjw.qbremote.ui.theme.qbGlassOutlineColor
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor
import com.hjw.qbremote.ui.theme.qbGlassSubtleContainerColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun buildSortedDistinctTrimmedStrings(values: List<String>): List<String> {
    return values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

internal fun buildPageThemeSignature(
    appTheme: com.hjw.qbremote.data.AppTheme,
    customBackgroundToneIsLight: Boolean,
    customBackgroundImagePath: String,
): String {
    return listOf(
        appTheme.name,
        customBackgroundToneIsLight.toString(),
        customBackgroundImagePath.trim(),
    ).joinToString("|")
}

internal data class WalletCardPalette(
    val background: Brush,
    val accent: Color,
)

internal fun walletCardPalette(
    stackedIndex: Int,
): WalletCardPalette {
    val palettes = listOf(
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF16213F),
                    Color(0xFF3458B8),
                    Color(0xFF5E89FF),
                ),
            ),
            accent = Color(0xFFEAF2FF),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF3A1C0F),
                    Color(0xFF9B5223),
                    Color(0xFFE79A45),
                ),
            ),
            accent = Color(0xFFFFF3E7),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF112C26),
                    Color(0xFF24755D),
                    Color(0xFF52C79B),
                ),
            ),
            accent = Color(0xFFE9FFF5),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF2B183B),
                    Color(0xFF7A46B1),
                    Color(0xFFB97DFF),
                ),
            ),
            accent = Color(0xFFF6EEFF),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF381520),
                    Color(0xFF9F4062),
                    Color(0xFFE785A7),
                ),
            ),
            accent = Color(0xFFFFEEF5),
        ),
        WalletCardPalette(
            background = Brush.linearGradient(
                listOf(
                    Color(0xFF13293F),
                    Color(0xFF2280A6),
                    Color(0xFF6FD2F7),
                ),
            ),
            accent = Color(0xFFEAFBFF),
        ),
    )
    return palettes[stackedIndex % palettes.size]
}

@Composable
internal fun backendLabel(backendType: ServerBackendType): String {
    return when (backendType) {
        ServerBackendType.QBITTORRENT -> stringResource(R.string.backend_qbittorrent)
        ServerBackendType.TRANSMISSION -> stringResource(R.string.backend_transmission)
    }
}

internal fun backendOverviewIconRes(backendType: ServerBackendType): Int {
    return when (backendType) {
        ServerBackendType.QBITTORRENT -> R.drawable.ic_backend_qbittorrent
        ServerBackendType.TRANSMISSION -> R.drawable.ic_backend_transmission
    }
}

internal fun formatDashboardSnapshotTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return runCatching {
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault())
            .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
    }.getOrDefault("-")
}

@Composable
internal fun ServerOverviewActionButton(
    iconRes: Int,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}


@Composable
internal fun ServerProfileSummaryCard(
    profile: ServerProfile,
    active: Boolean,
    addressText: String,
    summaryText: String,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.width(214.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
            } else {
                qbGlassOutlineColor(defaultAlpha = 0.28f)
            },
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
            } else {
                qbGlassSubtleContainerColor()
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (active) {
                        stringResource(R.string.server_profile_active)
                    } else {
                        stringResource(R.string.server_profile_tap_to_connect)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSwitch) {
                    Text(
                        text = if (active) {
                            stringResource(R.string.dashboard_open_torrents)
                        } else {
                            stringResource(R.string.connect)
                        },
                    )
                }
                Row {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
internal fun DashboardEntryHintBubble(
    text: String,
    dismissDescription: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = qbGlassChipColor(),
                shape = RoundedCornerShape(14.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "×",
            modifier = Modifier
                .semantics { contentDescription = dismissDescription }
                .clickable(onClick = onDismiss)
                .padding(horizontal = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun DashboardSecondaryStatsBlock(
    uploadSpeedText: String,
    downloadSpeedText: String,
    uploadLimitText: String,
    downloadLimitText: String,
    showTotals: Boolean,
    totalDownloadedText: String,
    totalUploadedText: String,
    useAltSpeedLimits: Boolean = false,
    supportsGlobalSpeedLimit: Boolean = false,
    onToggleAltSpeed: (() -> Unit)? = null,
    onOpenSpeedSettings: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DashboardSpeedMetricPanel(
                title = stringResource(R.string.upload),
                directionGlyph = "↑",
                speedText = uploadSpeedText,
                limitText = uploadLimitText,
                totalText = totalUploadedText,
                showTotal = showTotals,
                accentColor = Color(0xFF2B73F5),
            )
            DashboardSpeedMetricPanel(
                title = stringResource(R.string.download),
                directionGlyph = "↓",
                speedText = downloadSpeedText,
                limitText = downloadLimitText,
                totalText = totalDownloadedText,
                showTotal = showTotals,
                accentColor = Color(0xFF08A3AE),
            )
        }
        if (supportsGlobalSpeedLimit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val altSpeedTint = if (useAltSpeedLimits) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(
                    painter = painterResource(R.drawable.ic_speed),
                    contentDescription = stringResource(R.string.alt_speed_toggle),
                    tint = altSpeedTint,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggleAltSpeed?.invoke() },
                )
                Text(
                    text = if (useAltSpeedLimits) {
                        stringResource(R.string.alt_speed_mode_enabled)
                    } else {
                        stringResource(R.string.alt_speed_mode_disabled)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = altSpeedTint,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.global_speed_limit_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenSpeedSettings?.invoke() },
                )
            }
        }
    }
}

internal data class SpeedDisplayParts(
    val value: String,
    val unit: String,
)

@Composable
internal fun RowScope.DashboardSpeedMetricPanel(
    title: String,
    directionGlyph: String,
    speedText: String,
    limitText: String,
    totalText: String,
    showTotal: Boolean,
    accentColor: Color,
) {
    val speedParts = remember(speedText) { splitSpeedDisplayParts(speedText) }
    val speedDescription = if (directionGlyph == "↑") {
        stringResource(R.string.global_up_fmt, speedText)
    } else {
        stringResource(R.string.global_down_fmt, speedText)
    }
    val totalDescription = if (showTotal) {
        if (directionGlyph == "↑") {
            stringResource(R.string.global_total_up_fmt, totalText)
        } else {
            stringResource(R.string.global_total_down_fmt, totalText)
        }
    } else {
        ""
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .semantics {
                contentDescription = listOf(speedDescription, totalDescription)
                    .filter { it.isNotBlank() }
                    .joinToString("，")
            }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = directionGlyph,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(accentColor, RoundedCornerShape(99.dp)),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = speedParts.value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            )
            if (speedParts.unit.isNotBlank()) {
                Text(
                    text = speedParts.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.limit_value_fmt, limitText),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showTotal) {
            Text(
                text = stringResource(R.string.total_value_fmt, totalText),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
            )
        }
    }
}

internal fun splitSpeedDisplayParts(speedText: String): SpeedDisplayParts {
    val normalized = speedText.trim()
    val splitIndex = normalized.lastIndexOf(' ')
    return if (splitIndex in 1 until normalized.lastIndex) {
        SpeedDisplayParts(
            value = normalized.substring(0, splitIndex),
            unit = normalized.substring(splitIndex + 1),
        )
    } else {
        SpeedDisplayParts(value = normalized, unit = "")
    }
}

@Composable
internal fun DashboardStatusPill(
    label: String,
    count: Int,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .background(
                color = qbGlassSubtleContainerColor(),
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = 1.dp,
                color = qbGlassOutlineColor(defaultAlpha = 0.24f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, RoundedCornerShape(99.dp)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun buildDashboardStateSummary(torrents: List<TorrentInfo>): DashboardStateSummary {
    if (torrents.isEmpty()) return DashboardStateSummary()

    var uploading = 0
    var downloading = 0
    var pausedUpload = 0
    var pausedDownload = 0
    var error = 0
    var checking = 0
    var waiting = 0

    torrents.forEach { torrent ->
        when (normalizeTorrentState(effectiveTorrentState(torrent))) {
            "uploading", "forcedup", "stalledup" -> uploading++
            "downloading", "forceddl", "stalleddl", "metadl", "forcedmetadl", "allocating", "moving" -> downloading++
            "pausedup", "stoppedup" -> pausedUpload++
            "pauseddl", "stoppeddl" -> pausedDownload++
            "error", "missingfiles" -> error++
            "checkingdl", "checkingup", "checkingresumedata" -> checking++
            "queueddl", "queuedup" -> waiting++
        }
    }

    return DashboardStateSummary(
        uploadingCount = uploading,
        downloadingCount = downloading,
        pausedUploadCount = pausedUpload,
        pausedDownloadCount = pausedDownload,
        errorCount = error,
        checkingCount = checking,
        waitingCount = waiting,
    )
}

internal fun formatRateLimit(value: Long, unlimitedLabel: String): String {
    return if (value <= 0L) {
        unlimitedLabel
    } else {
        formatSpeed(value)
    }
}

@Composable
internal fun torrentListSortLabel(option: TorrentListSortOption): String {
    return when (option) {
        TorrentListSortOption.ADDED_TIME -> stringResource(R.string.sort_added_time)
        TorrentListSortOption.UPLOAD_SPEED -> stringResource(R.string.sort_upload_speed)
        TorrentListSortOption.DOWNLOAD_SPEED -> stringResource(R.string.sort_download_speed)
        TorrentListSortOption.SHARE_RATIO -> stringResource(R.string.sort_share_ratio)
        TorrentListSortOption.TOTAL_UPLOADED -> stringResource(R.string.sort_total_uploaded)
        TorrentListSortOption.TOTAL_DOWNLOADED -> stringResource(R.string.sort_total_downloaded)
        TorrentListSortOption.TORRENT_SIZE -> stringResource(R.string.sort_torrent_size)
        TorrentListSortOption.ACTIVITY_TIME -> stringResource(R.string.sort_activity_time)
        TorrentListSortOption.SEEDERS -> stringResource(R.string.sort_seeders)
        TorrentListSortOption.LEECHERS -> stringResource(R.string.sort_leechers)
        TorrentListSortOption.CROSS_SEED_COUNT -> stringResource(R.string.sort_cross_seed_count)
    }
}

internal fun trackerSiteName(tracker: String, unknownLabel: String): String {
    return formatTrackerSiteName(tracker, unknownLabel)
}

internal fun reorderServerProfileIds(
    current: List<String>,
    profileId: String,
    targetIndex: Int,
): List<String> {
    val currentIndex = current.indexOf(profileId)
    if (currentIndex < 0 || targetIndex !in current.indices || currentIndex == targetIndex) {
        return current
    }
    return current.toMutableList().apply {
        remove(profileId)
        add(targetIndex, profileId)
    }
}

// GlobalSpeedLimitDialog 已迁移至 GlobalSpeedLimitDialog.kt
















