package com.hjw.qbremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.hjw.qbremote.data.AppLanguage
import com.hjw.qbremote.data.AppTheme
import com.hjw.qbremote.data.BackendConnectionError
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot
import com.hjw.qbremote.data.DailyUploadTrackingSnapshot
import com.hjw.qbremote.data.DashboardCacheSnapshot
import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.MainActivity
import com.hjw.qbremote.TorrentWidgetProvider
import com.hjw.qbremote.data.TorrentRepository
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.AddTorrentFile
import com.hjw.qbremote.data.model.AddTorrentRequest
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.DailyCountryUploadStats
import com.hjw.qbremote.data.model.GlobalSpeedLimits
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

enum class RefreshScene {
    DASHBOARD,
    SERVER,
    TORRENT_DETAIL,
    SETTINGS,
}

data class DailyTagUploadStat(
    val tag: String,
    val uploadedBytes: Long,
    val torrentCount: Int,
    val isNoTag: Boolean = false,
)

data class RealtimeSpeedPoint(
    val timestamp: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val onlineServerCount: Int = 0,
)

data class DashboardAggregateState(
    val transferInfo: TransferInfo = TransferInfo(),
    val chartTransferInfo: TransferInfo? = null,
    val torrents: List<TorrentInfo> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<DailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryUploadRecord> = emptyList(),
    val realtimeSpeedSeries: List<RealtimeSpeedPoint> = emptyList(),
    val totalServerCount: Int = 0,
    val categoryCoverageServerCount: Int = 0,
    val countryCoverageServerCount: Int = 0,
)

data class PendingBackendRepair(
    val profileId: String,
    val profileName: String,
    val expectedBackend: ServerBackendType,
    val detectedBackend: ServerBackendType,
    val detail: String = "",
)

@androidx.compose.runtime.Immutable
class MainViewModel(
    internal val connectionStore: ConnectionStore,
    internal val repository: TorrentRepository,
) : ViewModel() {
    internal val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val torrentListFilterState = MutableStateFlow(TorrentListFilterState())
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val torrentListDisplayState: StateFlow<TorrentListDisplayState> = combine(
        _uiState.map { it.torrents }.distinctUntilChanged(),
        torrentListFilterState,
    ) { torrents, filterState ->
        torrents to filterState
    }.mapLatest { (torrents, filterState) ->
        withContext(Dispatchers.Default) {
            buildTorrentListDisplayState(
                torrents = torrents,
                filterState = filterState,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TorrentListDisplayState(),
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val serverDashboardDisplayState: StateFlow<ServerDashboardDisplayState> = _uiState
        .map(::buildServerDashboardDisplayInput)
        .distinctUntilChanged()
        .mapLatest { state ->
            withContext(Dispatchers.Default) {
                buildServerDashboardDisplayState(state)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ServerDashboardDisplayState(),
        )

    internal val backgroundJobManager = BackgroundJobManager(
        scope = viewModelScope,
        getState = { _uiState.value },
        onAutoRefresh = { refresh() },
        onHomeChartRefresh = { refreshHomeDashboardChartTransferInfo() },
    )
    private var countryPeerTrackerJob: Job? = null
    internal var dashboardCacheHydrationJob: Job? = null
    internal var dashboardAggregationJob: Job? = null
    internal var serverSchedulerJob: Job? = null
    private var autoConnectAttempted = false
    internal var isRefreshInProgress = false
    internal var hydratedDashboardScopeKey: String? = null
    private var initialSettingsLoaded = false
    private var initialServerProfilesLoaded = false
    private var initialDashboardCacheHydrated = false
    internal var initialDashboardSnapshotsHydrated = false
    internal var activeProfileRequestVersion = 0L
    private val previousTorrentStates = mutableMapOf<String, String>()

    internal val realtimeSpeedTracker = RealtimeSpeedTracker(connectionStore)
    private val dailyCountryUploadTracker = DailyCountryUploadTracker(connectionStore, repository)
    internal val serverRefreshMutex = Mutex()
    private val cachedProfileSettings = mutableMapOf<String, ConnectionSettings>()
    internal val nextServerRefreshAt = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            connectionStore.migrateLegacyPasswordIfNeeded()
            connectionStore.cleanupLegacyGlobalChartSettingsIfNeeded()
            supervisorScope {
                launch {
                    connectionStore.settingsFlow.collect { settings ->
                        _uiState.update { current ->
                            current.copy(
                                settings = settings,
                                activeCapabilities = repository.capabilitiesFor(settings),
                            )
                        }
                        hydrateDashboardCacheForCurrentScope()
                        markInitialSettingsLoaded()
                    }
                }
                launch {
                    connectionStore.serverProfilesFlow.collect { profilesState ->
                        val previousActiveProfileId = _uiState.value.activeServerProfileId
                        if (profilesState.activeProfileId != previousActiveProfileId) {
                            bumpActiveProfileRequestVersion()
                        }
                        pruneCachedProfileSettingsInMemory(profilesState.profiles)
                        repository.selectProfile(profilesState.activeProfileId)
                        val dashboardPreferences = connectionStore.loadServerDashboardPreferences()
                        val availableProfileIds = profilesState.profiles.map { profile -> profile.id }
                        val availableProfileIdSet = availableProfileIds.toHashSet()
                        _uiState.update { current ->
                            current.copy(
                                serverProfiles = profilesState.profiles,
                                serverDashboardPreferences = filterDashboardPreferencesForProfiles(
                                    preferences = dashboardPreferences,
                                    profiles = profilesState.profiles,
                                ),
                                activeServerProfileId = profilesState.activeProfileId,
                                selectedDashboardProfileId = resolvePreferredProfileId(
                                    availableIds = availableProfileIds,
                                    primaryCandidate = current.selectedDashboardProfileId,
                                    secondaryCandidate = profilesState.activeProfileId,
                                ),
                                pendingBackendRepair = current.pendingBackendRepair
                                    ?.takeIf { pending -> pending.profileId in availableProfileIdSet },
                            )
                        }
                        seedCachedSettingsForProfile(profilesState.activeProfileId)
                        hydrateDashboardCacheForCurrentScope()
                        hydrateDashboardServerSnapshots()
                        synchronizeServerScheduler()
                        startHomeChartRefresh()
                        autoConnectIfNeeded(_uiState.value.settings)
                        markInitialServerProfilesLoaded()
                    }
                }
            }
        }
    }

    internal fun updateTorrentSearchQuery(query: String) {
        torrentListFilterState.update { current ->
            if (current.query == query) current else current.copy(query = query)
        }
    }

    internal fun updateTorrentListSortOption(sortOption: TorrentListSortOption) {
        torrentListFilterState.update { current ->
            if (current.sortOption == sortOption) current else current.copy(sortOption = sortOption)
        }
    }

    internal fun updateTorrentListSortDirection(descending: Boolean) {
        torrentListFilterState.update { current ->
            if (current.descending == descending) current else current.copy(descending = descending)
        }
    }

    internal fun updateTorrentListStateFilter(stateFilter: TorrentStateFilter) {
        torrentListFilterState.update { current ->
            if (current.stateFilter == stateFilter) current else current.copy(stateFilter = stateFilter)
        }
    }

    internal fun updateTorrentListCategoryFilter(category: String) {
        torrentListFilterState.update { current ->
            val next = if (current.categoryFilter == category) "" else category
            if (current.categoryFilter == next) current else current.copy(categoryFilter = next)
        }
    }

    internal fun updateTorrentListTagFilter(tag: String) {
        torrentListFilterState.update { current ->
            val next = if (current.tagFilter == tag) "" else tag
            if (current.tagFilter == next) current else current.copy(tagFilter = next)
        }
    }

    private fun markInitialSettingsLoaded() {
        if (!initialSettingsLoaded) {
            initialSettingsLoaded = true
            maybeMarkStartupRestoreComplete()
        }
    }

    private fun markInitialServerProfilesLoaded() {
        if (!initialServerProfilesLoaded) {
            initialServerProfilesLoaded = true
            maybeMarkStartupRestoreComplete()
        }
    }

    internal fun markInitialDashboardCacheHydrated() {
        if (!initialDashboardCacheHydrated) {
            initialDashboardCacheHydrated = true
            maybeMarkStartupRestoreComplete()
        }
    }

    internal fun markInitialDashboardSnapshotsHydrated() {
        if (!initialDashboardSnapshotsHydrated) {
            initialDashboardSnapshotsHydrated = true
            maybeMarkStartupRestoreComplete()
        }
    }

    private fun maybeMarkStartupRestoreComplete() {
        if (
            _uiState.value.startupRestoreComplete ||
            !initialSettingsLoaded ||
            !initialServerProfilesLoaded ||
            !initialDashboardCacheHydrated ||
            !initialDashboardSnapshotsHydrated
        ) {
            return
        }
        _uiState.update { current ->
            if (current.startupRestoreComplete) current else current.copy(startupRestoreComplete = true)
        }
    }

    fun updateHost(value: String) = updateSettings { current ->
        val parsed = parseHostInputHints(value)
        current.copy(
            host = value,
            port = parsed?.port ?: current.port,
            useHttps = parsed?.useHttps ?: current.useHttps,
        )
    }
    fun updatePort(value: String) = updateSettings { it.copy(port = value.toIntOrNull() ?: 0) }
    fun updateUseHttps(value: Boolean) = updateSettings { it.copy(useHttps = value) }
    fun updateUsername(value: String) = updateSettings { it.copy(username = value) }
    fun updatePassword(value: String) = updateSettings { it.copy(password = value) }
    fun updateServerBackendType(value: ServerBackendType) = updateSettings { it.copy(serverBackendType = value) }
    fun updateRefreshSeconds(value: String) {
        val sec = value.toIntOrNull()?.coerceIn(5, 120) ?: 5
        updateSettings { it.copy(refreshSeconds = sec) }
    }

    fun updateAppLanguage(value: AppLanguage) = updateAndPersistSettings {
        it.copy(appLanguage = value)
    }

    fun updateAppTheme(value: AppTheme) = updateAndPersistSettings {
        it.copy(appTheme = value)
    }

    fun applyCustomThemeBackground(
        imagePath: String,
        toneIsLight: Boolean,
    ) = updateAndPersistSettings {
        it.copy(
            appTheme = AppTheme.CUSTOM,
            customBackgroundImagePath = imagePath,
            customBackgroundToneIsLight = toneIsLight,
        )
    }

    fun updateDeleteFilesDefault(value: Boolean) = updateAndPersistSettings {
        it.copy(deleteFilesDefault = value)
    }

    fun updateDeleteFilesWhenNoSeeders(value: Boolean) = updateAndPersistSettings {
        it.copy(deleteFilesWhenNoSeeders = value)
    }

    fun dismissHomeTorrentEntryHint() = updateAndPersistSettings {
        if (it.homeTorrentEntryHintDismissed) {
            it
        } else {
            it.copy(homeTorrentEntryHintDismissed = true)
        }
    }

    fun markDashboardHideHintSeen() = updateAndPersistSettings {
        if (it.hasSeenDashboardHideHint) it else it.copy(hasSeenDashboardHideHint = true)
    }

    fun markDashboardHiddenSnackSeen() = updateAndPersistSettings {
        if (it.hasSeenDashboardHiddenSnack) it else it.copy(hasSeenDashboardHiddenSnack = true)
    }

    fun updateRefreshScene(scene: RefreshScene) {
        _uiState.update { current ->
            if (current.refreshScene == scene) current else current.copy(refreshScene = scene)
        }
    }

    fun setDashboardReorderHold(profileId: String?) {
        val normalizedProfileId = profileId?.trim().orEmpty().ifBlank { null }
        var releaseResult = DashboardReorderHoldReleaseResult()
        _uiState.update { current ->
            if (normalizedProfileId != null) {
                if (current.dashboardRefreshHoldProfileId == normalizedProfileId) {
                    current
                } else {
                    current.copy(dashboardRefreshHoldProfileId = normalizedProfileId)
                }
            } else {
                releaseResult = releaseDashboardReorderHold(current)
                if (releaseResult.profileIdToRefreshImmediately == null) {
                    current
                } else {
                    current.copy(dashboardRefreshHoldProfileId = releaseResult.nextHeldProfileId)
                }
            }
        }
        val releasedProfileId = releaseResult.profileIdToRefreshImmediately ?: return
        nextServerRefreshAt[releasedProfileId] = 0L
        viewModelScope.launch {
            refreshServerSnapshotNow(
                profileId = releasedProfileId,
                showSelectedError = false,
            )
        }
    }

    fun prepareServerDashboardTransition(profileId: String) {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return
        _uiState.update { current ->
            current.copy(
                selectedDashboardProfileId = normalizedProfileId,
                dashboardSessionToken = current.dashboardSessionToken + 1L,
                isConnecting = true,
                connected = false,
                errorMessage = null,
                pendingBackendRepair = current.pendingBackendRepair
                    ?.takeUnless { it.profileId != normalizedProfileId },
                serverVersion = "-",
                transferInfo = TransferInfo(),
                torrents = emptyList(),
                dailyTagUploadDate = "",
                dailyTagUploadStats = emptyList(),
                dailyCountryUploadDate = "",
                dailyCountryUploadStats = emptyList(),
                categoryOptions = emptyList(),
                tagOptions = emptyList(),
                dashboardCacheHydrated = false,
                hasDashboardSnapshot = false,
                detailHash = "",
                detailLoading = false,
                detailProperties = null,
                detailFiles = emptyList(),
                detailTrackers = emptyList(),
                pendingActionKeys = emptySet(),
            )
        }
    }

    fun connect() {
        viewModelScope.launch {
            runCatching {
                val currentState = _uiState.value
                val targetProfileId = when {
                    !currentState.activeServerProfileId.isNullOrBlank() -> currentState.activeServerProfileId
                    currentState.settings.host.trim().isNotBlank() && currentState.settings.username.trim().isNotBlank() -> {
                        connectionStore.save(currentState.settings)
                        connectionStore.serverProfilesFlow.first().activeProfileId
                    }

                    else -> null
                } ?: error("请先添加服务器。")

                val targetSettings = connectionStore.switchToServerProfile(targetProfileId)
                repository.selectProfile(targetProfileId)
                bumpActiveProfileRequestVersion()
                val capabilities = repository.capabilitiesFor(targetSettings)
                _uiState.update { current ->
                    prepareConnectingProfileState(
                        current = current,
                        settings = targetSettings,
                        profileId = targetProfileId,
                        capabilities = capabilities,
                    )
                }
                hydrateDashboardCacheForCurrentScope(force = true)
                synchronizeServerScheduler()
                nextServerRefreshAt[targetProfileId] = 0L
                refreshServerSnapshotNow(
                    profileId = targetProfileId,
                    showSelectedError = true,
                    forceSettings = targetSettings,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connected = false,
                        errorMessage = error.message ?: VmErr.CONNECT_SERVER,
                    )
                }
            }
        }
    }

    fun addServerProfile(
        name: String,
        backendType: ServerBackendType,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val nextSettings = buildProfileSettingsDraft(
                    backendType = backendType,
                    host = host,
                    port = port,
                    useHttps = useHttps,
                    username = username,
                    password = password,
                    refreshSeconds = refreshSeconds,
                )

                val profile = connectionStore.addServerProfile(name = name, settings = nextSettings)
                val switched = connectionStore.switchToServerProfile(profile.id)
                repository.selectProfile(profile.id)
                bumpActiveProfileRequestVersion()
                val capabilities = repository.capabilitiesFor(switched)
                _uiState.update { current ->
                    prepareConnectingProfileState(
                        current = current,
                        settings = switched,
                        profileId = profile.id,
                        capabilities = capabilities,
                    )
                }
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: VmErr.ADD_SERVER)
                }
            }
            if (result.isSuccess) {
                hydrateDashboardCacheForCurrentScope(force = true)
                synchronizeServerScheduler()
                val profileId = _uiState.value.activeServerProfileId ?: return@launch
                nextServerRefreshAt[profileId] = 0L
                refreshServerSnapshotNow(profileId = profileId, showSelectedError = true)
            }
        }
    }

    fun updateServerProfile(
        profileId: String,
        name: String,
        backendType: ServerBackendType,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val wasActive = _uiState.value.activeServerProfileId == profileId
            val result = runCatching {
                val existingSettings = connectionStore.loadSettingsForProfile(profileId)
                    ?: error("服务器配置不存在")
                val nextSettings = buildProfileSettingsDraft(
                    baseSettings = existingSettings,
                    backendType = backendType,
                    host = host,
                    port = port,
                    useHttps = useHttps,
                    username = username,
                    password = password.ifBlank { existingSettings.password },
                    refreshSeconds = refreshSeconds,
                )
                connectionStore.updateServerProfile(
                    profileId = profileId,
                    name = name,
                    settings = nextSettings,
                    passwordOverride = password.takeIf { it.isNotBlank() },
                )
                repository.removeProfile(profileId)
                nextServerRefreshAt[profileId] = 0L
                if (wasActive) {
                    val switched = connectionStore.switchToServerProfile(profileId)
                    repository.selectProfile(profileId)
                    bumpActiveProfileRequestVersion()
                    val capabilities = repository.capabilitiesFor(switched)
                    _uiState.update { current ->
                        prepareConnectingProfileState(
                            current = current,
                            settings = switched,
                            profileId = profileId,
                            capabilities = capabilities,
                        )
                    }
                }
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: VmErr.UPDATE_SERVER)
                }
            }
            if (result.isSuccess) {
                hydrateDashboardServerSnapshots()
                synchronizeServerScheduler()
                refreshServerSnapshotNow(profileId = profileId, showSelectedError = wasActive)
            }
        }
    }

    fun deleteServerProfile(profileId: String) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val result = runCatching {
                connectionStore.deleteServerProfile(profileId)
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: VmErr.DELETE_SERVER)
                }
            }
            result.getOrNull()?.let { resultValue ->
                repository.removeProfile(profileId)
                nextServerRefreshAt.remove(profileId)
                hydrateDashboardServerSnapshots()

                val nextProfileId = resultValue.activeProfileId
                if (nextProfileId.isNullOrBlank()) {
                    serverSchedulerJob?.cancel()
                    serverSchedulerJob = null
                    repository.clearAllSessions()
                    bumpActiveProfileRequestVersion()
                    _uiState.update { current ->
                        current.copy(
                            activeServerProfileId = null,
                            selectedDashboardProfileId = null,
                            dashboardSessionToken = current.dashboardSessionToken + 1L,
                            connected = false,
                            isConnecting = false,
                            serverVersion = "-",
                            transferInfo = TransferInfo(),
                            torrents = emptyList(),
                            dailyTagUploadDate = "",
                            dailyTagUploadStats = emptyList(),
                            dailyCountryUploadDate = "",
                            dailyCountryUploadStats = emptyList(),
                            dashboardServerSnapshots = emptyList(),
                            dashboardAggregate = DashboardAggregateState(),
                            categoryOptions = emptyList(),
                            tagOptions = emptyList(),
                            pendingBackendRepair = null,
                            detailHash = "",
                            detailLoading = false,
                            detailProperties = null,
                            detailFiles = emptyList(),
                            detailTrackers = emptyList(),
                            pendingActionKeys = emptySet(),
                        )
                    }
                } else {
                    repository.selectProfile(nextProfileId)
                    val nextSettings = resultValue.settings
                        ?: connectionStore.loadSettingsForProfile(nextProfileId)
                        ?: _uiState.value.settings
                    bumpActiveProfileRequestVersion()
                    val capabilities = repository.capabilitiesFor(nextSettings)
                    _uiState.update { current ->
                        prepareConnectingProfileState(
                            current = current,
                            settings = nextSettings,
                            profileId = nextProfileId,
                            capabilities = capabilities,
                        )
                    }
                    hydrateDashboardCacheForCurrentScope(force = true)
                    synchronizeServerScheduler()
                    nextServerRefreshAt[nextProfileId] = 0L
                    refreshServerSnapshotNow(profileId = nextProfileId, showSelectedError = false)
                }
            }
        }
    }

    fun switchServerProfile(profileId: String) {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return
        prepareServerDashboardTransition(normalizedProfileId)
        viewModelScope.launch {
            val result = runCatching {
                val switched = connectionStore.switchToServerProfile(normalizedProfileId)
                repository.selectProfile(normalizedProfileId)
                bumpActiveProfileRequestVersion()
                _uiState.update { current ->
                    current.copy(
                        settings = switched,
                        activeServerProfileId = normalizedProfileId,
                        selectedDashboardProfileId = normalizedProfileId,
                        activeCapabilities = repository.capabilitiesFor(switched),
                        isConnecting = true,
                        pendingBackendRepair = null,
                    )
                }
                updateCachedProfileSettings(normalizedProfileId, switched)
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: VmErr.SWITCH_SERVER)
                }
            }
            if (result.isSuccess) {
                hydrateDashboardCacheForCurrentScope(force = true)
                synchronizeServerScheduler()
                nextServerRefreshAt[normalizedProfileId] = 0L
                refreshServerSnapshotNow(profileId = normalizedProfileId, showSelectedError = true)
            }
        }
    }

    fun selectDashboardProfile(profileId: String) {
        if (profileId.isBlank()) return
        switchServerProfile(profileId)
    }

    fun reorderServerProfiles(profileIds: List<String>) {
        val normalizedIds = profileIds.map { it.trim() }.filter { it.isNotBlank() }
        if (normalizedIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                connectionStore.reorderServerProfiles(normalizedIds)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: VmErr.REORDER_SERVERS)
                }
            }
        }
    }

    fun updateServerDashboardCardVisibility(
        profileId: String,
        card: DashboardChartCard,
        visible: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val fallbackSettings = resolveProfileSettings(profileId) ?: _uiState.value.settings
            runCatching {
                connectionStore.updateServerDashboardPreferences(profileId, fallbackSettings) { current ->
                    val visibleCards = current.visibleCards.toMutableList()
                    if (visible) {
                        if (!visibleCards.contains(card.storageKey)) visibleCards += card.storageKey
                    } else {
                        visibleCards.remove(card.storageKey)
                    }
                    current.copy(visibleCards = visibleCards)
                }
            }.onSuccess { preferences ->
                _uiState.update { current ->
                    current.copy(
                        serverDashboardPreferences = current.serverDashboardPreferences + (profileId to preferences),
                    )
                }
                onComplete(true)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: VmErr.UPDATE_CHART_DISPLAY)
                }
                onComplete(false)
            }
        }
    }

    fun updateServerDashboardCardsVisibility(
        profileId: String,
        cards: List<DashboardChartCard>,
        visible: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (profileId.isBlank()) return
        val normalizedCards = cards.distinct()
        if (normalizedCards.isEmpty()) {
            onComplete(true)
            return
        }
        viewModelScope.launch {
            val fallbackSettings = resolveProfileSettings(profileId) ?: _uiState.value.settings
            runCatching {
                connectionStore.updateServerDashboardPreferences(profileId, fallbackSettings) { current ->
                    val visibleCards = current.visibleCards.toMutableList()
                    normalizedCards.forEach { card ->
                        if (visible) {
                            if (card.storageKey !in visibleCards) {
                                visibleCards += card.storageKey
                            }
                        } else {
                            visibleCards.remove(card.storageKey)
                        }
                    }
                    current.copy(
                        visibleCards = visibleCards.toSet().toList(),
                    )
                }
            }.onSuccess { updatedPreferences ->
                _uiState.update { current ->
                    current.copy(
                        serverDashboardPreferences = current.serverDashboardPreferences
                            .toMutableMap()
                            .apply { this[profileId] = updatedPreferences },
                    )
                }
                onComplete(true)
            }.onFailure {
                onComplete(false)
            }
        }
    }

    fun updateServerDashboardCardOrder(
        profileId: String,
        order: List<DashboardChartCard>,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val fallbackSettings = resolveProfileSettings(profileId) ?: _uiState.value.settings
            runCatching {
                connectionStore.updateServerDashboardPreferences(profileId, fallbackSettings) { current ->
                    current.copy(cardOrder = order.joinToString(",") { it.storageKey })
                }
            }.onSuccess { preferences ->
                _uiState.update { current ->
                    current.copy(
                        serverDashboardPreferences = current.serverDashboardPreferences + (profileId to preferences),
                    )
                }
                onComplete(true)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: VmErr.UPDATE_CHART_ORDER)
                }
                onComplete(false)
            }
        }
    }

    fun resetServerDashboardPreferences(
        profileId: String,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val fallbackSettings = resolveProfileSettings(profileId) ?: _uiState.value.settings
            val defaults = defaultServerDashboardPreferences(fallbackSettings)
            runCatching {
                connectionStore.saveServerDashboardPreferences(profileId, defaults)
                defaults
            }.onSuccess { preferences ->
                _uiState.update { current ->
                    current.copy(
                        serverDashboardPreferences = current.serverDashboardPreferences + (profileId to preferences),
                    )
                }
                onComplete(true)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: VmErr.RESET_CHART_SETTINGS)
                }
                onComplete(false)
            }
        }
    }

    fun markServerStackReorderHintSeen() = updateAndPersistSettings { current ->
        current.copy(hasSeenServerStackReorderHint = true)
    }

    fun markServerDashboardSwipeHintSeen() = updateAndPersistSettings { current ->
        current.copy(hasSeenServerDashboardSwipeHint = true)
    }

    fun markServerDashboardCardHintSeen() = updateAndPersistSettings { current ->
        current.copy(hasSeenServerDashboardCardHint = true)
    }

    fun exportTorrentFile(
        hash: String,
        onSuccess: (ByteArray) -> Unit,
    ) {
        val profileId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        val normalizedHash = hash.trim()
        if (profileId.isBlank() || normalizedHash.isBlank()) return
        val requestVersion = currentActiveProfileRequestVersion()
        viewModelScope.launch {
            repository.exportTorrentFile(profileId, normalizedHash)
                .onSuccess { bytes -> onSuccess(bytes) }
                .onFailure { error ->
                    if (isActiveProfileRequestValid(profileId, requestVersion)) {
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: VmErr.EXPORT_TORRENT)
                        }
                    }
                }
        }
    }

    private fun autoConnectIfNeeded(settings: ConnectionSettings) {
        if (autoConnectAttempted) return
        if (settings.host.isBlank() || settings.username.isBlank()) return
        val state = _uiState.value
        if (state.serverProfiles.isNotEmpty() && state.activeServerProfileId.isNullOrBlank()) return
        autoConnectAttempted = true
        connectInternal(persistSettings = false, showErrorOnFailure = false)
    }

    private fun defaultServerDashboardPreferences(settings: ConnectionSettings): ServerDashboardPreferences {
        val isTransmission = settings.serverBackendType == ServerBackendType.TRANSMISSION
        val defaultKeys = if (isTransmission) {
            listOf(
                DashboardChartCard.CATEGORY_SHARE.storageKey,
                DashboardChartCard.TAG_UPLOAD.storageKey,
                DashboardChartCard.TORRENT_STATE.storageKey,
                DashboardChartCard.TRACKER_SITE.storageKey,
            )
        } else {
            listOf(
                DashboardChartCard.COUNTRY_FLOW.storageKey,
                DashboardChartCard.CATEGORY_SHARE.storageKey,
                DashboardChartCard.DAILY_UPLOAD.storageKey,
            )
        }
        return ServerDashboardPreferences(
            visibleCards = defaultKeys,
            cardOrder = defaultKeys.joinToString(","),
        )
    }

    private fun bumpActiveProfileRequestVersion() {
        activeProfileRequestVersion += 1
    }

    internal fun currentActiveProfileRequestVersion(): Long = activeProfileRequestVersion

    internal fun isActiveProfileRequestValid(
        profileId: String,
        requestVersion: Long,
    ): Boolean {
        return shouldApplyActiveProfileAsyncResult(
            requestedProfileId = profileId,
            requestVersion = requestVersion,
            activeProfileId = _uiState.value.activeServerProfileId,
            activeRequestVersion = activeProfileRequestVersion,
        )
    }

    private fun isDetailRequestValid(
        profileId: String,
        hash: String,
        requestVersion: Long,
    ): Boolean {
        val normalizedHash = hash.trim()
        return normalizedHash.isNotBlank() &&
            isActiveProfileRequestValid(profileId, requestVersion) &&
            _uiState.value.detailHash == normalizedHash
    }

    private fun connectInternal(
        persistSettings: Boolean,
        showErrorOnFailure: Boolean,
    ) {
        if (_uiState.value.isConnecting) return
        viewModelScope.launch {
            resetDailyCountryUploadTrackingState()
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            val settings = _uiState.value.settings
            if (persistSettings) {
                connectionStore.save(settings)
            }
            _uiState.value.activeServerProfileId?.let { activeProfileId ->
                updateCachedProfileSettings(activeProfileId, settings)
            }
            hydrateDashboardCacheForCurrentScope()

            repository.connect(settings)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            connected = true,
                            activeCapabilities = repository.activeCapabilities(),
                        )
                    }
                    refreshServerVersion()
                    refresh()
                    loadGlobalSelectionOptions()
                    startAutoRefresh()
                    startHomeChartRefresh()
                    startHourlyBoundaryRefresh()
                    if (repository.activeCapabilities().supportsCountryDistribution) {
                        startCountryPeerTracker()
                    } else {
                        countryPeerTrackerJob?.cancel()
                        _uiState.update {
                            if (it.dailyCountryUploadStats.isEmpty()) it
                            else it.copy(
                                dailyCountryUploadDate = "",
                                dailyCountryUploadStats = emptyList(),
                            )
                        }
                    }
                    refreshDashboardServerSnapshotsAsync()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            connected = false,
                            errorMessage = if (showErrorOnFailure) {
                                error.message ?: "Connection failed."
                            } else {
                                null
                            }
                        )
                    }
                    backgroundJobManager.stopAll()
                    countryPeerTrackerJob?.cancel()
                }
        }
    }

    private fun stopBackgroundJobs() {
        backgroundJobManager.stopAll()
        countryPeerTrackerJob?.cancel()
        dashboardAggregationJob?.cancel()
        serverSchedulerJob?.cancel()
        countryPeerTrackerJob = null
        dashboardAggregationJob = null
        serverSchedulerJob = null
    }

    private fun buildProfileSettingsDraft(
        baseSettings: ConnectionSettings = _uiState.value.settings,
        backendType: ServerBackendType,
        host: String,
        port: String,
        useHttps: Boolean,
        username: String,
        password: String,
        refreshSeconds: String,
    ): ConnectionSettings {
        val normalizedHost = host.trim()
        val parsed = parseHostInputHints(normalizedHost)
        val defaultPort = when (backendType) {
            ServerBackendType.QBITTORRENT -> 8080
            ServerBackendType.TRANSMISSION -> 9091
        }
        val resolvedPort = parsed?.port ?: (port.toIntOrNull() ?: defaultPort)
        val resolvedUseHttps = parsed?.useHttps ?: useHttps
        val nextSettings = baseSettings.copy(
            host = normalizedHost,
            port = resolvedPort.coerceIn(1, 65535),
            useHttps = resolvedUseHttps,
            username = username.trim(),
            password = password,
            serverBackendType = backendType,
            refreshSeconds = (refreshSeconds.toIntOrNull() ?: 5).coerceIn(5, 120),
        )
        require(nextSettings.host.isNotBlank()) { "主机不能为空" }
        require(nextSettings.username.isNotBlank()) { "用户名不能为空" }
        return nextSettings
    }

    private fun resetUiForServerSwitch(
        settings: ConnectionSettings,
        activeProfileId: String?,
    ) {
        _uiState.update {
            it.copy(
                settings = settings,
                activeServerProfileId = activeProfileId,
                activeCapabilities = repository.capabilitiesFor(settings),
                connected = false,
                serverVersion = "-",
                transferInfo = TransferInfo(),
                torrents = emptyList(),
                dailyTagUploadDate = "",
                dailyTagUploadStats = emptyList(),
                dailyCountryUploadDate = "",
                dailyCountryUploadStats = emptyList(),
                selectedDashboardProfileId = activeProfileId ?: it.selectedDashboardProfileId,
                dashboardCacheHydrated = false,
                hasDashboardSnapshot = false,
                detailHash = "",
                detailLoading = false,
                detailProperties = null,
                detailFiles = emptyList(),
                detailTrackers = emptyList(),
                pendingActionKeys = emptySet(),
            )
        }
    }

    fun refresh(manual: Boolean = false) {
        if (isRefreshInProgress) return
        isRefreshInProgress = true
        viewModelScope.launch {
            try {
                if (manual) {
                    _uiState.update {
                        it.copy(
                            isManualRefreshing = true,
                            errorMessage = null,
                        )
                    }
                }

                val state = _uiState.value
                val refreshAllServers = state.refreshScene == RefreshScene.DASHBOARD &&
                    state.serverProfiles.size > 1

                if (refreshAllServers) {
                    state.serverProfiles.forEach { profile ->
                        if (
                            shouldSkipRefreshForDashboardReorderHold(
                                heldProfileId = state.dashboardRefreshHoldProfileId,
                                profileId = profile.id,
                            )
                        ) {
                            return@forEach
                        }
                        refreshServerSnapshotNow(
                            profileId = profile.id,
                            showSelectedError = manual && profile.id == state.activeServerProfileId,
                        )
                    }
                } else {
                    val activeProfileId = state.activeServerProfileId
                    if (!activeProfileId.isNullOrBlank()) {
                        if (
                            shouldSkipRefreshForDashboardReorderHold(
                                heldProfileId = state.dashboardRefreshHoldProfileId,
                                profileId = activeProfileId,
                            )
                        ) {
                            return@launch
                        }
                        refreshServerSnapshotNow(
                            profileId = activeProfileId,
                            showSelectedError = manual,
                        )
                    }
                }
            } finally {
                isRefreshInProgress = false
                if (manual) {
                    _uiState.update {
                        if (it.isManualRefreshing) {
                            it.copy(isManualRefreshing = false)
                        } else {
                            it
                        }
                    }
                }
                detectCompletedTorrents()
                updateWidgetData()
            }
        }
    }

    private fun detectCompletedTorrents() {
        val state = _uiState.value
        val torrents = state.torrents
        torrents.forEach { torrent ->
            val hash = torrent.hash.ifBlank { return@forEach }
            val prevState = previousTorrentStates[hash]
            val currentState = torrent.state.trim().lowercase()
            previousTorrentStates[hash] = currentState
            if (prevState == null) return@forEach
            val wasDownloading = prevState in setOf("downloading", "forceddl", "stalldl", "queueddl")
            val isNowCompleted = currentState in setOf("uploading", "forcedup", "stalledup", "queuedup", "pausedup")
                    || (torrent.progress >= 1f && currentState !in setOf("checking", "checkingup", "checkingdl", "moving", "error", "missingfiles"))
            if (wasDownloading && isNowCompleted) {
                val context = connectionStore.context.applicationContext
                MainActivity.notifyTorrentCompleted(
                    context, torrent.name.ifBlank { hash.take(12) }, vibrate = true
                )
            }
        }
    }

    private fun updateWidgetData() {
        val state = _uiState.value
        val chartInfo = state.dashboardAggregate.chartTransferInfo
        val allSnapshots = state.dashboardServerSnapshots
        val totalTorrents = if (allSnapshots.isNotEmpty()) {
            allSnapshots.sumOf { it.torrents.size }
        } else {
            state.torrents.size
        }
        TorrentWidgetProvider.updateData(
            downloadSpeed = chartInfo?.downloadSpeed ?: state.transferInfo.downloadSpeed,
            uploadSpeed = chartInfo?.uploadSpeed ?: state.transferInfo.uploadSpeed,
            torrentCount = totalTorrents,
        )
        TorrentWidgetProvider.refreshWidgets(connectionStore.context.applicationContext)
    }

    fun pauseTorrent(hash: String) = runTorrentAction(hash) { profileId ->
        repository.pauseTorrent(profileId, hash).getOrThrow()
    }

    fun resumeTorrent(hash: String) = runTorrentAction(hash) { profileId ->
        repository.resumeTorrent(profileId, hash).getOrThrow()
    }

    fun deleteTorrent(hash: String, deleteFiles: Boolean) = runTorrentAction(hash) { profileId ->
        repository.deleteTorrent(profileId, hash, deleteFiles).getOrThrow()
    }

    fun reannounceTorrent(hash: String) = runDetailAction(hash) { profileId ->
        repository.reannounceTorrent(profileId, hash).getOrThrow()
    }

    fun recheckTorrent(hash: String) = runDetailAction(hash) { profileId ->
        repository.recheckTorrent(profileId, hash).getOrThrow()
    }

    fun loadTorrentDetail(hash: String) {
        val profileId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        val normalizedHash = hash.trim()
        if (profileId.isBlank() || normalizedHash.isBlank()) return
        val requestVersion = currentActiveProfileRequestVersion()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    detailHash = normalizedHash,
                    detailLoading = true,
                    errorMessage = null,
                )
            }
            repository.fetchTorrentDetail(profileId, normalizedHash)
                .onSuccess { detail ->
                    val trackers = repository.fetchTorrentTrackers(profileId, normalizedHash)
                        .getOrElse { emptyList() }
                    val categoryOptions = repository.fetchCategoryOptions(profileId)
                        .getOrElse { emptyList() }
                    val tagOptions = repository.fetchTagOptions(profileId)
                        .getOrElse { emptyList() }
                    _uiState.update { current ->
                        if (!isDetailRequestValid(profileId, normalizedHash, requestVersion)) {
                            current
                        } else {
                            current.copy(
                                detailLoading = false,
                                detailProperties = detail.properties,
                                detailFiles = detail.files,
                                detailTrackers = trackers,
                                categoryOptions = categoryOptions,
                                tagOptions = tagOptions,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        if (!isDetailRequestValid(profileId, normalizedHash, requestVersion)) {
                            current
                        } else {
                            current.copy(
                                detailLoading = false,
                                detailProperties = null,
                                detailFiles = emptyList(),
                                detailTrackers = emptyList(),
                                errorMessage = error.message ?: VmErr.LOAD_TORRENT_DETAIL,
                            )
                        }
                    }
                }
        }
    }

    fun renameTorrent(hash: String, newName: String) = runDetailAction(hash) { profileId ->
        repository.renameTorrent(profileId, hash, newName).getOrThrow()
    }

    fun setTorrentLocation(hash: String, location: String) = runDetailAction(hash) { profileId ->
        repository.setTorrentLocation(profileId, hash, location).getOrThrow()
    }

    fun setTorrentCategory(hash: String, category: String) = runDetailAction(hash) { profileId ->
        repository.setTorrentCategory(profileId, hash, category).getOrThrow()
    }

    fun setTorrentTags(hash: String, oldTags: String, newTags: String) = runDetailAction(hash) { profileId ->
        repository.setTorrentTags(profileId, hash, oldTags, newTags).getOrThrow()
    }

    fun setTorrentSpeedLimit(hash: String, downloadLimitKb: String, uploadLimitKb: String) = runDetailAction(hash) { profileId ->
        val dl = parseLimitKbToBytes(downloadLimitKb)
        val up = parseLimitKbToBytes(uploadLimitKb)
        repository.setTorrentSpeedLimit(profileId, hash, dl, up).getOrThrow()
    }

    // ---- 全局限速设置 ----

    fun openGlobalSpeedLimitDialog() {
        val targetId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        if (targetId.isBlank()) return
        _uiState.update {
            it.copy(
                showGlobalSpeedLimitDialog = true,
                globalSpeedLimitLoading = true,
                speedLimitTargetProfileId = targetId,
            )
        }
        viewModelScope.launch {
            repository.fetchGlobalSpeedLimits(targetId)
                .onSuccess { limits ->
                    _uiState.update { it.copy(globalSpeedLimits = limits, globalSpeedLimitLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            showGlobalSpeedLimitDialog = false,
                            globalSpeedLimitLoading = false,
                            speedLimitTargetProfileId = null,
                            errorMessage = "${VmErr.FETCH_SPEED_LIMITS}: ${error.message}",
                        )
                    }
                }
        }
    }

    fun switchSpeedLimitServer(profileId: String) {
        _uiState.update {
            it.copy(globalSpeedLimitLoading = true, speedLimitTargetProfileId = profileId)
        }
        viewModelScope.launch {
            repository.fetchGlobalSpeedLimits(profileId)
                .onSuccess { limits ->
                    _uiState.update { it.copy(globalSpeedLimits = limits, globalSpeedLimitLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            globalSpeedLimitLoading = false,
                            errorMessage = "${VmErr.FETCH_SPEED_LIMITS}: ${error.message}",
                        )
                    }
                }
        }
    }

    fun dismissGlobalSpeedLimitDialog() {
        _uiState.update {
            it.copy(showGlobalSpeedLimitDialog = false, globalSpeedLimits = null, speedLimitTargetProfileId = null)
        }
    }

    fun saveGlobalSpeedLimits(
        dlKb: String, ulKb: String, altDlKb: String, altUlKb: String,
        schedulerEnabled: Boolean, scheduleFromHour: Int, scheduleFromMin: Int,
        scheduleToHour: Int, scheduleToMin: Int, schedulerDays: Int,
    ) {
        val profileId = _uiState.value.speedLimitTargetProfileId?.trim().orEmpty()
        if (profileId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val dl = parseLimitKbToBytes(dlKb).coerceAtLeast(0L)
                val ul = parseLimitKbToBytes(ulKb).coerceAtLeast(0L)
                val altDl = parseLimitKbToBytes(altDlKb).coerceAtLeast(0L)
                val altUl = parseLimitKbToBytes(altUlKb).coerceAtLeast(0L)
                repository.setGlobalSpeedLimits(
                    profileId, dl, ul, altDl, altUl,
                    schedulerEnabled, scheduleFromHour, scheduleFromMin,
                    scheduleToHour, scheduleToMin, schedulerDays,
                ).getOrThrow()
            }.onSuccess {
                _uiState.update {
                    it.copy(showGlobalSpeedLimitDialog = false, globalSpeedLimits = null, speedLimitTargetProfileId = null)
                }
                refresh()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "${VmErr.SAVE_SPEED_LIMITS}: ${error.message}") }
            }
        }
    }

    fun toggleAltSpeedMode() {
        val profileId = (_uiState.value.speedLimitTargetProfileId
            ?: _uiState.value.activeServerProfileId)?.trim().orEmpty()
        if (profileId.isBlank()) return
        viewModelScope.launch {
            repository.toggleAltSpeedMode(profileId)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "${VmErr.TOGGLE_ALT_SPEED}: ${error.message}") }
                }
        }
    }

    fun setTorrentShareRatio(hash: String, ratio: String) = runDetailAction(hash) { profileId ->
        val value = ratio.trim().toDoubleOrNull() ?: throw IllegalArgumentException("分享比率格式无效")
        repository.setTorrentShareRatio(profileId, hash, value).getOrThrow()
    }

    fun addTracker(hash: String, trackerUrl: String) = runDetailAction(hash) { profileId ->
        repository.addTracker(profileId, hash, trackerUrl.trim()).getOrThrow()
    }

    fun editTracker(
        hash: String,
        tracker: TorrentTracker,
        newUrl: String,
    ) = runDetailAction(hash) { profileId ->
        repository.editTracker(
            profileId = profileId,
            hash = hash,
            tracker = tracker,
            newUrl = newUrl.trim(),
        ).getOrThrow()
    }

    fun removeTracker(
        hash: String,
        tracker: TorrentTracker,
    ) = runDetailAction(hash) { profileId ->
        repository.removeTracker(
            profileId = profileId,
            hash = hash,
            tracker = tracker,
        ).getOrThrow()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissPendingBackendRepair() {
        _uiState.update { current ->
            current.copy(pendingBackendRepair = null)
        }
    }

    fun confirmPendingBackendRepair() {
        val pending = _uiState.value.pendingBackendRepair ?: return
        viewModelScope.launch {
            runCatching {
                val profile = _uiState.value.serverProfiles.firstOrNull { it.id == pending.profileId }
                    ?: error("服务器配置不存在")
                val existingSettings = connectionStore.loadSettingsForProfile(pending.profileId)
                    ?: error("服务器配置不存在")
                val updatedSettings = existingSettings.copy(serverBackendType = pending.detectedBackend)
                connectionStore.updateServerProfile(
                    profileId = pending.profileId,
                    name = profile.name,
                    settings = updatedSettings,
                    passwordOverride = null,
                )
                repository.removeProfile(pending.profileId)
                nextServerRefreshAt[pending.profileId] = 0L
                val isActive = _uiState.value.activeServerProfileId == pending.profileId
                if (isActive) {
                    val switched = connectionStore.switchToServerProfile(pending.profileId)
                    repository.selectProfile(pending.profileId)
                    bumpActiveProfileRequestVersion()
                    val capabilities = repository.capabilitiesFor(switched)
                    _uiState.update { current ->
                        prepareConnectingProfileState(
                            current = current,
                            settings = switched,
                            profileId = pending.profileId,
                            capabilities = capabilities,
                        )
                    }
                    hydrateDashboardCacheForCurrentScope(force = true)
                } else {
                    _uiState.update { current ->
                        current.copy(
                            pendingBackendRepair = null,
                            errorMessage = null,
                        )
                    }
                }
                hydrateDashboardServerSnapshots()
                synchronizeServerScheduler()
                refreshServerSnapshotNow(
                    profileId = pending.profileId,
                    showSelectedError = true,
                )
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        pendingBackendRepair = null,
                        errorMessage = userFacingConnectionMessage(error),
                    )
                }
            }
        }
    }

    fun loadGlobalSelectionOptions() {
        val profileId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        if (!_uiState.value.connected || profileId.isBlank()) return
        val requestVersion = currentActiveProfileRequestVersion()
        viewModelScope.launch {
            val categoryOptions = repository.fetchCategoryOptions(profileId).getOrElse { emptyList() }
            val tagOptions = repository.fetchTagOptions(profileId).getOrElse { emptyList() }
            _uiState.update { current ->
                if (!isActiveProfileRequestValid(profileId, requestVersion)) {
                    current
                } else {
                    current.copy(
                        categoryOptions = categoryOptions,
                        tagOptions = tagOptions,
                    )
                }
            }
        }
    }

    fun handleSharedMagnet(url: String) {
        _uiState.update { it.copy(sharedMagnetUrl = url.trim()) }
    }

    fun clearSharedMagnetUrl() {
        _uiState.update { it.copy(sharedMagnetUrl = "") }
    }

    fun addTorrent(
        urls: String,
        files: List<AddTorrentFile>,
        autoTmm: Boolean,
        category: String,
        tags: String,
        savePath: String,
        paused: Boolean,
        skipChecking: Boolean,
        sequentialDownload: Boolean,
        firstLastPiecePrio: Boolean,
        uploadLimitKb: String,
        downloadLimitKb: String,
    ) {
        if (!_uiState.value.connected) {
            _uiState.update { it.copy(errorMessage = VmErr.CONNECT_FIRST) }
            return
        }
        val profileId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        if (profileId.isBlank()) {
            _uiState.update { it.copy(errorMessage = VmErr.SELECT_SERVER_FIRST) }
            return
        }
        val requestVersion = currentActiveProfileRequestVersion()
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            runCatching {
                val request = AddTorrentRequest(
                    urls = urls.trim(),
                    files = files,
                    autoTmm = autoTmm,
                    category = category.trim(),
                    tags = tags.trim(),
                    savePath = savePath.trim(),
                    paused = paused,
                    skipChecking = skipChecking,
                    sequentialDownload = sequentialDownload,
                    firstLastPiecePrio = firstLastPiecePrio,
                    uploadLimitBytes = parseLimitKbToBytes(uploadLimitKb),
                    downloadLimitBytes = parseLimitKbToBytes(downloadLimitKb),
                )
                repository.addTorrent(profileId, request).getOrThrow()
            }.onSuccess {
                if (isActiveProfileRequestValid(profileId, requestVersion)) {
                    loadGlobalSelectionOptions()
                    refresh()
                } else {
                    nextServerRefreshAt[profileId] = 0L
                }
            }.onFailure { error ->
                if (isActiveProfileRequestValid(profileId, requestVersion)) {
                    _uiState.update { it.copy(errorMessage = error.message ?: "添加种子失败。") }
                }
            }
        }
    }

    private fun runTorrentAction(
        hash: String,
        action: suspend (String) -> Unit,
    ) {
        val profileId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        val normalizedHash = hash.trim()
        if (profileId.isBlank() || normalizedHash.isBlank()) return
        val pendingActionKey = buildPendingActionKey(profileId, normalizedHash)
        if (_uiState.value.pendingActionKeys.contains(pendingActionKey)) return
        val requestVersion = currentActiveProfileRequestVersion()

        viewModelScope.launch {
            _uiState.update {
                it.copy(pendingActionKeys = it.pendingActionKeys + pendingActionKey, errorMessage = null)
            }
            runCatching { action(profileId) }
                .onSuccess {
                    if (isActiveProfileRequestValid(profileId, requestVersion)) {
                        refresh()
                    } else {
                        nextServerRefreshAt[profileId] = 0L
                    }
                }
                .onFailure { error ->
                    if (isActiveProfileRequestValid(profileId, requestVersion)) {
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: "Action failed.")
                        }
                    }
                }
            _uiState.update {
                it.copy(pendingActionKeys = it.pendingActionKeys - pendingActionKey)
            }
        }
    }

    private fun runDetailAction(
        hash: String,
        action: suspend (String) -> Unit,
    ) {
        val normalizedHash = hash.trim()
        if (normalizedHash.isBlank()) return
        val requestVersion = currentActiveProfileRequestVersion()
        runTorrentAction(normalizedHash) { profileId ->
            action(profileId)
            val detail = repository.fetchTorrentDetail(profileId, normalizedHash).getOrThrow()
            val trackers = repository.fetchTorrentTrackers(profileId, normalizedHash).getOrElse { emptyList() }
            val categoryOptions = repository.fetchCategoryOptions(profileId).getOrElse { emptyList() }
            val tagOptions = repository.fetchTagOptions(profileId).getOrElse { emptyList() }
            _uiState.update { current ->
                if (!isDetailRequestValid(profileId, normalizedHash, requestVersion)) {
                    current
                } else {
                    current.copy(
                        detailHash = normalizedHash,
                        detailLoading = false,
                        detailProperties = detail.properties,
                        detailFiles = detail.files,
                        detailTrackers = trackers,
                        categoryOptions = categoryOptions,
                        tagOptions = tagOptions,
                    )
                }
            }
        }
    }

    internal suspend fun refreshDetailSnapshot(
        profileId: String,
        hash: String,
        requestVersion: Long,
    ) {
        val detail = repository.fetchTorrentDetail(profileId, hash).getOrNull() ?: return
        val trackers = repository.fetchTorrentTrackers(profileId, hash).getOrElse { emptyList() }
        _uiState.update { current ->
            if (!isDetailRequestValid(profileId, hash, requestVersion)) {
                current
            } else {
                current.copy(
                    detailProperties = detail.properties,
                    detailFiles = detail.files,
                    detailTrackers = trackers,
                )
            }
        }
    }

    private fun refreshServerVersion() {
        val profileId = _uiState.value.activeServerProfileId?.trim().orEmpty()
        if (profileId.isBlank()) return
        val requestVersion = currentActiveProfileRequestVersion()
        viewModelScope.launch {
            repository.fetchServerVersion(profileId)
                .onSuccess { version ->
                    if (!isActiveProfileRequestValid(profileId, requestVersion)) return@onSuccess
                    var updatedState: MainUiState? = null
                    _uiState.update { current ->
                        if (!isActiveProfileRequestValid(profileId, requestVersion)) {
                            current
                        } else {
                            current.copy(serverVersion = version.ifBlank { "-" })
                                .also { updatedState = it }
                        }
                    }
                    updatedState?.let { stateSnapshot ->
                        saveDashboardServerSnapshotForProfile(
                            profileId = profileId,
                            stateSnapshot = stateSnapshot,
                        )
                    }
                }
        }
    }

    internal fun saveDashboardCache(
        stateSnapshot: MainUiState = _uiState.value,
        scopeKey: String = buildDailyUploadTrackingScopeKey(
            activeProfileId = stateSnapshot.activeServerProfileId,
            settings = stateSnapshot.settings,
        ),
    ) {
        viewModelScope.launch {
            connectionStore.saveDashboardCacheSnapshot(
                scopeKey = scopeKey,
                snapshot = DashboardCacheSnapshot(
                    transferInfo = stateSnapshot.transferInfo,
                    torrents = stateSnapshot.torrents,
                    dailyTagUploadDate = stateSnapshot.dailyTagUploadDate,
                    dailyTagUploadStats = stateSnapshot.dailyTagUploadStats.map { stat ->
                        CachedDailyTagUploadStat(
                            tag = stat.tag,
                            uploadedBytes = stat.uploadedBytes,
                            torrentCount = stat.torrentCount,
                            isNoTag = stat.isNoTag,
                        )
                    },
                    dailyCountryUploadDate = stateSnapshot.dailyCountryUploadDate,
                    dailyCountryUploadStats = stateSnapshot.dailyCountryUploadStats,
                ),
            )
        }
    }


    // =================================================================================
    // Dashboard refresh functions extracted to DashboardRefreshController.kt
    // =================================================================================

    private fun startCountryPeerTracker() {
        countryPeerTrackerJob?.cancel()
        countryPeerTrackerJob = viewModelScope.launch {
            while (isActive) {
                delay(COUNTRY_TRACKER_SAMPLE_INTERVAL_MS)
                val state = _uiState.value
                if (!state.connected) continue
                if (!state.activeCapabilities.supportsCountryDistribution) continue
                val profileId = state.activeServerProfileId?.trim().orEmpty()
                if (profileId.isBlank()) continue
                val requestVersion = currentActiveProfileRequestVersion()
                val scopeKey = buildDailyUploadTrackingScopeKey(
                    activeProfileId = profileId,
                    settings = state.settings,
                )

                val countryStats = dailyCountryUploadTracker.mutex.withLock {
                    dailyCountryUploadTracker.sample(
                        profileId = profileId,
                        key = scopeKey,
                        torrents = state.torrents,
                    )
                }
                var updatedState: MainUiState? = null
                _uiState.update { current ->
                    if (!isActiveProfileRequestValid(profileId, requestVersion)) {
                        current
                    } else {
                        current.copy(
                            dailyCountryUploadDate = countryStats.dateLabel,
                            dailyCountryUploadStats = countryStats.countries,
                        ).also { next ->
                            updatedState = next
                        }
                    }
                }
                updatedState?.let { stateSnapshot ->
                    saveDashboardCache(
                        stateSnapshot = stateSnapshot,
                        scopeKey = scopeKey,
                    )
                    saveDashboardServerSnapshotForProfile(
                        profileId = profileId,
                        stateSnapshot = stateSnapshot,
                    )
                }
            }
        }
    }

    private fun resetDailyCountryUploadTrackingState() {
        dailyCountryUploadTracker.reset()
        _uiState.update {
            it.copy(
                dailyCountryUploadDate = "",
                dailyCountryUploadStats = emptyList(),
            )
        }
    }
    private fun pruneCachedProfileSettingsInMemory(profiles: List<ServerProfile>) {
        val pruned = pruneCachedProfileSettings(
            cache = cachedProfileSettings,
            profiles = profiles,
        )
        cachedProfileSettings.clear()
        cachedProfileSettings.putAll(pruned)
    }

    internal fun updateCachedProfileSettings(
        profileId: String,
        settings: ConnectionSettings,
    ) {
        val updated = cacheProfileSettings(
            cache = cachedProfileSettings,
            profileId = profileId,
            settings = settings,
        )
        if (updated === cachedProfileSettings) return
        cachedProfileSettings.clear()
        cachedProfileSettings.putAll(updated)
    }

    internal suspend fun resolveProfileSettings(profileId: String): ConnectionSettings? {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return null
        val activeProfile = _uiState.value.serverProfiles.firstOrNull { it.id == normalizedProfileId }
        val currentState = _uiState.value
        resolveActiveOrCachedProfileSettings(
            profileId = normalizedProfileId,
            activeProfileId = currentState.activeServerProfileId,
            activeProfile = activeProfile,
            currentSettings = currentState.settings,
            cachedSettings = cachedProfileSettings[normalizedProfileId],
        )?.let { resolved ->
            updateCachedProfileSettings(normalizedProfileId, resolved)
            return resolved
        }

        val loaded = connectionStore.loadSettingsForProfile(normalizedProfileId) ?: return null
        updateCachedProfileSettings(normalizedProfileId, loaded)
        return loaded
    }

    internal fun currentDailyUploadTrackingScopeKey(): String {
        val state = _uiState.value
        val preferredProfileId = state.selectedDashboardProfileId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: state.activeServerProfileId
        return buildDailyUploadTrackingScopeKey(
            activeProfileId = preferredProfileId,
            settings = state.settings,
        )
    }

    internal fun torrentTrackingKey(torrent: TorrentInfo): String {
        return dailyCountryTorrentTrackingKey(torrent)
    }

    private suspend fun seedCachedSettingsForProfile(profileId: String?) {
        val normalizedProfileId = profileId?.trim().orEmpty()
        if (normalizedProfileId.isBlank()) return
        val activeProfile = _uiState.value.serverProfiles.firstOrNull { it.id == normalizedProfileId }
        val currentSettings = _uiState.value.settings
        val settings = if (activeProfile != null && settingsBelongToProfile(activeProfile, currentSettings)) {
            currentSettings
        } else {
            connectionStore.loadSettingsForProfile(normalizedProfileId)
        } ?: return
        updateCachedProfileSettings(normalizedProfileId, settings)
    }

    override fun onCleared() {
        backgroundJobManager.stopAll()
        countryPeerTrackerJob?.cancel()
        dashboardAggregationJob?.cancel()
        serverSchedulerJob?.cancel()
        repository.clearAllSessions()
        super.onCleared()
    }

    companion object {
        private const val COUNTRY_TRACKER_SAMPLE_INTERVAL_MS = 1_500L

        fun factory(
            connectionStore: ConnectionStore,
            repository: TorrentRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(connectionStore, repository) as T
            }
        }
    }
}




