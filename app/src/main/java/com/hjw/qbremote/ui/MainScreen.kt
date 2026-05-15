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

internal data class DashboardStateSummary(
    val uploadingCount: Int = 0,
    val downloadingCount: Int = 0,
    val pausedUploadCount: Int = 0,
    val pausedDownloadCount: Int = 0,
    val errorCount: Int = 0,
    val checkingCount: Int = 0,
    val waitingCount: Int = 0,
)

private data class DashboardStatusPillItem(
    val label: String,
    val count: Int,
    val accentColor: Color,
)

@Immutable
data class PieLegendEntry(
    val label: String,
    val value: Long,
    val valueText: String,
)

private val HomeServerStackExpandedCardHeight = 210.dp
private val HomeServerStackCollapsedCardHeight = 90.dp
private val HomeServerStackExposedHeight = 60.dp
private val DashboardCardSpacing = 10.dp
internal const val ServerCardClickSuppressionWindowMs = 140L
private val ServerCardClickSuppressionDragThreshold = 6.dp

internal fun resolveServerCardClickSuppressionTimestamp(
    dragDistanceSinceStart: Float,
    clickSuppressionThresholdPx: Float,
    currentTimeMillis: Long,
): Long {
    return if (dragDistanceSinceStart >= clickSuppressionThresholdPx) {
        currentTimeMillis
    } else {
        0L
    }
}

internal fun shouldSuppressServerCardClick(
    lastDragFinishedAt: Long,
    currentTimeMillis: Long,
    suppressionWindowMs: Long = ServerCardClickSuppressionWindowMs,
): Boolean {
    if (lastDragFinishedAt <= 0L) return false
    return currentTimeMillis - lastDragFinishedAt <= suppressionWindowMs
}

internal data class WalletServerStackCardPresentation(
    val showExpandedLayout: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val borderAlpha: Float,
)

internal fun resolveWalletServerStackCardPresentation(
    selected: Boolean,
    isDragging: Boolean,
    isSettling: Boolean,
): WalletServerStackCardPresentation {
    val showExpandedLayout = selected || isDragging || isSettling
    return WalletServerStackCardPresentation(
        showExpandedLayout = showExpandedLayout,
        horizontalPadding = if (showExpandedLayout) 16.dp else 15.dp,
        verticalPadding = if (showExpandedLayout) 14.dp else 10.dp,
        borderAlpha = if (showExpandedLayout) 0.28f else 0.14f,
    )
}

private enum class AppPage {
    DASHBOARD,
    SERVER_DASHBOARD,
    TORRENT_LIST,
    TORRENT_DETAIL,
    SETTINGS,
}

private data class PageAnimationState(
    val page: AppPage,
    val dashboardSessionKey: String = "",
    val themeSignature: String = "",
)

enum class DashboardChartCard(
    val storageKey: String,
) {
    COUNTRY_FLOW("country_flow"),
    CATEGORY_SHARE("category_share"),
    DAILY_UPLOAD("daily_upload"),
    TAG_UPLOAD("tag_upload"),
    TORRENT_STATE("torrent_state"),
    TRACKER_SITE("tracker_site"),
}

val PanelShape = RoundedCornerShape(20.dp)
private val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF060A12),
        Color(0xFF0B1422),
        Color(0xFF08131E),
        Color(0xFF060A12),
    ),
)
private val LightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF5FAFF),
        Color(0xFFEAF3FC),
        Color(0xFFE4F0F9),
        Color(0xFFF6FAFF),
    ),
)
val DashboardPiePalette = listOf(
    Color(0xFF4C8DFF),
    Color(0xFF33BC84),
    Color(0xFFF3A53C),
    Color(0xFFA77AF2),
    Color(0xFFEF6D5E),
    Color(0xFF19B1C3),
    Color(0xFF8F9FB7),
    Color(0xFFFFCF5C),
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var currentPage by rememberSaveable { mutableStateOf(AppPage.DASHBOARD) }
    var previousPage by rememberSaveable { mutableStateOf(AppPage.DASHBOARD) }
    var showAddTorrentSheet by rememberSaveable { mutableStateOf(false) }
    var showServerProfileSheet by rememberSaveable { mutableStateOf(false) }
    var serverSheetEditingProfileId by rememberSaveable { mutableStateOf("") }
    var pendingDeleteProfileId by rememberSaveable { mutableStateOf("") }
    var pendingTorrentExportHash by rememberSaveable { mutableStateOf("") }
    var pendingTorrentExportName by rememberSaveable { mutableStateOf("") }
    var selectedTorrentIdentity by rememberSaveable { mutableStateOf("") }
    var pendingTorrentReturnIdentity by rememberSaveable { mutableStateOf("") }
    var showDashboardCardManagerSheet by rememberSaveable { mutableStateOf(false) }
    var showTorrentSortMenu by remember { mutableStateOf(false) }
    var showTorrentSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortScrollRequestId by remember { mutableIntStateOf(0) }
    val addTorrentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val serverProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dashboardCardManagerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openDrawerDescription = stringResource(R.string.menu_open_drawer)
    val backDescription = stringResource(R.string.back)
    val manageServersDescription = stringResource(R.string.menu_manage_servers)
    val sortDescription = stringResource(R.string.menu_sort)
    val searchDescription = stringResource(R.string.menu_search)
    val addTorrentDescription = stringResource(R.string.menu_add_torrent)
    val localContext = LocalContext.current
    val exportTorrentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-bittorrent"),
    ) { uri ->
        val exportHash = pendingTorrentExportHash
        pendingTorrentExportHash = ""
        pendingTorrentExportName = ""
        if (uri == null || exportHash.isBlank()) return@rememberLauncherForActivityResult
        viewModel.exportTorrentFile(exportHash) { bytes ->
            runCatching {
                localContext.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                } ?: error("Unable to open export target.")
            }.onSuccess {
                scope.launch {
                    snackbarHostState.showSnackbar(localContext.getString(R.string.detail_export_success))
                }
            }.onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar(localContext.getString(R.string.detail_export_failed))
                }
            }
        }
    }
    val backgroundTargetSizePx = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density,
    ) {
        val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
        (maxOf(widthPx, heightPx) * 1.18f).roundToInt().coerceAtLeast(1)
    }
    val appBackgroundBrush = when (state.settings.appTheme) {
        AppTheme.DARK -> DarkBackgroundGradient
        AppTheme.LIGHT -> LightBackgroundGradient
        AppTheme.CUSTOM -> DarkBackgroundGradient
    }
    val customBackgroundState = rememberCustomBackgroundImageState(
        path = state.settings.customBackgroundImagePath,
        targetMaxDimensionPx = backgroundTargetSizePx,
    )
    val customBackgroundImage = customBackgroundState.image
    val showCustomBackgroundImage = state.settings.appTheme == AppTheme.CUSTOM && customBackgroundImage != null
    val customBackgroundScrim = if (state.settings.customBackgroundToneIsLight) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }
    val pageThemeSignature = remember(
        state.settings.appTheme,
        state.settings.customBackgroundToneIsLight,
        state.settings.customBackgroundImagePath,
    ) {
        buildPageThemeSignature(
            appTheme = state.settings.appTheme,
            customBackgroundToneIsLight = state.settings.customBackgroundToneIsLight,
            customBackgroundImagePath = state.settings.customBackgroundImagePath,
        )
    }
    val torrentListDisplayState by viewModel.torrentListDisplayState.collectAsStateWithLifecycle()
    val serverDashboardDisplay by viewModel.serverDashboardDisplayState.collectAsStateWithLifecycle()
    val torrentListFilterState = torrentListDisplayState.torrentListFilterState
    val torrentListBaseSnapshot = torrentListDisplayState.torrentListBaseSnapshot
    val visibleTorrentItems = torrentListDisplayState.visibleTorrentItems
    val torrentPlacementContextKey = remember(currentPage, state.activeServerProfileId) {
        "${currentPage.name}:${state.activeServerProfileId.orEmpty()}"
    }
    val visibleTorrentItemKeys = remember(visibleTorrentItems) {
        visibleTorrentItems.map { item -> item.torrent.hash.ifBlank { item.identityKey } }
    }
    var previousVisibleTorrentItemKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var previousTorrentPlacementContextKey by remember { mutableStateOf<String?>(null) }
    val comparablePreviousTorrentItemKeys = remember(
        torrentPlacementContextKey,
        previousTorrentPlacementContextKey,
        previousVisibleTorrentItemKeys,
    ) {
        if (previousTorrentPlacementContextKey == torrentPlacementContextKey) {
            previousVisibleTorrentItemKeys
        } else {
            emptyList()
        }
    }
    val animateTorrentPlacement = remember(
        comparablePreviousTorrentItemKeys,
        visibleTorrentItemKeys,
    ) {
        shouldAnimateTorrentPlacement(
            previousKeys = comparablePreviousTorrentItemKeys,
            currentKeys = visibleTorrentItemKeys,
            sortOption = torrentListFilterState.sortOption,
        )
    }
    LaunchedEffect(torrentPlacementContextKey, visibleTorrentItemKeys) {
        previousTorrentPlacementContextKey = torrentPlacementContextKey
        previousVisibleTorrentItemKeys = visibleTorrentItemKeys.toList()
    }
    val crossSeedCounts = torrentListBaseSnapshot.crossSeedCounts
    val activePendingProfileId = remember(state.activeServerProfileId) {
        state.activeServerProfileId.orEmpty()
    }
    fun isPendingAction(hash: String): Boolean {
        if (activePendingProfileId.isBlank()) return false
        return state.pendingActionKeys.contains(buildPendingActionKey(activePendingProfileId, hash))
    }
    val categoryOptionsForAdd = remember(state.categoryOptions) {
        buildSortedDistinctTrimmedStrings(state.categoryOptions)
    }
    val tagOptionsForAdd = remember(state.tagOptions) {
        buildSortedDistinctTrimmedStrings(state.tagOptions)
    }
    val pathOptionsForAdd = remember(state.torrents) {
        buildSortedDistinctTrimmedStrings(state.torrents.map { torrent -> torrent.savePath })
    }
    val selectedTorrent = remember(torrentListBaseSnapshot.torrents, selectedTorrentIdentity) {
        torrentListBaseSnapshot.torrents.firstOrNull { torrentIdentityKey(it) == selectedTorrentIdentity }
    }
    val showHomeAggregateDashboard = state.serverProfiles.isNotEmpty()
    val showServerStackReorderUi = state.serverProfiles.size > 1
    val dashboardServerSnapshotIds = remember(state.dashboardServerSnapshots) {
        state.dashboardServerSnapshots.map { it.profileId }
    }
    var localDashboardServerProfileOrder by remember {
        mutableStateOf(dashboardServerSnapshotIds)
    }
    LaunchedEffect(dashboardServerSnapshotIds) {
        localDashboardServerProfileOrder = reconcileReorderableItemOrder(
            currentOrder = localDashboardServerProfileOrder,
            availableItems = dashboardServerSnapshotIds,
        )
    }
    val orderedDashboardServerSnapshots = remember(
        state.dashboardServerSnapshots,
        localDashboardServerProfileOrder,
    ) {
        orderDashboardServerSnapshots(
            snapshots = state.dashboardServerSnapshots,
            orderedProfileIds = localDashboardServerProfileOrder,
        )
    }
    val selectedDashboardProfileId = state.selectedDashboardProfileId
        ?: state.activeServerProfileId
        ?: state.serverProfiles.firstOrNull()?.id
    val serverDashboardSessionKey = remember(
        selectedDashboardProfileId,
        state.dashboardSessionToken,
    ) {
        "${selectedDashboardProfileId.orEmpty()}:${state.dashboardSessionToken}"
    }
    val selectedDashboardSnapshot = remember(
        orderedDashboardServerSnapshots,
        selectedDashboardProfileId,
    ) {
        orderedDashboardServerSnapshots.firstOrNull { it.profileId == selectedDashboardProfileId }
    }
    val selectedServerProfile = remember(state.serverProfiles, selectedDashboardProfileId) {
        state.serverProfiles.firstOrNull { it.id == selectedDashboardProfileId }
    }
    val pendingDeleteProfile = remember(pendingDeleteProfileId, state.serverProfiles) {
        state.serverProfiles.firstOrNull { it.id == pendingDeleteProfileId }
    }
    val selectedDashboardBackendType = serverDashboardDisplay.backendType
    val serverDashboardCapabilities = remember(selectedDashboardBackendType) {
        defaultCapabilitiesFor(selectedDashboardBackendType)
    }
    val serverDashboardVersion = serverDashboardDisplay.serverVersion
    val serverDashboardTransferInfo = serverDashboardDisplay.transferInfo
    val serverDashboardTorrents = serverDashboardDisplay.torrents
    val serverDashboardTorrentCount = serverDashboardDisplay.torrentCount
    val serverDashboardShowContent = serverDashboardDisplay.hasContent
    val availableDashboardCards = serverDashboardDisplay.availableCards
    val currentDashboardPreferences = serverDashboardDisplay.resolvedPreferences
    val showServerStackHint = showServerStackReorderUi &&
        !state.settings.hasSeenServerStackReorderHint
    val showDashboardSwipeHint = selectedServerProfile != null &&
        !state.settings.hasSeenServerDashboardSwipeHint
    val showDashboardCardHint = selectedServerProfile != null &&
        !state.settings.hasSeenServerDashboardCardHint
    val dashboardListState = rememberLazyListState()
    val serverDashboardListState = rememberLazyListState()
    val torrentListState = rememberLazyListState()
    val torrentDetailListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    var localDashboardCardOrder by remember(selectedServerProfile?.id, selectedDashboardBackendType) {
        mutableStateOf(parseDashboardCardOrder(currentDashboardPreferences.cardOrder, availableDashboardCards))
    }
    var localVisibleDashboardCardKeys by remember(selectedServerProfile?.id, selectedDashboardBackendType) {
        mutableStateOf(currentDashboardPreferences.visibleCards.toSet())
    }
    val visibleDashboardCards = remember(
        localDashboardCardOrder,
        localVisibleDashboardCardKeys,
    ) {
        localDashboardCardOrder.filter { card ->
            card.storageKey in localVisibleDashboardCardKeys
        }
    }
    val displayVisibleDashboardCards = remember(visibleDashboardCards) {
        buildDashboardDisplayCards(visibleDashboardCards)
    }
    val dashboardDragGestureKey = remember(displayVisibleDashboardCards) {
        displayVisibleDashboardCards.joinToString(separator = "|") { it.ownerKey }
    }
    val displayDashboardPreferences = remember(
        currentDashboardPreferences,
        localDashboardCardOrder,
        localVisibleDashboardCardKeys,
        visibleDashboardCards,
    ) {
        currentDashboardPreferences.copy(
            cardOrder = serializeDashboardCardOrder(localDashboardCardOrder, availableDashboardCards),
            visibleCards = visibleDashboardCards.map { it.storageKey },
        )
    }
    var dashboardDragState by remember { mutableStateOf<VerticalReorderDragState<DashboardDisplayCardItem>?>(null) }
    var dashboardDropJob by remember { mutableStateOf<Job?>(null) }
    var dashboardLastDragFlushTime by remember { mutableLongStateOf(0L) }
    var dashboardAccumulatedDragDelta by remember { mutableFloatStateOf(0f) }
    var dashboardLockedCardHeights by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var revealedDashboardHideCardKey by remember(selectedServerProfile?.id, currentPage) {
        mutableStateOf<String?>(null)
    }
    val dashboardCardHeights = remember { mutableStateMapOf<String, Int>() }
    var serverDragState by remember { mutableStateOf<VerticalReorderDragState<String>?>(null) }
    var serverDropJob by remember { mutableStateOf<Job?>(null) }
    var serverDragSettling by remember { mutableStateOf(false) }
    var serverLastDragFlushTime by remember { mutableLongStateOf(0L) }
    var serverAccumulatedDragDelta by remember { mutableFloatStateOf(0f) }
    val pageListScrollEnabled = shouldEnablePageListScroll(
        draggingServerProfileId = serverDragState?.item,
        draggingDashboardCard = dashboardDragState?.item,
        settlingServerProfileId = serverDragState?.item.takeIf { serverDragSettling },
    )

    fun listStateForPage(page: AppPage): LazyListState {
        return when (page) {
            AppPage.DASHBOARD -> dashboardListState
            AppPage.SERVER_DASHBOARD -> serverDashboardListState
            AppPage.TORRENT_LIST -> torrentListState
            AppPage.TORRENT_DETAIL -> torrentDetailListState
            AppPage.SETTINGS -> settingsListState
        }
    }

    fun closeDrawer(action: () -> Unit) {
        action()
        scope.launch { drawerState.close() }
    }

    fun openSettings() {
        if (currentPage != AppPage.SETTINGS) {
            previousPage = currentPage
        }
        currentPage = AppPage.SETTINGS
    }

    fun openServerProfileSheet(editingProfileId: String? = null) {
        serverSheetEditingProfileId = editingProfileId.orEmpty()
        showServerProfileSheet = true
    }

    fun requestDeleteServerProfile(profileId: String) {
        pendingDeleteProfileId = profileId
    }

    fun copyToClipboard(value: String, successMessageRes: Int) {
        val text = value.trim()
        if (text.isBlank()) return
        clipboardManager.setText(AnnotatedString(text))
        scope.launch {
            snackbarHostState.showSnackbar(localContext.getString(successMessageRes))
        }
    }

    fun requestTorrentExport(hash: String, torrentName: String) {
        val normalizedHash = hash.trim()
        if (normalizedHash.isBlank()) return
        pendingTorrentExportHash = normalizedHash
        pendingTorrentExportName = buildTorrentExportFileName(
            torrentName = torrentName,
            hash = normalizedHash,
        )
        exportTorrentLauncher.launch(pendingTorrentExportName)
    }

    fun openTorrentList() {
        if (currentPage != AppPage.TORRENT_LIST) {
            previousPage = currentPage
        }
        currentPage = AppPage.TORRENT_LIST
    }

    fun openTorrentListFromDashboard() {
        if (!state.settings.homeTorrentEntryHintDismissed) {
            viewModel.dismissHomeTorrentEntryHint()
        }
        openTorrentList()
    }

    fun openServerDashboard(profileId: String) {
        if (profileId.isBlank()) return
        if (currentPage != AppPage.SERVER_DASHBOARD) {
            previousPage = currentPage
        }
        currentPage = AppPage.SERVER_DASHBOARD
        viewModel.switchServerProfile(profileId)
    }

    fun openTorrentDetail(torrent: TorrentInfo) {
        val torrentIdentity = torrentIdentityKey(torrent)
        selectedTorrentIdentity = torrentIdentity
        pendingTorrentReturnIdentity = if (currentPage == AppPage.TORRENT_LIST) {
            torrentIdentity
        } else {
            ""
        }
        if (currentPage != AppPage.TORRENT_DETAIL) {
            previousPage = currentPage
        }
        currentPage = AppPage.TORRENT_DETAIL
    }

    fun backToPreviousPage() {
        val nextPage = if (previousPage == currentPage) AppPage.DASHBOARD else previousPage
        if (currentPage == AppPage.TORRENT_DETAIL && nextPage != AppPage.TORRENT_LIST) {
            pendingTorrentReturnIdentity = ""
        }
        currentPage = nextPage
    }

    fun requestScrollToFirstTorrentAfterSort() {
        sortScrollRequestId += 1
    }

    fun persistDashboardCardOrderIfChanged(
        nextOrder: List<DashboardChartCard>,
        rollbackOrder: List<DashboardChartCard> = nextOrder,
    ) {
        val profileId = selectedServerProfile?.id ?: return
        val serialized = serializeDashboardCardOrder(nextOrder, availableDashboardCards)
        if (serialized != currentDashboardPreferences.cardOrder) {
            viewModel.updateServerDashboardCardOrder(profileId, nextOrder) { success ->
                if (!success && state.activeServerProfileId == profileId) {
                    localDashboardCardOrder = rollbackOrder
                }
            }
        }
    }

    fun startDashboardCardDrag(card: DashboardDisplayCardItem) {
        val visibleCards = displayVisibleDashboardCards
        val startIndex = visibleCards.indexOf(card)
        if (startIndex < 0) return
        revealedDashboardHideCardKey = null
        val itemSpacingPx = with(density) { DashboardCardSpacing.toPx() }
        val defaultCardHeightPx = with(density) { 180.dp.toPx() }
        val slotHeights = visibleCards.map { dashboardCard ->
            dashboardCardHeights[dashboardCard.ownerKey]?.toFloat() ?: defaultCardHeightPx
        }
        val slotTops = buildList {
            var currentTop = 0f
            visibleCards.forEachIndexed { index, _ ->
                add(currentTop)
                currentTop += slotHeights[index] + itemSpacingPx
            }
        }
        val edgeSlackPx = with(density) { 24.dp.toPx() }
        dashboardDropJob?.cancel()
        dashboardDropJob = null
        dashboardDragState = null
        dashboardLockedCardHeights = visibleCards.associate { dashboardCard ->
            val measuredHeight = dashboardCardHeights[dashboardCard.ownerKey] ?: defaultCardHeightPx.roundToInt()
            dashboardCard.ownerKey to measuredHeight
        }
        val session = buildVerticalReorderSession(
            items = visibleCards,
            startIndex = startIndex,
            slotTops = slotTops,
            slotHeights = slotHeights,
            edgeSlackPx = edgeSlackPx,
        )
        dashboardDragState = createVerticalReorderDragState(
            session = session,
            item = card,
        )
        selectedServerProfile?.id?.let(viewModel::setDashboardReorderHold)
    }

    fun updateDashboardCardDrag(card: DashboardDisplayCardItem, deltaY: Float) {
        val dragState = dashboardDragState ?: return
        if (dragState.item != card) return
        dashboardAccumulatedDragDelta += deltaY
        val now = SystemClock.elapsedRealtime()
        if (now - dashboardLastDragFlushTime < 16L) return
        dashboardLastDragFlushTime = now
        val delta = dashboardAccumulatedDragDelta
        dashboardAccumulatedDragDelta = 0f
        dashboardDragState = applyVerticalReorderDragDelta(
            state = dragState,
            deltaY = delta,
        )
    }

    fun finishDashboardCardDrag(commit: Boolean) {
        val dragState = dashboardDragState ?: return
        val draggedCard = dragState.item
        val dragSession = dragState.session
        val previousOrder = localDashboardCardOrder
        val finalTargetIndex = resolveVerticalReorderFinalTargetIndex(
            state = dragState,
            commit = commit,
        )
        val finalOffsetY = resolveVerticalReorderRestingOffset(
            state = dragState,
            commit = commit,
        )
        val nextOrder = if (commit) {
            reorderDashboardCardOrderForDisplay(
                order = localDashboardCardOrder,
                displayCards = dragSession.items,
                owner = draggedCard.owner,
                targetIndex = finalTargetIndex,
            )
        } else {
            previousOrder
        }
        dashboardDropJob?.cancel()
        dashboardDragState = dragState.copy(targetIndex = finalTargetIndex)
        dashboardDropJob = scope.launch {
            val startOffsetY = dashboardDragState?.offsetY ?: dragState.offsetY
            if (abs(startOffsetY - finalOffsetY) > 0.5f) {
                val animatable = Animatable(startOffsetY)
                animatable.animateTo(
                    targetValue = finalOffsetY,
                    animationSpec = ReorderSettleAnimationSpec,
                ) {
                    dashboardDragState = dashboardDragState?.copy(
                        offsetY = value,
                        targetIndex = finalTargetIndex,
                    )
                }
            }
            val shouldPersistOrder = commit && nextOrder != previousOrder
            Snapshot.withMutableSnapshot {
                if (shouldPersistOrder) {
                    localDashboardCardOrder = nextOrder
                }
                dashboardDragState = null
                dashboardLockedCardHeights = emptyMap()
                dashboardDropJob = null
            }
            if (shouldPersistOrder) {
                persistDashboardCardOrderIfChanged(nextOrder, previousOrder)
            }
            viewModel.setDashboardReorderHold(null)
        }
    }

    fun endDashboardCardDrag() = finishDashboardCardDrag(commit = true)

    fun cancelDashboardCardDrag() = finishDashboardCardDrag(commit = false)

    fun hideDashboardCard(card: DashboardDisplayCardItem) {
        val profileId = selectedServerProfile?.id ?: return
        val previousVisibleKeys = localVisibleDashboardCardKeys
        val nextVisibleKeys = applyDashboardDisplayCardVisibility(
            visibleKeys = previousVisibleKeys,
            displayCard = card,
            visible = false,
        )
        localVisibleDashboardCardKeys = nextVisibleKeys
        revealedDashboardHideCardKey = null
        viewModel.updateServerDashboardCardsVisibility(
            profileId = profileId,
            cards = card.representedCards,
            visible = false,
        ) { success ->
            if (!success && state.activeServerProfileId == profileId) {
                localVisibleDashboardCardKeys = previousVisibleKeys
            }
        }
    }

    fun startServerStackDrag(profileId: String) {
        val orderedIds = orderedDashboardServerSnapshots.map { it.profileId }
        val startIndex = orderedIds.indexOf(profileId)
        if (startIndex < 0 || orderedIds.size < 2) return
        val exposedStepPx = with(density) { HomeServerStackExposedHeight.toPx() }
        val edgeSlackPx = with(density) { 24.dp.toPx() }
        serverDropJob?.cancel()
        serverDropJob = null
        serverDragState = null
        serverDragSettling = false
        val session = buildHomeServerStackReorderSession(
            orderedProfileIds = orderedIds,
            startIndex = startIndex,
            exposedStepPx = exposedStepPx,
            edgeSlackPx = edgeSlackPx,
        )
        serverDragState = createVerticalReorderDragState(
            session = session,
            item = profileId,
        )
    }

    fun updateServerStackDrag(profileId: String, deltaY: Float) {
        val dragState = serverDragState ?: return
        if (serverDragSettling) return
        if (dragState.item != profileId) return
        serverAccumulatedDragDelta += deltaY
        val now = SystemClock.elapsedRealtime()
        if (now - serverLastDragFlushTime < 16L) return
        serverLastDragFlushTime = now
        val delta = serverAccumulatedDragDelta
        serverAccumulatedDragDelta = 0f
        serverDragState = applyVerticalReorderDragDelta(
            state = dragState,
            deltaY = delta,
        )
    }

    fun finishServerStackDrag(commit: Boolean) {
        val dragState = serverDragState ?: return
        val dropPlan = resolveHomeServerStackDropPlan(
            state = dragState,
            commit = commit,
        )
        serverDropJob?.cancel()
        serverDragSettling = true
        serverDragState = dragState.copy(targetIndex = dropPlan.finalTargetIndex)
        serverDropJob = scope.launch {
            val startOffsetY = serverDragState?.offsetY ?: dragState.offsetY
            if (abs(startOffsetY - dropPlan.finalOffsetY) > 0.5f) {
                val animatable = Animatable(startOffsetY)
                animatable.animateTo(
                    targetValue = dropPlan.finalOffsetY,
                    animationSpec = ReorderSettleAnimationSpec,
                ) {
                    serverDragState = serverDragState?.copy(
                        offsetY = value,
                        targetIndex = dropPlan.finalTargetIndex,
                    )
                }
            }
            Snapshot.withMutableSnapshot {
                if (dropPlan.shouldCommitReorder) {
                    localDashboardServerProfileOrder = dropPlan.reorderedIds
                }
                serverDragState = null
                serverDragSettling = false
                serverDropJob = null
            }
            if (dropPlan.shouldCommitReorder) {
                viewModel.reorderServerProfiles(dropPlan.reorderedIds)
            }
        }
    }

    fun endServerStackDrag() = finishServerStackDrag(commit = true)

    fun cancelServerStackDrag() = finishServerStackDrag(commit = false)

    fun scrollToTopOfCurrentPage(animated: Boolean) {
        scope.launch {
            val targetListState = listStateForPage(currentPage)
            if (animated) {
                targetListState.animateScrollToItem(0)
            } else {
                targetListState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(sortScrollRequestId) {
        if (sortScrollRequestId <= 0) return@LaunchedEffect
        if (currentPage != AppPage.TORRENT_LIST) return@LaunchedEffect
        val targetIndex = if (showTorrentSearchBar && visibleTorrentItems.isNotEmpty()) 1 else 0
        torrentListState.scrollToItem(targetIndex)
        // Guard against LazyList position restore after data reordering.
        yield()
        torrentListState.scrollToItem(targetIndex)
    }

    LaunchedEffect(
        currentPage,
        pendingTorrentReturnIdentity,
        visibleTorrentItems,
        showTorrentSearchBar,
    ) {
        if (currentPage != AppPage.TORRENT_LIST) return@LaunchedEffect
        val anchorIdentity = pendingTorrentReturnIdentity.ifBlank { return@LaunchedEffect }
        val targetListIndex = visibleTorrentItems.indexOfFirst { it.identityKey == anchorIdentity }
        pendingTorrentReturnIdentity = ""
        if (targetListIndex < 0) return@LaunchedEffect
        val targetIndex = targetListIndex + if (showTorrentSearchBar) 1 else 0
        torrentListState.scrollToItem(targetIndex)
        yield()
        torrentListState.scrollToItem(targetIndex)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    LaunchedEffect(currentDashboardPreferences.cardOrder, selectedServerProfile?.id, selectedDashboardBackendType) {
        localDashboardCardOrder = parseDashboardCardOrder(
            currentDashboardPreferences.cardOrder,
            availableDashboardCards,
        )
    }

    LaunchedEffect(currentDashboardPreferences.visibleCards, selectedServerProfile?.id, selectedDashboardBackendType) {
        localVisibleDashboardCardKeys = currentDashboardPreferences.visibleCards.toSet()
    }

    LaunchedEffect(revealedDashboardHideCardKey, selectedServerProfile?.id, currentPage) {
        val currentKey = revealedDashboardHideCardKey ?: return@LaunchedEffect
        if (currentPage != AppPage.SERVER_DASHBOARD) return@LaunchedEffect
        delay(5_000)
        if (revealedDashboardHideCardKey == currentKey) {
            revealedDashboardHideCardKey = null
        }
    }

    LaunchedEffect(currentPage) {
        if (currentPage != AppPage.DASHBOARD) {
            serverDropJob?.cancel()
            serverDropJob = null
            serverDragState = null
            serverDragSettling = false
        }
        if (currentPage != AppPage.SERVER_DASHBOARD) {
            showDashboardCardManagerSheet = false
            revealedDashboardHideCardKey = null
            dashboardDropJob?.cancel()
            dashboardDropJob = null
            dashboardDragState = null
            dashboardLockedCardHeights = emptyMap()
            viewModel.setDashboardReorderHold(null)
        }
    }

    LaunchedEffect(currentPage, selectedTorrent?.hash) {
        if (currentPage != AppPage.TORRENT_LIST) {
            showTorrentSortMenu = false
        }
        val hash = selectedTorrent?.hash.orEmpty()
        val refreshScene = when (currentPage) {
            AppPage.DASHBOARD -> RefreshScene.DASHBOARD
            AppPage.SERVER_DASHBOARD -> RefreshScene.SERVER
            AppPage.TORRENT_LIST -> RefreshScene.SERVER
            AppPage.TORRENT_DETAIL -> RefreshScene.TORRENT_DETAIL
            AppPage.SETTINGS -> RefreshScene.SETTINGS
        }
        viewModel.updateRefreshScene(refreshScene)
        if (currentPage == AppPage.TORRENT_DETAIL && hash.isNotBlank()) {
            viewModel.loadTorrentDetail(hash)
        }
    }

    LaunchedEffect(currentPage, serverDashboardSessionKey) {
        if (currentPage == AppPage.SERVER_DASHBOARD) {
            serverDashboardListState.scrollToItem(0)
        }
    }

    BackHandler(enabled = currentPage != AppPage.DASHBOARD) {
        backToPreviousPage()
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                drawerContainerColor = qbGlassStrongContainerColor(),
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                DrawerThemeItem(
                    settings = state.settings,
                    onThemeChange = { theme ->
                        closeDrawer { viewModel.updateAppTheme(theme) }
                    },
                    onApplyCustomTheme = { imagePath, toneIsLight ->
                        closeDrawer {
                            viewModel.applyCustomThemeBackground(imagePath, toneIsLight)
                        }
                    },
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentPage) {
                    val edgeWidthPx = with(density) { 36.dp.toPx() }
                    val triggerDistancePx = with(density) { 90.dp.toPx() }
                    var trackingFromEdge = false
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            trackingFromEdge = offset.x <= edgeWidthPx
                            dragDistance = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!trackingFromEdge) return@detectHorizontalDragGestures
                            if (dragAmount > 0f) {
                                dragDistance += dragAmount
                            }
                            if (dragDistance >= triggerDistancePx && currentPage != AppPage.DASHBOARD) {
                                backToPreviousPage()
                                trackingFromEdge = false
                                dragDistance = 0f
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            trackingFromEdge = false
                            dragDistance = 0f
                        },
                        onDragCancel = {
                            trackingFromEdge = false
                            dragDistance = 0f
                        },
                    )
                },
        ) {
            if (showCustomBackgroundImage) {
                customBackgroundImage?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(customBackgroundScrim),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackgroundBrush),
                )
            }
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
                topBar = {
                    val compactTorrentTopBar = currentPage == AppPage.TORRENT_LIST
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        navigationIcon = {
                            TextButton(
                                modifier = Modifier.semantics {
                                    contentDescription = if (currentPage == AppPage.DASHBOARD) {
                                        openDrawerDescription
                                    } else {
                                        backDescription
                                    }
                                },
                                onClick = {
                                    if (currentPage == AppPage.DASHBOARD) {
                                        scope.launch { drawerState.open() }
                                    } else {
                                        backToPreviousPage()
                                    }
                                },
                                contentPadding = if (compactTorrentTopBar) {
                                    PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                } else {
                                    ButtonDefaults.TextButtonContentPadding
                                },
                            ) {
                                Text(
                                    text = if (currentPage == AppPage.DASHBOARD) "≡" else "←",
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        },
                        title = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (compactTorrentTopBar) {
                                            Modifier.offset(x = (-4).dp)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .pointerInput(currentPage) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                scrollToTopOfCurrentPage(animated = true)
                                            },
                                        )
                                    },
                            ) {
                                TopBrandTitle(
                                    modifier = Modifier.fillMaxWidth(),
                                    compact = compactTorrentTopBar,
                                )
                            }
                        },
                        actions = {
                            when (currentPage) {
                                AppPage.DASHBOARD -> {
                                    Row(
                                        modifier = Modifier.padding(end = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(
                                            modifier = Modifier.semantics {
                                                contentDescription = manageServersDescription
                                            },
                                            onClick = { openServerProfileSheet() },
                                        ) {
                                            Text(
                                                text = stringResource(R.string.menu_servers),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                        TextButton(onClick = { openSettings() }) {
                                            Text(
                                                text = stringResource(R.string.menu_settings),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                    }
                                }

                                AppPage.SERVER_DASHBOARD -> {
                                    TextButton(
                                        onClick = {
                                            viewModel.markServerDashboardCardHintSeen()
                                            showDashboardCardManagerSheet = true
                                        },
                                    ) {
                                        Text(
                                            text = stringResource(R.string.dashboard_manage_cards_action),
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }

                                AppPage.SETTINGS -> {
                                    TextButton(
                                        onClick = {
                                            if (state.connected) viewModel.refresh(manual = true) else viewModel.connect()
                                        },
                                    ) {
                                        Text(
                                            text = if (state.connected) {
                                                if (state.isManualRefreshing) {
                                                    stringResource(R.string.refreshing)
                                                } else {
                                                    stringResource(R.string.refresh)
                                                }
                                            } else {
                                                stringResource(R.string.connect)
                                            },
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }

                                AppPage.TORRENT_DETAIL -> {
                                    TextButton(
                                        onClick = {
                                            if (state.connected) viewModel.refresh(manual = true) else viewModel.connect()
                                        },
                                    ) {
                                        Text(
                                            text = stringResource(R.string.refresh),
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }

                                AppPage.TORRENT_LIST -> {
                                    Row(
                                        modifier = Modifier
                                            .padding(end = 2.dp)
                                            .offset(x = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box {
                                            TextButton(
                                                modifier = Modifier
                                                    .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                                                    .semantics {
                                                        contentDescription = sortDescription
                                                    },
                                                onClick = { showTorrentSortMenu = true },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.menu_sort),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showTorrentSortMenu,
                                                onDismissRequest = { showTorrentSortMenu = false },
                                            ) {
                                                TorrentListSortOption.entries.forEach { option ->
                                                    val isSelected = option == torrentListFilterState.sortOption
                                                    DropdownMenuItem(
                                                        text = {
                                                            val prefix = if (isSelected) "✓ " else ""
                                                            Text("$prefix${torrentListSortLabel(option)}")
                                                        },
                                                        onClick = {
                                                            viewModel.updateTorrentListSortOption(option)
                                                            showTorrentSortMenu = false
                                                            requestScrollToFirstTorrentAfterSort()
                                                        },
                                                    )
                                                }
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                        text = {
                                                            val prefix = if (torrentListFilterState.descending) "✓ " else ""
                                                            Text("${prefix}${stringResource(R.string.sort_descending)}")
                                                        },
                                                    onClick = {
                                                        viewModel.updateTorrentListSortDirection(true)
                                                        showTorrentSortMenu = false
                                                        requestScrollToFirstTorrentAfterSort()
                                                    },
                                                )
                                                DropdownMenuItem(
                                                        text = {
                                                            val prefix = if (!torrentListFilterState.descending) "✓ " else ""
                                                            Text("${prefix}${stringResource(R.string.sort_ascending)}")
                                                        },
                                                    onClick = {
                                                        viewModel.updateTorrentListSortDirection(false)
                                                        showTorrentSortMenu = false
                                                        requestScrollToFirstTorrentAfterSort()
                                                    },
                                                )
                                            }
                                        }
                                        TextButton(
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                                                .semantics {
                                                    contentDescription = searchDescription
                                                },
                                            onClick = {
                                                showTorrentSearchBar = !showTorrentSearchBar
                                                if (!showTorrentSearchBar) {
                                                    viewModel.updateTorrentSearchQuery("")
                                                }
                                                scope.launch {
                                                    torrentListState.scrollToItem(0)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        ) {
                                            Text(
                                                text = if (showTorrentSearchBar) {
                                                    stringResource(R.string.menu_collapse)
                                                } else {
                                                    stringResource(R.string.menu_search)
                                                },
                                                color = MaterialTheme.colorScheme.onBackground,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                        TextButton(
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                                                .semantics {
                                                    contentDescription = addTorrentDescription
                                                },
                                            onClick = {
                                                viewModel.loadGlobalSelectionOptions()
                                                showAddTorrentSheet = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        ) {
                                            Text(
                                                text = "+",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 22.sp,
                                            )
                                        }
                                    }
                                }

                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                val hasSavedConnection = remember(
                    state.serverProfiles,
                    state.settings.host,
                    state.settings.username,
                ) {
                    state.serverProfiles.isNotEmpty() ||
                        (state.settings.host.trim().isNotBlank() && state.settings.username.trim().isNotBlank())
                }
                val showRestorePlaceholder = !state.startupRestoreComplete ||
                    (hasSavedConnection && !state.dashboardCacheHydrated && !state.connected)
                val showTorrentListContent = state.connected || state.hasDashboardSnapshot
                val showTorrentDetailRestorePlaceholder = selectedTorrent == null &&
                    selectedTorrentIdentity.isNotBlank() &&
                    showRestorePlaceholder
                val showServerDashboardSkeleton = selectedServerProfile != null &&
                    selectedDashboardSnapshot == null &&
                    (state.isConnecting || showRestorePlaceholder)
                val showDashboardSnapshot = if (showHomeAggregateDashboard) {
                    state.serverProfiles.isNotEmpty()
                } else {
                    state.connected || state.hasDashboardSnapshot || state.dashboardServerSnapshots.isNotEmpty()
                }
                val showDashboardSkeleton = !showDashboardSnapshot && showRestorePlaceholder
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

                val contentList: @Composable (AppPage) -> Unit = { page ->
                    // 页面级 state selector — 只重组当前页相关的字段
                    val dashboardState by rememberDashboardPageState(state)
                    val detailState by rememberTorrentDetailPageState(state)
                    val stableDraggingOffsetY = remember(serverDragState?.item, serverDragSettling) {
                        { if (serverDragSettling) 0f else serverDragState?.offsetY ?: 0f }
                    }
                    val stableSettlingOffsetY = remember(serverDragState?.item, serverDragSettling) {
                        { if (serverDragSettling) serverDragState?.offsetY ?: 0f else 0f }
                    }
                    LazyColumn(
                        state = listStateForPage(page),
                        modifier = contentModifier,
                        userScrollEnabled = pageListScrollEnabled,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (page) {
                            AppPage.DASHBOARD -> dashboardHomePageContent(
                                state = dashboardState,
                                dashboardServerSnapshots = orderedDashboardServerSnapshots,
                                showDashboardSnapshot = showDashboardSnapshot,
                                showHomeAggregateDashboard = showHomeAggregateDashboard,
                                showDashboardSkeleton = showDashboardSkeleton,
                                showServerStackHint = showServerStackHint,
                                draggingProfileId = serverDragState?.item,
                                settlingProfileId = serverDragState?.item.takeIf { serverDragSettling },
                                draggingOffsetY = stableDraggingOffsetY,
                                settlingOffsetY = stableSettlingOffsetY,
                                draggingTargetIndex = serverDragState?.targetIndex ?: -1,
                                dragSession = serverDragState?.session,
                                onDismissReorderHint = viewModel::markServerStackReorderHintSeen,
                                onStartServerStackDrag = { profileId ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.markServerStackReorderHintSeen()
                                    startServerStackDrag(profileId)
                                },
                                onDragServerStack = ::updateServerStackDrag,
                                onEndServerStackDrag = ::endServerStackDrag,
                                onCancelServerStackDrag = ::cancelServerStackDrag,
                                onOpenServerDashboard = ::openServerDashboard,
                                onDismissHomeTorrentEntryHint = viewModel::dismissHomeTorrentEntryHint,
                                onOpenTorrentList = ::openTorrentListFromDashboard,
                                onSwitchServerProfile = viewModel::switchServerProfile,
                                onEditServerProfile = { profileId -> openServerProfileSheet(profileId) },
                                onRequestDeleteServerProfile = ::requestDeleteServerProfile,
                                onOpenConnection = ::openSettings,
                                onToggleAltSpeed = viewModel::toggleAltSpeedMode,
                                onOpenSpeedSettings = viewModel::openGlobalSpeedLimitDialog,
                            )

                            AppPage.SERVER_DASHBOARD -> serverDashboardRootPageContent(
                                sessionKey = serverDashboardSessionKey,
                                selectedServerProfile = selectedServerProfile,
                                showContent = serverDashboardShowContent,
                                showDashboardCardHint = showDashboardCardHint,
                                showDashboardSwipeHint = showDashboardSwipeHint,
                                showSkeleton = showServerDashboardSkeleton,
                                showRestorePlaceholder = showRestorePlaceholder,
                                selectedDashboardBackendType = selectedDashboardBackendType,
                                serverDashboardCapabilities = serverDashboardCapabilities,
                                serverDashboardDisplay = serverDashboardDisplay,
                                serverDashboardVersion = serverDashboardVersion,
                                serverDashboardTransferInfo = serverDashboardTransferInfo,
                                serverDashboardTorrents = serverDashboardTorrents,
                                serverDashboardTorrentCount = serverDashboardTorrentCount,
                                displayVisibleDashboardCards = displayVisibleDashboardCards,
                                dashboardDragGestureKey = dashboardDragGestureKey,
                                draggingDashboardCard = dashboardDragState?.item,
                                settlingDashboardCard = null,
                                draggingDashboardOffsetY = { dashboardDragState?.offsetY ?: 0f },
                                settlingDashboardOffsetY = { 0f },
                                draggingDashboardTargetIndex = dashboardDragState?.targetIndex ?: -1,
                                draggingDashboardSession = dashboardDragState?.session,
                                revealedDashboardHideCardKey = revealedDashboardHideCardKey,
                                dashboardCardHeights = dashboardCardHeights,
                                dashboardLockedCardHeights = dashboardLockedCardHeights,
                                onRevealHideCard = { card -> revealedDashboardHideCardKey = card.ownerKey },
                                onHideCard = ::hideDashboardCard,
                                onStartCardDrag = { card ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    startDashboardCardDrag(card)
                                },
                                onDragCard = ::updateDashboardCardDrag,
                                onEndCardDrag = ::endDashboardCardDrag,
                                onCancelCardDrag = ::cancelDashboardCardDrag,
                                onOpenManager = {
                                    viewModel.markServerDashboardCardHintSeen()
                                    showDashboardCardManagerSheet = true
                                },
                                onOpenTorrentList = ::openTorrentListFromDashboard,
                                onSwitchServerProfile = viewModel::switchServerProfile,
                                onEditServerProfile = { profileId -> openServerProfileSheet(profileId) },
                                onRequestDeleteServerProfile = ::requestDeleteServerProfile,
                                onDismissHomeTorrentEntryHint = viewModel::dismissHomeTorrentEntryHint,
                                onMarkDashboardSwipeHintSeen = viewModel::markServerDashboardSwipeHintSeen,
                                onMarkDashboardCardHintSeen = viewModel::markServerDashboardCardHintSeen,
                                onOpenConnection = ::openSettings,
                            )

                            AppPage.TORRENT_LIST -> {
                                torrentListPageContent(
                                    showContent = showTorrentListContent,
                                    showRestorePlaceholder = showRestorePlaceholder,
                                    showSearchBar = showTorrentSearchBar,
                                    animatePlacement = animateTorrentPlacement,
                                    searchQuery = torrentListFilterState.query,
                                    onSearchQueryChange = viewModel::updateTorrentSearchQuery,
                                    filterState = torrentListFilterState,
                                    onStateFilterChange = viewModel::updateTorrentListStateFilter,
                                    onCategoryFilterChange = viewModel::updateTorrentListCategoryFilter,
                                    onTagFilterChange = viewModel::updateTorrentListTagFilter,
                                    categoryOptions = state.categoryOptions,
                                    tagOptions = state.tagOptions,
                                    visibleItems = visibleTorrentItems,
                                    isPendingAction = ::isPendingAction,
                                    onOpenDetails = ::openTorrentDetail,
                                    onOpenConnection = { openSettings() },
                                )
                            }

                            AppPage.TORRENT_DETAIL -> torrentDetailPageContent(
                                selectedTorrent = selectedTorrent,
                                selectedTorrentIdentity = selectedTorrentIdentity,
                                showRestorePlaceholder = showTorrentDetailRestorePlaceholder,
                                crossSeedCounts = crossSeedCounts,
                                state = detailState,
                                isPendingAction = ::isPendingAction,
                                onCopyHash = { hash ->
                                    copyToClipboard(hash, R.string.detail_hash_copied)
                                },
                                onCopyMagnet = { magnetUri ->
                                                    copyToClipboard(magnetUri, R.string.detail_magnet_copied)
                                },
                                onExportTorrent = { hash, name ->
                                                    requestTorrentExport(
                                                        hash = hash,
                                                        torrentName = name,
                                                    )
                                },
                                onPauseTorrent = viewModel::pauseTorrent,
                                onResumeTorrent = viewModel::resumeTorrent,
                                onDeleteTorrent = viewModel::deleteTorrent,
                                onRenameTorrent = viewModel::renameTorrent,
                                onSetTorrentLocation = viewModel::setTorrentLocation,
                                onSetTorrentCategory = viewModel::setTorrentCategory,
                                onSetTorrentTags = viewModel::setTorrentTags,
                                onSetTorrentSpeedLimit = viewModel::setTorrentSpeedLimit,
                                onSetTorrentShareRatio = viewModel::setTorrentShareRatio,
                                onReannounceTorrent = viewModel::reannounceTorrent,
                                onRecheckTorrent = viewModel::recheckTorrent,
                                onCopyTracker = { tracker ->
                                    copyToClipboard(tracker.url, R.string.detail_tracker_copied)
                                },
                                onEditTracker = { hash, tracker, newUrl ->
                                    viewModel.editTracker(hash, tracker, newUrl)
                                },
                                onDeleteTracker = { hash, tracker ->
                                    viewModel.removeTracker(hash, tracker)
                                },
                            )

                            AppPage.SETTINGS -> settingsRootPageContent(
                                state = state,
                                onAppLanguageChange = viewModel::updateAppLanguage,
                                onDeleteFilesWhenNoSeedersChange = viewModel::updateDeleteFilesWhenNoSeeders,
                                onDeleteFilesDefaultChange = viewModel::updateDeleteFilesDefault,
                                onBackendTypeChange = viewModel::updateServerBackendType,
                                onHostChange = viewModel::updateHost,
                                onPortChange = viewModel::updatePort,
                                onHttpsChange = viewModel::updateUseHttps,
                                onUserChange = viewModel::updateUsername,
                                onPasswordChange = viewModel::updatePassword,
                                onRefreshSecondsChange = viewModel::updateRefreshSeconds,
                                onConnect = {
                                    viewModel.connect()
                                    currentPage = AppPage.DASHBOARD
                                },
                            )
                        }
                    }
                }

                val animatedPageTarget = remember(currentPage, serverDashboardSessionKey, pageThemeSignature) {
                    PageAnimationState(
                        page = currentPage,
                        dashboardSessionKey = if (currentPage == AppPage.SERVER_DASHBOARD) {
                            serverDashboardSessionKey
                        } else {
                            ""
                        },
                        themeSignature = pageThemeSignature,
                    )
                }
                val animatedPageContent: @Composable () -> Unit = {
                    AnimatedContent(
                        targetState = animatedPageTarget,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 140))
                        },
                        label = "pageAnimation",
                    ) { pageState ->
                        key(pageState.page, pageState.dashboardSessionKey, pageState.themeSignature) {
                            contentList(pageState.page)
                        }
                    }
                }

                if (
                    (currentPage == AppPage.DASHBOARD || currentPage == AppPage.SERVER_DASHBOARD) &&
                    (state.connected || state.serverProfiles.isNotEmpty())
                ) {
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = state.isManualRefreshing,
                        onRefresh = { viewModel.refresh(manual = true) },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState),
                    ) {
                        animatedPageContent()
                        PullRefreshIndicator(
                            refreshing = state.isManualRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    animatedPageContent()
                }
            }

            if (showServerProfileSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showServerProfileSheet = false
                        serverSheetEditingProfileId = ""
                    },
                    sheetState = serverProfileSheetState,
                    containerColor = qbGlassStrongContainerColor(),
                    shape = PanelShape,
                    windowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ) {
                    ServerProfileSheet(
                        profiles = state.serverProfiles,
                        activeProfileId = state.activeServerProfileId,
                        initialEditingProfileId = serverSheetEditingProfileId.ifBlank { null },
                        onSwitchProfile = { profileId ->
                            viewModel.switchServerProfile(profileId)
                            showServerProfileSheet = false
                            serverSheetEditingProfileId = ""
                            currentPage = AppPage.DASHBOARD
                        },
                        onAddProfile = { name, backendType, host, port, useHttps, username, password, refreshSeconds ->
                            viewModel.addServerProfile(
                                name = name,
                                backendType = backendType,
                                host = host,
                                port = port,
                                useHttps = useHttps,
                                username = username,
                                password = password,
                                refreshSeconds = refreshSeconds,
                            )
                            showServerProfileSheet = false
                            serverSheetEditingProfileId = ""
                            currentPage = AppPage.DASHBOARD
                        },
                        onUpdateProfile = { profileId, name, backendType, host, port, useHttps, username, password, refreshSeconds ->
                            viewModel.updateServerProfile(
                                profileId = profileId,
                                name = name,
                                backendType = backendType,
                                host = host,
                                port = port,
                                useHttps = useHttps,
                                username = username,
                                password = password,
                                refreshSeconds = refreshSeconds,
                            )
                            showServerProfileSheet = false
                            serverSheetEditingProfileId = ""
                        },
                        onRequestDeleteProfile = ::requestDeleteServerProfile,
                        onCancel = {
                            showServerProfileSheet = false
                            serverSheetEditingProfileId = ""
                        },
                    )
                }
            }

            if (showAddTorrentSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddTorrentSheet = false },
                    sheetState = addTorrentSheetState,
                    containerColor = qbGlassStrongContainerColor(),
                    shape = PanelShape,
                    windowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ) {
                    AddTorrentSheet(
                        context = localContext,
                        capabilities = state.activeCapabilities,
                        categoryOptions = categoryOptionsForAdd,
                        tagOptions = tagOptionsForAdd,
                        pathOptions = pathOptionsForAdd,
                        initialUrls = state.sharedMagnetUrl,
                        onCancel = {
                            showAddTorrentSheet = false
                            viewModel.clearSharedMagnetUrl()
                        },
                        onAdd = { urls, files, autoTmm, category, tags, savePath, paused, skipChecking, sequential, firstLast, upKb, dlKb ->
                            viewModel.addTorrent(
                                urls = urls,
                                files = files,
                                autoTmm = autoTmm,
                                category = category,
                                tags = tags,
                                savePath = savePath,
                                paused = paused,
                                skipChecking = skipChecking,
                                sequentialDownload = sequential,
                                firstLastPiecePrio = firstLast,
                                uploadLimitKb = upKb,
                                downloadLimitKb = dlKb,
                            )
                            showAddTorrentSheet = false
                        },
                    )
                }
            }

            if (showDashboardCardManagerSheet && selectedServerProfile != null) {
                ModalBottomSheet(
                    onDismissRequest = { showDashboardCardManagerSheet = false },
                    sheetState = dashboardCardManagerSheetState,
                    containerColor = qbGlassStrongContainerColor(),
                    shape = PanelShape,
                    windowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ) {
                    ServerDashboardCardManagerSheet(
                        availableCards = availableDashboardCards,
                        preferences = displayDashboardPreferences,
                        onToggleCard = { card, visible ->
                            val currentProfileId = selectedServerProfile.id
                            val previousVisibleKeys = localVisibleDashboardCardKeys
                            localVisibleDashboardCardKeys = if (visible) {
                                previousVisibleKeys + card.storageKey
                            } else {
                                previousVisibleKeys - card.storageKey
                            }
                            viewModel.updateServerDashboardCardVisibility(
                                currentProfileId,
                                card,
                                visible,
                            ) { success ->
                                if (!success && currentProfileId == state.activeServerProfileId) {
                                    localVisibleDashboardCardKeys = previousVisibleKeys
                                }
                            }
                        },
                        onReset = {
                            val currentProfileId = selectedServerProfile.id
                            val previousOrder = localDashboardCardOrder
                            val previousVisibleKeys = localVisibleDashboardCardKeys
                            val defaults = defaultServerDashboardPreferencesForBackend(selectedDashboardBackendType)
                            localDashboardCardOrder = parseDashboardCardOrder(
                                defaults.cardOrder,
                                availableDashboardCards,
                            )
                            localVisibleDashboardCardKeys = defaults.visibleCards.toSet()
                            viewModel.resetServerDashboardPreferences(currentProfileId) { success ->
                                if (!success && currentProfileId == state.activeServerProfileId) {
                                    localDashboardCardOrder = previousOrder
                                    localVisibleDashboardCardKeys = previousVisibleKeys
                                }
                            }
                        },
                        onDismiss = { showDashboardCardManagerSheet = false },
                    )
                }
            }

            if (pendingDeleteProfile != null) {
                AlertDialog(
                    onDismissRequest = { pendingDeleteProfileId = "" },
                    title = { Text(stringResource(R.string.server_delete_title)) },
                    text = {
                        Text(
                            if (pendingDeleteProfile.id == state.activeServerProfileId) {
                                stringResource(
                                    R.string.server_delete_desc_active,
                                    pendingDeleteProfile.name,
                                )
                            } else {
                                stringResource(
                                    R.string.server_delete_desc,
                                    pendingDeleteProfile.name,
                                )
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (
                                    currentPage == AppPage.SERVER_DASHBOARD &&
                                    pendingDeleteProfile.id == state.activeServerProfileId
                                ) {
                                    currentPage = AppPage.DASHBOARD
                                }
                                viewModel.deleteServerProfile(pendingDeleteProfile.id)
                                pendingDeleteProfileId = ""
                                showServerProfileSheet = false
                                serverSheetEditingProfileId = ""
                            },
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteProfileId = "" }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            state.pendingBackendRepair?.let { pendingRepair ->
                val detectedBackendLabel = when (pendingRepair.detectedBackend) {
                    ServerBackendType.QBITTORRENT -> "qBittorrent"
                    ServerBackendType.TRANSMISSION -> "Transmission"
                }
                AlertDialog(
                    onDismissRequest = viewModel::dismissPendingBackendRepair,
                    title = { Text(stringResource(R.string.server_backend_repair_title)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.server_backend_repair_desc,
                                pendingRepair.profileName,
                                detectedBackendLabel,
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::confirmPendingBackendRepair) {
                            Text(stringResource(R.string.server_backend_repair_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissPendingBackendRepair) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            // 全局限速设置对话框
            if (state.showGlobalSpeedLimitDialog) {
                GlobalSpeedLimitDialog(
                    limits = state.globalSpeedLimits,
                    loading = state.globalSpeedLimitLoading,
                    serverProfiles = state.serverProfiles,
                    selectedProfileId = state.speedLimitTargetProfileId,
                    onSelectServer = viewModel::switchSpeedLimitServer,
                    onDismiss = viewModel::dismissGlobalSpeedLimitDialog,
                    onSave = viewModel::saveGlobalSpeedLimits,
                )
            }

        }
    }
}

@Composable
private fun CrossSeedDetailSummaryCard(
    sourceName: String,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = qbGlassChipColor(),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.cross_seed_detail_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (sourceName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.cross_seed_detail_source_fmt, sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.cross_seed_detail_count_fmt, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CrossSeedDetailCard(torrent: TorrentInfo) {
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
    val savePathText = torrent.savePath.ifBlank { "-" }
    val siteText = trackerSiteName(
        tracker = torrent.tracker,
        unknownLabel = stringResource(R.string.site_unknown),
    )
    val stateStyle = torrentStateStyle(effectiveState)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.3f)),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = torrent.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.added_fmt, formatAddedOn(torrent.addedOn)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = siteText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    item {
                        TorrentMetaChip(
                            text = tagsText,
                            containerColor = Color(0xFF0B8F6F),
                            contentColor = Color(0xFFE1FFF4),
                        )
                    }
                    item {
                        TorrentStateTag(
                            label = stateLabel,
                            style = stateStyle,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.last_activity_fmt, formatActiveAgo(torrent.lastActivity)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            TorrentInfoCell(
                text = stringResource(
                    R.string.torrent_speed_fmt,
                    formatSpeed(torrent.uploadSpeed),
                    formatSpeed(torrent.downloadSpeed),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TorrentInfoCell(
                    text = stringResource(
                        R.string.torrent_uploaded_downloaded_fmt,
                        formatBytes(torrent.uploaded),
                        formatBytes(torrent.downloaded),
                    ),
                    modifier = Modifier.weight(1f),
                )
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_size_fmt, formatBytes(torrent.size)),
                    modifier = Modifier.weight(1f),
                )
            }

            TorrentInfoCell(
                text = stringResource(R.string.torrent_category_fmt, categoryText),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_ratio_fmt, formatRatio(torrent.ratio)),
                    modifier = Modifier.weight(1f),
                )
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_seed_count_fmt, torrent.seeders, torrent.numComplete),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TorrentInfoCell(
                    text = stringResource(R.string.torrent_peer_count_fmt, torrent.leechers, torrent.numIncomplete),
                    modifier = Modifier.weight(1f),
                )
                TorrentInfoCell(
                    text = stringResource(R.string.cross_seed_detail_site_fmt, siteText),
                    modifier = Modifier.weight(1f),
                )
            }

            TorrentInfoCell(
                text = stringResource(R.string.torrent_save_path_fmt, savePathText),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun ServerOverviewCard(
    overviewTitle: String,
    backendType: ServerBackendType,
    serverProfiles: List<ServerProfile>,
    activeProfileId: String?,
    serverVersion: String,
    transferInfo: TransferInfo,
    torrents: List<TorrentInfo>,
    torrentCount: Int,
    showTotals: Boolean,
    showEntryHint: Boolean,
    onCardClick: (() -> Unit)?,
    onDismissEntryHint: () -> Unit,
    onOpenTorrentList: () -> Unit,
    onSwitchServerProfile: (String) -> Unit,
    onEditServerProfile: (String) -> Unit,
    onRequestDeleteServerProfile: (String) -> Unit,
    swipeActionsEnabled: Boolean,
    showSwipeHint: Boolean,
    onDismissSwipeHint: () -> Unit,
    supportsGlobalSpeedLimit: Boolean = false,
    onToggleAltSpeed: (() -> Unit)? = null,
    onOpenSpeedSettings: (() -> Unit)? = null,
) {
    val stateSummary = remember(torrents) { buildDashboardStateSummary(torrents) }
    val activeProfile = remember(serverProfiles, activeProfileId) {
        serverProfiles.firstOrNull { it.id == activeProfileId }
    }
    val uploadLimitText = formatRateLimit(
        value = transferInfo.uploadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val downloadLimitText = formatRateLimit(
        value = transferInfo.downloadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val pausedTotal = stateSummary.pausedUploadCount + stateSummary.pausedDownloadCount
    val statusUploadingLabel = stringResource(R.string.status_uploading)
    val statusDownloadingLabel = stringResource(R.string.status_downloading)
    val statusPausedLabel = stringResource(R.string.status_paused)
    val statusErrorLabel = stringResource(R.string.status_error)
    val statusCheckingLabel = stringResource(R.string.status_checking)
    val statusWaitingLabel = stringResource(R.string.status_waiting)
    val statusTotalLabel = stringResource(R.string.status_total_torrents)
    val statusPills = remember(
        stateSummary,
        torrentCount,
        statusUploadingLabel,
        statusDownloadingLabel,
        statusPausedLabel,
        statusErrorLabel,
        statusCheckingLabel,
        statusWaitingLabel,
        statusTotalLabel,
    ) {
        listOf(
            DashboardStatusPillItem(
                label = statusUploadingLabel,
                count = stateSummary.uploadingCount,
                accentColor = Color(0xFF3BBA6F),
            ),
            DashboardStatusPillItem(
                label = statusDownloadingLabel,
                count = stateSummary.downloadingCount,
                accentColor = Color(0xFF3990FF),
            ),
            DashboardStatusPillItem(
                label = statusPausedLabel,
                count = pausedTotal,
                accentColor = Color(0xFF8D98A8),
            ),
            DashboardStatusPillItem(
                label = statusErrorLabel,
                count = stateSummary.errorCount,
                accentColor = Color(0xFFE1493D),
            ),
            DashboardStatusPillItem(
                label = statusCheckingLabel,
                count = stateSummary.checkingCount,
                accentColor = Color(0xFFE1A22B),
            ),
            DashboardStatusPillItem(
                label = statusWaitingLabel,
                count = stateSummary.waitingCount,
                accentColor = Color(0xFFA674E8),
            ),
            DashboardStatusPillItem(
                label = statusTotalLabel,
                count = torrentCount,
                accentColor = Color(0xFF11A9B5),
            ),
        )
    }
    val actionWidth = 104.dp
    val actionWidthPx = with(LocalDensity.current) { 104.dp.toPx() }
    var revealOffset by rememberSaveable(activeProfileId) { mutableFloatStateOf(0f) }
    val animatedRevealOffset by animateFloatAsState(
        targetValue = revealOffset,
        label = "serverOverviewRevealOffset",
    )
    LaunchedEffect(activeProfile?.id, swipeActionsEnabled) {
        revealOffset = 0f
    }
    val showActionRail = swipeActionsEnabled &&
        activeProfile != null &&
        animatedRevealOffset <= -(actionWidthPx * 0.42f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape),
    ) {
        if (showActionRail) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(actionWidth),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServerOverviewActionButton(
                        iconRes = R.drawable.ic_action_edit,
                        description = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            revealOffset = 0f
                            activeProfile?.id?.let(onEditServerProfile)
                        },
                    )
                    ServerOverviewActionButton(
                        iconRes = R.drawable.ic_action_delete,
                        description = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            revealOffset = 0f
                            activeProfile?.id?.let(onRequestDeleteServerProfile)
                        },
                    )
                }
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedRevealOffset }
                .pointerInput(activeProfile?.id) {
                    if (activeProfile == null || !swipeActionsEnabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (showSwipeHint) {
                                onDismissSwipeHint()
                            }
                            revealOffset = (revealOffset + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                        onDragEnd = {
                            revealOffset = if (revealOffset <= -(actionWidthPx * 0.45f)) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
                    )
                }
                .then(
                    if (onCardClick != null) {
                        Modifier.clickable {
                            if (revealOffset < 0f) {
                                revealOffset = 0f
                            } else {
                                onCardClick()
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
            shape = PanelShape,
            border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.28f)),
            colors = qbGlassCardColors(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = backendOverviewIconRes(backendType)),
                        contentDescription = overviewTitle,
                        modifier = Modifier.size(34.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = overviewTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.server_version_fmt, serverVersion),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onOpenTorrentList) {
                        Text(
                            text = stringResource(R.string.dashboard_open_torrents),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (serverProfiles.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        items(serverProfiles, key = { it.id }) { profile ->
                            ServerProfileSummaryCard(
                                profile = profile,
                                active = profile.id == activeProfileId,
                                addressText = buildServerAddressText(
                                    ConnectionSettings(
                                        host = profile.host,
                                        port = profile.port,
                                        useHttps = profile.useHttps,
                                    ),
                                ),
                                summaryText = if (profile.id == activeProfileId) {
                                    stringResource(
                                        R.string.server_summary_speed_fmt,
                                        formatSpeed(transferInfo.uploadSpeed),
                                        formatSpeed(transferInfo.downloadSpeed),
                                    )
                                } else {
                                    stringResource(R.string.server_profile_saved)
                                },
                                onSwitch = {
                                    revealOffset = 0f
                                    onSwitchServerProfile(profile.id)
                                },
                                onEdit = {
                                    revealOffset = 0f
                                    onEditServerProfile(profile.id)
                                },
                                onDelete = {
                                    revealOffset = 0f
                                    onRequestDeleteServerProfile(profile.id)
                                },
                            )
                        }
                    }
                }

                if (showEntryHint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        DashboardEntryHintBubble(
                            text = stringResource(R.string.dashboard_open_torrents_hint),
                            dismissDescription = stringResource(R.string.dismiss_hint),
                            onDismiss = onDismissEntryHint,
                        )
                    }
                }
                if (showSwipeHint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        DashboardEntryHintBubble(
                            text = stringResource(R.string.dashboard_swipe_hint),
                            dismissDescription = stringResource(R.string.dismiss_hint),
                            onDismiss = onDismissSwipeHint,
                        )
                    }
                }

                DashboardSecondaryStatsBlock(
                    uploadSpeedText = formatSpeed(transferInfo.uploadSpeed),
                    downloadSpeedText = formatSpeed(transferInfo.downloadSpeed),
                    uploadLimitText = uploadLimitText,
                    downloadLimitText = downloadLimitText,
                    showTotals = showTotals,
                    totalDownloadedText = formatBytes(transferInfo.downloadedTotal),
                    totalUploadedText = formatBytes(transferInfo.uploadedTotal),
                    useAltSpeedLimits = transferInfo.useAltSpeedLimits,
                    supportsGlobalSpeedLimit = supportsGlobalSpeedLimit,
                    onToggleAltSpeed = onToggleAltSpeed,
                    onOpenSpeedSettings = onOpenSpeedSettings,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    items(statusPills, key = { it.label }) { pill ->
                        DashboardStatusPill(
                            label = pill.label,
                            count = pill.count,
                            accentColor = pill.accentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MultiServerDashboardSection(
    aggregate: DashboardAggregateState,
    snapshots: List<CachedDashboardServerSnapshot>,
    draggingProfileId: String?,
    settlingProfileId: String?,
    draggingOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<String>?,
    showReorderHint: Boolean,
    onDismissReorderHint: () -> Unit,
    onStartDrag: (String) -> Unit,
    onDragDelta: (String, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onOpenServerDashboard: (String) -> Unit,
    onOpenSpeedSettings: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DashboardAggregateOverviewCard(
            aggregate = aggregate,
            onOpenSpeedSettings = onOpenSpeedSettings,
        )
        WalletServerCardStack(
            snapshots = snapshots,
            draggingProfileId = draggingProfileId,
            settlingProfileId = settlingProfileId,
            draggingOffsetY = draggingOffsetY,
            settlingOffsetY = settlingOffsetY,
            draggingTargetIndex = draggingTargetIndex,
            dragSession = dragSession,
            showReorderHint = showReorderHint,
            onDismissReorderHint = onDismissReorderHint,
            onStartDrag = onStartDrag,
            onDragDelta = onDragDelta,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onOpenServerDashboard = onOpenServerDashboard,
        )
    }
}

@Composable
private fun DashboardAggregateOverviewCard(
    aggregate: DashboardAggregateState,
    onOpenSpeedSettings: () -> Unit = {},
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_server_speed_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.dashboard_server_speed_online_count,
                        aggregate.totalServerCount,
                        aggregate.totalServerCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AggregateSpeedTextLine(
                label = stringResource(R.string.sort_upload_speed),
                value = formatSpeed(aggregate.transferInfo.uploadSpeed),
                accent = Color(0xFF3BBA6F),
            )
            AggregateSpeedTextLine(
                label = stringResource(R.string.sort_download_speed),
                value = formatSpeed(aggregate.transferInfo.downloadSpeed),
                accent = Color(0xFF3990FF),
            )
            InlineRealtimeSpeedChart(
                aggregate = aggregate,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.global_speed_limit_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenSpeedSettings() },
                )
            }
        }
    }
}

@Composable
private fun AggregateSpeedTextLine(
    label: String,
    value: String,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accent, RoundedCornerShape(99.dp)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun WalletServerCardStack(
    snapshots: List<CachedDashboardServerSnapshot>,
    draggingProfileId: String?,
    settlingProfileId: String?,
    draggingOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    draggingTargetIndex: Int,
    dragSession: VerticalReorderSession<String>?,
    showReorderHint: Boolean,
    onDismissReorderHint: () -> Unit,
    onStartDrag: (String) -> Unit,
    onDragDelta: (String, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onOpenServerDashboard: (String) -> Unit,
) {
    val orderedSnapshots = snapshots
    val paletteIndexByProfileId = remember(orderedSnapshots) {
        orderedSnapshots
            .map { it.profileId }
            .distinct()
            .sorted()
            .mapIndexed { index, profileId -> profileId to index }
            .toMap()
    }
    val serverStackGestureKey = remember(orderedSnapshots) {
        orderedSnapshots.joinToString(separator = "|") { it.profileId }
    }
    val stackHeight = if (orderedSnapshots.isEmpty()) {
        0.dp
    } else {
        HomeServerStackExpandedCardHeight + HomeServerStackExposedHeight * (orderedSnapshots.size - 1)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.server_stack_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.server_stack_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showReorderHint) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DashboardEntryHintBubble(
                        text = stringResource(R.string.server_stack_reorder_hint),
                        dismissDescription = stringResource(R.string.dismiss_hint),
                        onDismiss = onDismissReorderHint,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stackHeight),
        ) {
            orderedSnapshots.withIndex().toList().asReversed().forEach { (index, snapshot) ->
                key(snapshot.profileId) {
                    val isSettlingCard = settlingProfileId == snapshot.profileId
                    WalletServerStackCard(
                        snapshot = snapshot,
                        gestureKey = serverStackGestureKey,
                        stackedIndex = index,
                        paletteIndex = paletteIndexByProfileId[snapshot.profileId] ?: index,
                        cardHeight = HomeServerStackExpandedCardHeight,
                        collapsedCardHeight = HomeServerStackCollapsedCardHeight,
                        exposedHeight = HomeServerStackExposedHeight,
                        selected = index == 0,
                        stackCount = orderedSnapshots.size,
                        isDragging = draggingProfileId == snapshot.profileId && !isSettlingCard,
                        isSettling = isSettlingCard,
                        dragOffsetY = { if (draggingProfileId == snapshot.profileId && !isSettlingCard) draggingOffsetY() else 0f },
                        settlingOffsetY = { if (isSettlingCard) settlingOffsetY() else 0f },
                        siblingOffsetY = calculateServerStackSiblingOffset(
                            profileId = snapshot.profileId,
                            draggingProfileId = draggingProfileId,
                            draggingTargetIndex = draggingTargetIndex,
                            dragSession = dragSession,
                        ),
                        animateSiblingOffset = dragSession != null,
                        onDragStart = { onStartDrag(snapshot.profileId) },
                        onDragDelta = { deltaY -> onDragDelta(snapshot.profileId, deltaY) },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onClick = { onOpenServerDashboard(snapshot.profileId) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun WalletServerStackCard(
    snapshot: CachedDashboardServerSnapshot,
    gestureKey: String,
    stackedIndex: Int,
    paletteIndex: Int,
    cardHeight: Dp,
    collapsedCardHeight: Dp,
    exposedHeight: Dp,
    selected: Boolean,
    stackCount: Int,
    isDragging: Boolean,
    isSettling: Boolean,
    dragOffsetY: () -> Float,
    settlingOffsetY: () -> Float,
    siblingOffsetY: Float,
    animateSiblingOffset: Boolean,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = remember(snapshot.profileId, paletteIndex) {
        walletCardPalette(paletteIndex)
    }
    val stateLabel = if (snapshot.isStale) {
        stringResource(R.string.server_snapshot_stale_state)
    } else {
        stringResource(R.string.server_snapshot_live_state)
    }
    val exposedStepPx = with(LocalDensity.current) { exposedHeight.toPx() }
    val cornerShape = RoundedCornerShape(24.dp)
    val collapsedShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )
    val uploadLimitText = formatRateLimit(
        value = snapshot.transferInfo.uploadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val downloadLimitText = formatRateLimit(
        value = snapshot.transferInfo.downloadRateLimit,
        unlimitedLabel = stringResource(R.string.limit_unlimited),
    )
    val serverNameTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 18.sp,
        lineHeight = 22.sp,
    )
    val clickSuppressionThresholdPx = with(LocalDensity.current) {
        ServerCardClickSuppressionDragThreshold.toPx()
    }
    val presentation = remember(selected, isDragging, isSettling) {
        resolveWalletServerStackCardPresentation(
            selected = selected,
            isDragging = isDragging,
            isSettling = isSettling,
        )
    }
    val cardShape = if (presentation.showExpandedLayout) cornerShape else collapsedShape
    var isPressed by remember(snapshot.profileId) { mutableStateOf(false) }
    var lastDragFinishedAt by remember(snapshot.profileId) { mutableLongStateOf(0L) }
    var dragDistanceSinceStart by remember(snapshot.profileId) { mutableFloatStateOf(0f) }
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragDelta by rememberUpdatedState(onDragDelta)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)
    val latestOnClick by rememberUpdatedState(onClick)
    val animatedSiblingOffset by animateFloatAsState(
        targetValue = siblingOffsetY,
        animationSpec = if (animateSiblingOffset) {
            ReorderSiblingOffsetAnimationSpec
        } else {
            snap()
        },
        label = "walletServerSiblingOffset",
    )
    val draggedScale by animateFloatAsState(
        targetValue = if (isDragging) ReorderDraggedScale else 1f,
        animationSpec = ReorderScaleAnimationSpec,
        label = "walletServerDraggedScale",
    )
    val baseTranslationY = if (selected) {
        (stackCount - 1) * exposedStepPx
    } else {
        (stackCount - 1 - stackedIndex) * exposedStepPx
    }
    val displayHeight = if (presentation.showExpandedLayout) cardHeight else collapsedCardHeight
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed && !isDragging) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "walletServerPressedScale",
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(displayHeight)
            .graphicsLayer {
                translationY = baseTranslationY + when {
                    isDragging -> dragOffsetY()
                    isSettling -> settlingOffsetY()
                    else -> animatedSiblingOffset
                }
                scaleX = when {
                    isDragging -> draggedScale
                    presentation.showExpandedLayout -> 1f
                    else -> 0.992f
                } * pressedScale
                scaleY = when {
                    isDragging -> draggedScale
                    presentation.showExpandedLayout -> 1f
                    else -> 0.992f
                } * pressedScale
                shadowElevation = when {
                    isDragging -> ReorderDraggedShadow
                    isSettling -> ReorderSettlingShadow
                    selected -> ReorderSelectedShadow
                    else -> ReorderCollapsedShadow
                }
                shape = cardShape
                // 不在 graphicsLayer 内 clip，与 shadowElevation 组合会导致残影
            }
            .clip(cardShape)
            .pointerInput(snapshot.profileId, gestureKey) {
                if (stackCount < 2) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isPressed = false
                        dragDistanceSinceStart = 0f
                        latestOnDragStart()
                    },
                    onDragEnd = {
                        isPressed = false
                        lastDragFinishedAt = resolveServerCardClickSuppressionTimestamp(
                            dragDistanceSinceStart = dragDistanceSinceStart,
                            clickSuppressionThresholdPx = clickSuppressionThresholdPx,
                            currentTimeMillis = SystemClock.elapsedRealtime(),
                        )
                        latestOnDragEnd()
                    },
                    onDragCancel = {
                        isPressed = false
                        lastDragFinishedAt = resolveServerCardClickSuppressionTimestamp(
                            dragDistanceSinceStart = dragDistanceSinceStart,
                            clickSuppressionThresholdPx = clickSuppressionThresholdPx,
                            currentTimeMillis = SystemClock.elapsedRealtime(),
                        )
                        latestOnDragCancel()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceSinceStart += kotlin.math.abs(dragAmount.y)
                        latestOnDragDelta(dragAmount.y)
                    },
                )
            }
            .pointerInput(snapshot.profileId, gestureKey, lastDragFinishedAt) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        if (
                            shouldSuppressServerCardClick(
                                lastDragFinishedAt = lastDragFinishedAt,
                                currentTimeMillis = SystemClock.elapsedRealtime(),
                            )
                        ) {
                            return@detectTapGestures
                        }
                        isPressed = false
                        latestOnClick()
                    }
                )
            }
            .zIndex(
                if (isDragging || isSettling) {
                    (stackCount + 1).toFloat()
                } else {
                    (stackCount - stackedIndex).toFloat()
                },
            ),
        shape = cardShape,
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = presentation.borderAlpha),
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = palette.background,
                    shape = cardShape,
                )
                .padding(
                    horizontal = presentation.horizontalPadding,
                    vertical = presentation.verticalPadding,
                ),
        ) {
            if (presentation.showExpandedLayout) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = snapshot.profileName.ifBlank { backendLabel(snapshot.backendType) },
                                style = serverNameTextStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = backendLabel(snapshot.backendType),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.server_summary_speed_fmt,
                                    formatSpeed(snapshot.transferInfo.uploadSpeed),
                                    formatSpeed(snapshot.transferInfo.downloadSpeed),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    
                    WalletCardMetricLine(
                        label = stringResource(R.string.sort_total_uploaded),
                        value = formatBytes(snapshot.transferInfo.uploadedTotal),
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.sort_total_downloaded),
                        value = formatBytes(snapshot.transferInfo.downloadedTotal),
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.upload_limit_kb_label),
                        value = uploadLimitText,
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.download_limit_kb_label),
                        value = downloadLimitText,
                    )
                    WalletCardMetricLine(
                        label = stringResource(R.string.free_space_label),
                        value = formatBytes(snapshot.transferInfo.freeSpaceOnDisk),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = snapshot.profileName.ifBlank { backendLabel(snapshot.backendType) },
                        style = serverNameTextStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(
                                R.string.server_summary_speed_fmt,
                                formatSpeed(snapshot.transferInfo.uploadSpeed),
                                formatSpeed(snapshot.transferInfo.downloadSpeed),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accent,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletCardMetricLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.76f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
internal fun DashboardManagementEmptyCard(
    onOpenManager: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_all_cards_hidden_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.dashboard_all_cards_hidden_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onOpenManager) {
                Text(stringResource(R.string.dashboard_manage_cards_action))
            }
        }
    }
}

@Composable
internal fun PageRestorePlaceholder() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, qbGlassOutlineColor(defaultAlpha = 0.28f)),
        colors = qbGlassCardColors(),
    ) {
        Text(
            text = stringResource(R.string.loading),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun ServerDashboardSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ServerDashboardSkeletonCard(
            titleWidthFraction = 0.34f,
            bodyHeight = 132.dp,
        )
        ServerDashboardSkeletonCard(
            titleWidthFraction = 0.42f,
            bodyHeight = 182.dp,
        )
        ServerDashboardSkeletonCard(
            titleWidthFraction = 0.28f,
            bodyHeight = 168.dp,
        )
    }
}

@Composable
private fun ServerDashboardSkeletonCard(
    titleWidthFraction: Float,
    bodyHeight: Dp,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, qbGlassOutlineColor()),
        colors = qbGlassCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(titleWidthFraction)
                    .height(16.dp)
                    .background(
                        color = qbGlassSubtleContainerColor(),
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bodyHeight)
                    .background(
                        color = qbGlassStrongContainerColor(),
                        shape = RoundedCornerShape(18.dp),
                    ),
            )
        }
    }
}

@Composable
private fun ServerDashboardCardManagerSheet(
    availableCards: List<DashboardChartCard>,
    preferences: ServerDashboardPreferences,
    onToggleCard: (DashboardChartCard, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_manage_cards_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        availableCards.forEach { card ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dashboardChartCardLabel(card),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = preferences.visibleCards.contains(card.storageKey),
                    onCheckedChange = { checked -> onToggleCard(card, checked) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.dashboard_manage_cards_reset))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    }
}
