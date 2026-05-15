package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.ServerBackendType
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.hjw.qbremote.data.BackendConnectionError
import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.DailyCountryUploadStats
import com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.update

// =============================================================================
// Dashboard Refresh Controller
// Extracted from MainViewModel.kt (~1016 lines)
// =============================================================================

internal fun MainViewModel.hydrateDashboardServerSnapshots() {
        dashboardAggregationJob?.cancel()
        dashboardAggregationJob = viewModelScope.launch {
            val ordered = orderedDashboardServerSnapshots(
                profiles = _uiState.value.serverProfiles,
                snapshotsById = connectionStore.loadDashboardServerSnapshots(),
            ).map { snapshot ->
                // useAltSpeedLimits 是实时易变状态，不应从持久化缓存恢复，
                // 否则历史值可能导致聚合仪表盘误显示"备用限速已启用"
                snapshot.copy(
                    transferInfo = snapshot.transferInfo.copy(useAltSpeedLimits = false),
                )
            }
            val aggregate = buildDashboardAggregateWithHistory(
                snapshots = ordered,
                sampleFreshData = false,
            )
            _uiState.update { current ->
                applyDashboardSnapshotsToState(
                    current = current,
                    orderedSnapshots = ordered,
                    aggregate = aggregate,
                )
            }
            syncSelectedUiFromStoredSnapshot()
            markInitialDashboardSnapshotsHydrated()
        }
    }

internal fun MainViewModel.synchronizeServerScheduler() {
        val profiles = _uiState.value.serverProfiles
        if (profiles.isEmpty()) {
            serverSchedulerJob?.cancel()
            serverSchedulerJob = null
            nextServerRefreshAt.clear()
            repository.clearAllSessions()
            return
        }

        val activeIds = profiles.map { it.id }.toSet()
        nextServerRefreshAt.keys.retainAll(activeIds)
        profiles.forEach { profile ->
            nextServerRefreshAt.putIfAbsent(profile.id, 0L)
        }
        repository.selectProfile(_uiState.value.activeServerProfileId)

        if (serverSchedulerJob?.isActive == true) return
        serverSchedulerJob = viewModelScope.launch {
            while (isActive) {
                val currentProfiles = _uiState.value.serverProfiles
                if (currentProfiles.isEmpty()) break
                val now = System.currentTimeMillis()
                currentProfiles.forEach { profile ->
                    val dueAt = nextServerRefreshAt[profile.id] ?: 0L
                    if (
                        shouldSkipRefreshForDashboardReorderHold(
                            heldProfileId = _uiState.value.dashboardRefreshHoldProfileId,
                            profileId = profile.id,
                        )
                    ) {
                        return@forEach
                    }
                    if (now >= dueAt) {
                        refreshServerSnapshotNow(
                            profileId = profile.id,
                            showSelectedError = false,
                        )
                    }
                }
                delay(1_000L)
            }
        }
    }

internal suspend fun MainViewModel.refreshServerSnapshotNow(
        profileId: String,
        showSelectedError: Boolean,
        forceSettings: ConnectionSettings? = null,
    ) {
        if (profileId.isBlank()) return
        serverRefreshMutex.withLock {
            val state = _uiState.value
            val profile = state.serverProfiles.firstOrNull { it.id == profileId }
            val settings = forceSettings ?: resolveProfileSettings(profileId) ?: return
            val isSelectedProfile = state.activeServerProfileId == profileId
            val selectedRequestVersion = currentActiveProfileRequestVersion()
            updateCachedProfileSettings(profileId, settings)
            if (isSelectedProfile) {
                _uiState.update { current ->
                    current.copy(
                        isConnecting = true,
                        errorMessage = if (showSelectedError) null else current.errorMessage,
                    )
                }
            }

            val result = runCatching {
                repository.connect(profileId, settings).getOrThrow()
                val serverVersion = repository.fetchServerVersion(profileId).getOrElse { "-" }
                val dashboardData = repository.fetchDashboard(profileId).getOrThrow()
                val (tagDate, tagStats) = buildDashboardTagUploadStatsForScope(
                    scopeKey = "profile:$profileId",
                    torrents = dashboardData.torrents,
                )
                val countryStats = if (repository.capabilitiesFor(settings).supportsCountryDistribution) {
                    buildDashboardCountryUploadStatsForScope(
                        scopeKey = "profile:$profileId",
                        torrents = dashboardData.torrents,
                        fetchPeerSnapshots = { hashes ->
                            repository.fetchCountryPeerSnapshots(profileId, hashes)
                                .getOrElse { emptyList() }
                        },
                    )
                } else {
                    DailyCountryUploadStats(
                        dateLabel = tagDate,
                        countries = emptyList(),
                    )
                }
                CachedDashboardServerSnapshot(
                    profileId = profileId,
                    profileName = profile?.name ?: settings.host,
                    backendType = profile?.backendType ?: settings.serverBackendType,
                    host = profile?.host ?: settings.host,
                    port = profile?.port ?: settings.port,
                    useHttps = profile?.useHttps ?: settings.useHttps,
                    serverVersion = serverVersion.ifBlank { "-" },
                    transferInfo = dashboardData.transferInfo,
                    torrents = dashboardData.torrents,
                    dailyTagUploadDate = tagDate,
                    dailyTagUploadStats = tagStats.map { stat ->
                        CachedDailyTagUploadStat(
                            tag = stat.tag,
                            uploadedBytes = stat.uploadedBytes,
                            torrentCount = stat.torrentCount,
                            isNoTag = stat.isNoTag,
                        )
                    },
                    dailyCountryUploadDate = countryStats.dateLabel,
                    dailyCountryUploadStats = countryStats.countries,
                    lastUpdatedAt = System.currentTimeMillis(),
                    errorMessage = "",
                    isStale = false,
                )
            }

            result.onSuccess { snapshot ->
                persistDashboardSnapshot(snapshot)
                mergeDashboardSnapshot(snapshot, sampleFreshData = true)
                nextServerRefreshAt[profileId] = System.currentTimeMillis() + nextRefreshIntervalMs(settings)

                if (isSelectedProfile) {
                    repository.selectProfile(profileId)
                    if (isActiveProfileRequestValid(profileId, selectedRequestVersion)) {
                        syncSelectedUiFromSnapshot(
                            profileId = profileId,
                            settings = settings,
                            snapshot = snapshot,
                            connected = true,
                            selectedErrorMessage = null,
                            requestVersion = selectedRequestVersion,
                        )
                    }
                }
            }.onFailure { error ->
                Log.w("QBRemote", "refreshServerSnapshotNow failed for profile=$profileId", error)
                val summaryMessage = userFacingConnectionMessage(error)
                val currentSnapshot = _uiState.value.dashboardServerSnapshots
                    .firstOrNull { it.profileId == profileId }
                    ?: loadStoredDashboardSnapshot(profileId)
                val staleSnapshot = (currentSnapshot ?: CachedDashboardServerSnapshot(
                    profileId = profileId,
                    profileName = profile?.name ?: settings.host,
                    backendType = profile?.backendType ?: settings.serverBackendType,
                    host = profile?.host ?: settings.host,
                    port = profile?.port ?: settings.port,
                    useHttps = profile?.useHttps ?: settings.useHttps,
                )).copy(
                    profileName = profile?.name ?: currentSnapshot?.profileName ?: settings.host,
                    backendType = profile?.backendType ?: currentSnapshot?.backendType ?: settings.serverBackendType,
                    host = profile?.host ?: currentSnapshot?.host ?: settings.host,
                    port = profile?.port ?: currentSnapshot?.port ?: settings.port,
                    useHttps = profile?.useHttps ?: currentSnapshot?.useHttps ?: settings.useHttps,
                    errorMessage = summaryMessage,
                    isStale = true,
                )
                persistDashboardSnapshot(staleSnapshot)
                mergeDashboardSnapshot(staleSnapshot, sampleFreshData = false)
                nextServerRefreshAt[profileId] = System.currentTimeMillis() + nextRefreshIntervalMs(settings)

                if (isSelectedProfile && error is BackendConnectionError.WrongBackend) {
                    maybeQueueBackendRepair(
                        profileId = profileId,
                        profileName = profile?.name ?: staleSnapshot.profileName,
                        error = error,
                    )
                }

                if (isSelectedProfile) {
                    repository.selectProfile(profileId)
                    if (isActiveProfileRequestValid(profileId, selectedRequestVersion)) {
                        syncSelectedUiFromSnapshot(
                            profileId = profileId,
                            settings = settings,
                            snapshot = staleSnapshot,
                            connected = false,
                            selectedErrorMessage = if (error is BackendConnectionError.WrongBackend) {
                                null
                            } else if (showSelectedError && !shouldSuppressRefreshError(summaryMessage)) {
                                summaryMessage
                            } else {
                                null
                            },
                            requestVersion = selectedRequestVersion,
                        )
                    }
                }
            }
        }
    }

internal suspend fun MainViewModel.syncSelectedUiFromStoredSnapshot() {
        val profileId = _uiState.value.activeServerProfileId ?: return
        val settings = resolveProfileSettings(profileId) ?: return
        val snapshot = _uiState.value.dashboardServerSnapshots.firstOrNull { it.profileId == profileId }
            ?: loadStoredDashboardSnapshot(profileId)
        repository.selectProfile(profileId)
        val requestVersion = currentActiveProfileRequestVersion()
        syncSelectedUiFromSnapshot(
            profileId = profileId,
            settings = settings,
            snapshot = snapshot,
            connected = repository.isConnected(profileId) && snapshot?.isStale == false,
            selectedErrorMessage = null,
            requestVersion = requestVersion,
        )
    }

internal suspend fun MainViewModel.syncSelectedUiFromSnapshot(
        profileId: String,
        settings: ConnectionSettings,
        snapshot: CachedDashboardServerSnapshot?,
        connected: Boolean,
        selectedErrorMessage: String?,
        requestVersion: Long,
    ) {
        if (!isActiveProfileRequestValid(profileId, requestVersion)) return

        val categoryOptions = if (connected) {
            repository.fetchCategoryOptions(profileId).getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val tagOptions = if (connected) {
            repository.fetchTagOptions(profileId).getOrElse { emptyList() }
        } else {
            emptyList()
        }

        _uiState.update { current ->
            if (!isActiveProfileRequestValid(profileId, requestVersion)) {
                current
            } else {
                current.copy(
                    settings = settings,
                    activeCapabilities = repository.capabilitiesFor(settings),
                    isConnecting = false,
                    connected = connected,
                    serverVersion = snapshot?.serverVersion?.ifBlank { "-" } ?: "-",
                    transferInfo = snapshot?.transferInfo ?: TransferInfo(),
                    torrents = snapshot?.torrents ?: emptyList(),
                    dailyTagUploadDate = snapshot?.dailyTagUploadDate.orEmpty(),
                    dailyTagUploadStats = snapshot?.dailyTagUploadStats?.map { stat ->
                        DailyTagUploadStat(
                            tag = stat.tag,
                            uploadedBytes = stat.uploadedBytes,
                            torrentCount = stat.torrentCount,
                            isNoTag = stat.isNoTag,
                        )
                    }.orEmpty(),
                    dailyCountryUploadDate = snapshot?.dailyCountryUploadDate.orEmpty(),
                    dailyCountryUploadStats = snapshot?.dailyCountryUploadStats.orEmpty(),
                    categoryOptions = categoryOptions,
                    tagOptions = tagOptions,
                    dashboardCacheHydrated = true,
                    hasDashboardSnapshot = snapshot != null,
                    pendingBackendRepair = current.pendingBackendRepair
                        ?.takeUnless { connected && it.profileId == profileId },
                    errorMessage = selectedErrorMessage,
                )
            }
        }

        if (snapshot != null && isActiveProfileRequestValid(profileId, requestVersion)) {
            saveDashboardCache()
        }

        val detailHash = _uiState.value.detailHash
        if (connected && _uiState.value.refreshScene == RefreshScene.TORRENT_DETAIL && detailHash.isNotBlank()) {
            refreshDetailSnapshot(profileId, detailHash, requestVersion)
        }
    }

internal suspend fun MainViewModel.mergeDashboardSnapshot(
        snapshot: CachedDashboardServerSnapshot,
        sampleFreshData: Boolean,
    ) {
        val current = _uiState.value
        val snapshotsById = current.dashboardServerSnapshots
            .associateBy { it.profileId }
            .toMutableMap()
        snapshotsById[snapshot.profileId] = snapshot
        val ordered = orderedDashboardServerSnapshots(current.serverProfiles, snapshotsById)
        val aggregate = buildDashboardAggregateWithHistory(
            snapshots = ordered,
            sampleFreshData = sampleFreshData,
        )
        _uiState.update { latest ->
            applyDashboardSnapshotsToState(
                current = latest,
                orderedSnapshots = ordered,
                aggregate = aggregate,
            )
        }
    }

internal fun MainViewModel.nextRefreshIntervalMs(settings: ConnectionSettings): Long {
        return settings.refreshSeconds.coerceIn(5, 120) * 1_000L
    }

internal fun MainViewModel.refreshDashboardServerSnapshotsAsync(skipActive: Boolean = false) {
        dashboardAggregationJob?.cancel()
        dashboardAggregationJob = viewModelScope.launch {
            val profiles = _uiState.value.serverProfiles
            if (profiles.isEmpty()) {
                realtimeSpeedTracker.mutex.withLock {
                    resetHomeRealtimeSpeedSeriesStateLocked(clearPersisted = true)
                }
                _uiState.update { current ->
                    current.copy(
                        dashboardServerSnapshots = emptyList(),
                        selectedDashboardProfileId = null,
                        dashboardAggregate = DashboardAggregateState(),
                        aggregateOnlineServerCount = 0,
                    )
                }
                markInitialDashboardSnapshotsHydrated()
                return@launch
            }

            val snapshots = loadDashboardSnapshotsMap()
            val activeProfileId = _uiState.value.activeServerProfileId
            val activeProfile = profiles.firstOrNull { it.id == activeProfileId }

            if (!skipActive && _uiState.value.connected && activeProfile != null) {
                val activeSnapshot = buildActiveDashboardServerSnapshot(activeProfile, _uiState.value)
                persistDashboardSnapshot(activeSnapshot, snapshots)
            }

            val refreshResults = supervisorScope {
                profiles.mapNotNull { profile ->
                    if (profile.id == activeProfileId && _uiState.value.connected) {
                        null
                    } else {
                        async {
                            val previousSnapshot = snapshots[profile.id]
                            val settings = resolveProfileSettings(profile.id)
                            if (settings == null) {
                                DashboardSnapshotRefreshResult.Failure(
                                    profile = profile,
                                    error = IllegalStateException("Missing saved settings."),
                                    previousSnapshot = previousSnapshot,
                                )
                            } else {
                                repository.fetchDashboardSnapshot(settings).fold(
                                    onSuccess = { fetched ->
                                        DashboardSnapshotRefreshResult.Fresh(
                                            profile = profile,
                                            settings = settings,
                                            fetched = fetched,
                                            previousSnapshot = previousSnapshot,
                                        )
                                    },
                                    onFailure = { error ->
                                        DashboardSnapshotRefreshResult.Failure(
                                            profile = profile,
                                            error = error,
                                            previousSnapshot = previousSnapshot,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }.awaitAll()
            }

            val pendingStatsRefreshes = mutableListOf<DashboardStatsRefreshInput>()
            refreshResults.forEach { result ->
                when (result) {
                    is DashboardSnapshotRefreshResult.Fresh -> {
                        val baseSnapshot = buildCachedDashboardSnapshotFromFetch(
                            profile = result.profile,
                            fetched = result.fetched,
                            previousSnapshot = result.previousSnapshot,
                        )
                        persistDashboardSnapshot(baseSnapshot, snapshots)
                        pendingStatsRefreshes += DashboardStatsRefreshInput(
                            profile = result.profile,
                            settings = result.settings,
                            torrents = result.fetched.dashboardData.torrents,
                            baseSnapshot = baseSnapshot,
                        )
                    }

                    is DashboardSnapshotRefreshResult.Failure -> {
                        val staleSnapshot = (result.previousSnapshot ?: CachedDashboardServerSnapshot(
                            profileId = result.profile.id,
                            profileName = result.profile.name,
                            backendType = result.profile.backendType,
                            host = result.profile.host,
                            port = result.profile.port,
                            useHttps = result.profile.useHttps,
                        )).copy(
                            profileName = result.profile.name,
                            backendType = result.profile.backendType,
                            host = result.profile.host,
                            port = result.profile.port,
                            useHttps = result.profile.useHttps,
                            errorMessage = result.error.message ?: "Refresh failed.",
                            isStale = true,
                        )
                        persistDashboardSnapshot(staleSnapshot, snapshots)
                    }
                }
            }

            val ordered = orderedDashboardServerSnapshots(profiles, snapshots)
            val aggregate = buildDashboardAggregateWithHistory(
                snapshots = ordered,
                sampleFreshData = true,
            )
            _uiState.update { current ->
                applyDashboardSnapshotsToState(
                    current = current,
                    orderedSnapshots = ordered,
                    aggregate = aggregate,
                )
            }
            markInitialDashboardSnapshotsHydrated()

            if (pendingStatsRefreshes.isEmpty()) return@launch

            val enrichedSnapshots = supervisorScope {
                pendingStatsRefreshes.map { input ->
                    async {
                        enrichDashboardSnapshotStats(input)
                    }
                }.awaitAll()
            }

            if (!isActive) return@launch

            enrichedSnapshots.forEach { snapshot ->
                persistDashboardSnapshot(snapshot, snapshots)
            }

            val orderedEnriched = orderedDashboardServerSnapshots(profiles, snapshots)
            val aggregateWithEnrichedStats = buildDashboardAggregateWithHistory(
                snapshots = orderedEnriched,
                sampleFreshData = false,
            )
            _uiState.update { current ->
                applyDashboardSnapshotsToState(
                    current = current,
                    orderedSnapshots = orderedEnriched,
                    aggregate = aggregateWithEnrichedStats,
                )
            }
            markInitialDashboardSnapshotsHydrated()
        }
    }

internal fun MainViewModel.buildCachedDashboardSnapshotFromFetch(
        profile: ServerProfile,
        fetched: com.hjw.qbremote.data.DashboardSnapshotFetchResult,
        previousSnapshot: CachedDashboardServerSnapshot?,
    ): CachedDashboardServerSnapshot {
        val preservedCountryDate = if (defaultCapabilitiesFor(profile.backendType).supportsCountryDistribution) {
            previousSnapshot?.dailyCountryUploadDate.orEmpty()
        } else {
            ""
        }
        val preservedCountryStats = if (defaultCapabilitiesFor(profile.backendType).supportsCountryDistribution) {
            previousSnapshot?.dailyCountryUploadStats ?: emptyList()
        } else {
            emptyList()
        }
        return CachedDashboardServerSnapshot(
            profileId = profile.id,
            profileName = profile.name,
            backendType = profile.backendType,
            host = profile.host,
            port = profile.port,
            useHttps = profile.useHttps,
            serverVersion = fetched.serverVersion,
            transferInfo = fetched.dashboardData.transferInfo,
            torrents = fetched.dashboardData.torrents,
            dailyTagUploadDate = previousSnapshot?.dailyTagUploadDate.orEmpty(),
            dailyTagUploadStats = previousSnapshot?.dailyTagUploadStats ?: emptyList(),
            dailyCountryUploadDate = preservedCountryDate,
            dailyCountryUploadStats = preservedCountryStats,
            lastUpdatedAt = System.currentTimeMillis(),
            errorMessage = "",
            isStale = false,
        )
    }

internal suspend fun MainViewModel.enrichDashboardSnapshotStats(
        input: DashboardStatsRefreshInput,
    ): CachedDashboardServerSnapshot {
        val tagStats = buildDashboardTagUploadStatsForScope(
            scopeKey = "profile:${input.profile.id}",
            torrents = input.torrents,
        )
        val countryStats = if (repository.capabilitiesFor(input.settings).supportsCountryDistribution) {
            buildDashboardCountryUploadStatsForScope(
                scopeKey = "profile:${input.profile.id}",
                torrents = input.torrents,
                fetchPeerSnapshots = { hashes ->
                    repository.fetchCountryPeerSnapshots(input.settings, hashes)
                        .getOrElse { emptyList() }
                },
            )
        } else {
            DailyCountryUploadStats(
                dateLabel = tagStats.first,
                countries = emptyList(),
            )
        }
        return input.baseSnapshot.copy(
            dailyTagUploadDate = tagStats.first,
            dailyTagUploadStats = tagStats.second.map { stat ->
                CachedDailyTagUploadStat(
                    tag = stat.tag,
                    uploadedBytes = stat.uploadedBytes,
                    torrentCount = stat.torrentCount,
                    isNoTag = stat.isNoTag,
                )
            },
            dailyCountryUploadDate = countryStats.dateLabel,
            dailyCountryUploadStats = countryStats.countries,
            lastUpdatedAt = System.currentTimeMillis(),
        )
    }

internal suspend fun MainViewModel.saveActiveDashboardServerSnapshot() {
        val state = _uiState.value
        val activeProfileId = state.activeServerProfileId ?: return
        saveDashboardServerSnapshotForProfile(
            profileId = activeProfileId,
            stateSnapshot = state,
        )
    }

internal suspend fun MainViewModel.saveDashboardServerSnapshotForProfile(
        profileId: String,
        stateSnapshot: MainUiState,
    ) {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return
        val targetProfile = stateSnapshot.serverProfiles.firstOrNull { it.id == normalizedProfileId } ?: return
        val snapshot = buildActiveDashboardServerSnapshot(targetProfile, stateSnapshot)
        persistDashboardSnapshot(snapshot)
    }

internal suspend fun MainViewModel.loadDashboardSnapshotsMap(): MutableMap<String, CachedDashboardServerSnapshot> {
        return connectionStore.loadDashboardServerSnapshots().toMutableMap()
    }

internal suspend fun MainViewModel.loadStoredDashboardSnapshot(
        profileId: String,
    ): CachedDashboardServerSnapshot? {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) return null
        return connectionStore.loadDashboardServerSnapshots()[normalizedProfileId]
    }

internal suspend fun MainViewModel.persistDashboardSnapshot(
        snapshot: CachedDashboardServerSnapshot,
        snapshots: MutableMap<String, CachedDashboardServerSnapshot>? = null,
    ) {
        val normalizedProfileId = snapshot.profileId.trim()
        if (normalizedProfileId.isBlank()) return
        val normalizedSnapshot = if (normalizedProfileId == snapshot.profileId) {
            snapshot
        } else {
            snapshot.copy(profileId = normalizedProfileId)
        }
        snapshots?.set(normalizedSnapshot.profileId, normalizedSnapshot)
        connectionStore.saveDashboardServerSnapshot(normalizedSnapshot)
    }

internal fun MainViewModel.buildActiveDashboardServerSnapshot(
        profile: ServerProfile,
        state: MainUiState,
    ): CachedDashboardServerSnapshot {
        return CachedDashboardServerSnapshot(
            profileId = profile.id,
            profileName = profile.name,
            backendType = profile.backendType,
            host = profile.host,
            port = profile.port,
            useHttps = profile.useHttps,
            serverVersion = state.serverVersion,
            transferInfo = state.transferInfo,
            torrents = state.torrents,
            dailyTagUploadDate = state.dailyTagUploadDate,
            dailyTagUploadStats = state.dailyTagUploadStats.map { stat ->
                CachedDailyTagUploadStat(
                    tag = stat.tag,
                    uploadedBytes = stat.uploadedBytes,
                    torrentCount = stat.torrentCount,
                    isNoTag = stat.isNoTag,
                )
            },
            dailyCountryUploadDate = state.dailyCountryUploadDate,
            dailyCountryUploadStats = state.dailyCountryUploadStats,
            lastUpdatedAt = System.currentTimeMillis(),
            errorMessage = "",
            isStale = false,
        )
    }

internal fun MainViewModel.orderedDashboardServerSnapshots(
        profiles: List<ServerProfile>,
        snapshotsById: Map<String, CachedDashboardServerSnapshot>,
    ): List<CachedDashboardServerSnapshot> {
        return profiles.map { profile ->
            snapshotsById[profile.id]?.copy(
                profileName = profile.name,
                backendType = profile.backendType,
                host = profile.host,
                port = profile.port,
                useHttps = profile.useHttps,
            ) ?: CachedDashboardServerSnapshot(
                profileId = profile.id,
                profileName = profile.name,
                backendType = profile.backendType,
                host = profile.host,
                port = profile.port,
                useHttps = profile.useHttps,
                isStale = true,
            )
        }
    }

internal suspend fun MainViewModel.buildDashboardAggregateWithHistory(
        snapshots: List<CachedDashboardServerSnapshot>,
        sampleFreshData: Boolean,
    ): DashboardAggregateState {
        if (snapshots.isEmpty()) {
            realtimeSpeedTracker.mutex.withLock {
                resetHomeRealtimeSpeedSeriesStateLocked(clearPersisted = true)
            }
            return DashboardAggregateState()
        }
        val scopeKey = resolveHomeRealtimeSpeedScopeKey(snapshots)
        val aggregate = buildDashboardAggregateFromSnapshots(snapshots)
        val liveServerCount = snapshots.count { !it.isStale }
        val realtimeSpeedSeries = realtimeSpeedTracker.mutex.withLock {
            ensureHomeRealtimeSpeedSeriesLoadedLocked(scopeKey)
            when {
                liveServerCount <= 0 -> {
                    clearHomeRealtimeSpeedSeriesLocked(scopeKey)
                    emptyList()
                }
                sampleFreshData -> sampleHomeRealtimeSpeedPointLocked(
                    transferInfo = aggregate.transferInfo,
                    onlineServerCount = liveServerCount,
                    scopeKey = scopeKey,
                )
                else -> realtimeSpeedTracker.series.toList()
            }
        }
        return aggregate.copy(
            chartTransferInfo = null,
            realtimeSpeedSeries = realtimeSpeedSeries,
        )
    }

internal suspend fun MainViewModel.buildDashboardTagUploadStatsForScope(
        scopeKey: String,
        torrents: List<TorrentInfo>,
    ): Pair<String, List<DailyTagUploadStat>> {
        val today = LocalDate.now()
        val (updatedSnapshot, stats) = advanceDailyUploadTrackingSnapshot(
            previousSnapshot = connectionStore.loadDailyUploadTrackingSnapshot(scopeKey),
            today = today,
            torrents = torrents,
        )
        connectionStore.saveDailyUploadTrackingSnapshot(
            scopeKey = scopeKey,
            snapshot = updatedSnapshot,
        )
        return updatedSnapshot.date.ifBlank { today.toString() } to stats
    }

internal suspend fun MainViewModel.buildDashboardCountryUploadStatsForScope(
        scopeKey: String,
        torrents: List<TorrentInfo>,
        fetchPeerSnapshots: suspend (List<String>) -> List<CountryPeerSnapshot>,
    ): com.hjw.qbremote.data.model.DailyCountryUploadStats {
        val snapshot = connectionStore.loadDailyCountryUploadTrackingSnapshot(scopeKey)
        val today = LocalDate.now()
        val totalsByCountry = snapshot?.totalsByCountry?.toMutableMap() ?: mutableMapOf()
        val peerSnapshots = snapshot?.peerSnapshots?.toMutableMap() ?: mutableMapOf()
        val lastSeenByTorrent = snapshot?.lastSeenByTorrent?.toMutableMap() ?: mutableMapOf()
        val snapshotDate = runCatching {
            snapshot?.date?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        }.getOrNull()

        if (snapshotDate != today) {
            totalsByCountry.clear()
            peerSnapshots.clear()
            lastSeenByTorrent.clear()
        }

        val activeKeys = torrents.map(::torrentTrackingKey).toSet()
        lastSeenByTorrent.keys.retainAll(activeKeys)

        val activeHashes = mutableListOf<String>()
        torrents.forEach { torrent ->
            val trackingKey = torrentTrackingKey(torrent)
            val hash = torrent.hash.trim()
            if (hash.isBlank()) return@forEach
            val currentUploaded = torrent.uploaded.coerceAtLeast(0L)
            val previousUploaded = lastSeenByTorrent[trackingKey]
            lastSeenByTorrent[trackingKey] = currentUploaded
            if (previousUploaded == null) {
                if (torrent.uploadSpeed > 0L) {
                    activeHashes += hash
                }
                return@forEach
            }
            if (currentUploaded > previousUploaded || torrent.uploadSpeed > 0L) {
                activeHashes += hash
            }
        }

        val samples = fetchPeerSnapshots(activeHashes.distinct())
        val currentPeerSnapshots = samples.associateBy { it.key }
        val fallbackNames = samples
            .groupBy { it.countryCode.trim().uppercase(Locale.US) }
            .mapValues { (_, entries) ->
                entries.firstNotNullOfOrNull { it.countryName.trim().takeIf(String::isNotBlank) }.orEmpty()
            }

        samples.forEach { entry ->
            val countryCode = entry.countryCode.trim().uppercase(Locale.US)
            if (countryCode.isBlank()) return@forEach
            val previous = peerSnapshots[entry.key]
            val previousUploaded = previous?.uploadedBytes?.coerceAtLeast(0L)
            val currentUploaded = entry.uploadedBytes.coerceAtLeast(0L)
            val delta = when {
                previousUploaded == null -> 0L
                currentUploaded < previousUploaded -> currentUploaded
                else -> currentUploaded - previousUploaded
            }
            if (delta <= 0L) return@forEach
            totalsByCountry[countryCode] = (totalsByCountry[countryCode] ?: 0L) + delta
        }

        peerSnapshots.keys.retainAll(currentPeerSnapshots.keys)
        peerSnapshots.putAll(currentPeerSnapshots)

        connectionStore.saveDailyCountryUploadTrackingSnapshot(
            scopeKey = scopeKey,
            snapshot = DailyCountryUploadTrackingSnapshot(
                date = today.toString(),
                totalsByCountry = totalsByCountry,
                peerSnapshots = peerSnapshots,
                lastSeenByTorrent = lastSeenByTorrent,
            ),
        )

        return com.hjw.qbremote.data.model.DailyCountryUploadStats(
            dateLabel = today.toString(),
            countries = totalsByCountry.entries
                .filter { it.value > 0L }
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

internal suspend fun MainViewModel.sampleHomeRealtimeSpeedPointLocked(
        transferInfo: TransferInfo,
        onlineServerCount: Int,
        scopeKey: String,
    ): List<RealtimeSpeedPoint> {
        return realtimeSpeedTracker.sampleLocked(transferInfo, onlineServerCount, scopeKey)
    }

internal suspend fun MainViewModel.clearHomeRealtimeSpeedSeriesLocked(scopeKey: String) {
        realtimeSpeedTracker.clearLocked(scopeKey)
    }

internal suspend fun MainViewModel.resetHomeRealtimeSpeedSeriesStateLocked(clearPersisted: Boolean) {
        realtimeSpeedTracker.resetLocked(clearPersisted)
    }

internal suspend fun MainViewModel.ensureHomeRealtimeSpeedSeriesLoadedLocked(scopeKey: String) {
        realtimeSpeedTracker.ensureLoadedLocked(scopeKey)
    }

internal fun MainViewModel.resolveHomeRealtimeSpeedScopeKey(
        snapshots: List<CachedDashboardServerSnapshot>,
    ): String {
        return realtimeSpeedTracker.resolveScopeKey(snapshots, currentDailyUploadTrackingScopeKey())
    }


internal fun MainViewModel.parseLimitKbToBytes(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return -1L
        val kb = trimmed.toLongOrNull() ?: throw IllegalArgumentException("限速值必须是数字")
        if (kb < 0L) return -1L
        return kb * 1024L
    }

internal fun MainViewModel.shouldSuppressRefreshError(message: String?): Boolean {
        val normalized = message?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return false
        return normalized.contains("unable to resolve host") ||
            normalized.contains("no address associated with hostname")
    }

internal fun MainViewModel.maybeQueueBackendRepair(
        profileId: String,
        profileName: String,
        error: BackendConnectionError.WrongBackend,
    ) {
        _uiState.update { current ->
            current.copy(
                pendingBackendRepair = PendingBackendRepair(
                    profileId = profileId,
                    profileName = profileName.ifBlank { profileId },
                    expectedBackend = error.expected,
                    detectedBackend = error.detected,
                    detail = error.detail,
                ),
            )
        }
    }

internal fun MainViewModel.userFacingConnectionMessage(error: Throwable): String {
        return when (error) {
            is BackendConnectionError.WrongBackend -> {
                "服务器类型不匹配，目标看起来是 ${backendDisplayName(error.detected)}。"
            }

            is BackendConnectionError.RpcPathNotFound -> {
                if (error.failureSummary.isBlank()) {
                    "Transmission RPC 路径未找到。"
                } else {
                    "Transmission RPC 路径未找到。${error.failureSummary}"
                }
            }

            is BackendConnectionError.AuthFailed -> "${backendDisplayName(error.backendType)} 认证失败。"
            else -> error.message?.takeIf { it.isNotBlank() } ?: "刷新失败"
        }
    }

internal fun MainViewModel.backendDisplayName(type: ServerBackendType): String {
        return when (type) {
            ServerBackendType.QBITTORRENT -> "qBittorrent"
            ServerBackendType.TRANSMISSION -> "Transmission"
        }
    }

internal fun MainViewModel.hydrateDashboardCacheForCurrentScope(force: Boolean = false) {
        val scopeKey = currentDailyUploadTrackingScopeKey()
        if (!force && scopeKey == hydratedDashboardScopeKey && _uiState.value.dashboardCacheHydrated) {
            return
        }

        hydratedDashboardScopeKey = scopeKey
        dashboardCacheHydrationJob?.cancel()
        _uiState.update { current ->
            current.copy(
                dashboardCacheHydrated = false,
            )
        }

        dashboardCacheHydrationJob = viewModelScope.launch {
            val cache = connectionStore.loadDashboardCacheSnapshot(scopeKey)
            if (hydratedDashboardScopeKey != scopeKey) return@launch

            _uiState.update { current ->
                if (hydratedDashboardScopeKey != scopeKey) {
                    current
                } else {
                    applyDashboardCacheHydration(
                        current = current,
                        cache = cache,
                    )
                }
            }
            markInitialDashboardCacheHydrated()
        }
    }

internal fun MainViewModel.updateSettings(update: (ConnectionSettings) -> ConnectionSettings) {
        _uiState.update { current ->
            val nextSettings = update(current.settings)
            if (nextSettings == current.settings) {
                current
            } else {
                current.copy(settings = nextSettings)
            }
        }
    }

internal fun MainViewModel.updateAndPersistSettings(update: (ConnectionSettings) -> ConnectionSettings) {
        var changed = false
        _uiState.update { current ->
            val nextSettings = update(current.settings)
            if (nextSettings == current.settings) {
                current
            } else {
                changed = true
                current.copy(settings = nextSettings)
            }
        }
        if (!changed) return
        val settingsToPersist = _uiState.value.settings
        viewModelScope.launch {
            connectionStore.save(settingsToPersist)
        }
    }

internal fun MainViewModel.startAutoRefresh() = backgroundJobManager.startAutoRefresh()
internal fun MainViewModel.startHomeChartRefresh() = backgroundJobManager.startHomeChartRefresh()
internal fun MainViewModel.startHourlyBoundaryRefresh() = backgroundJobManager.startHourlyBoundaryRefresh()

internal suspend fun MainViewModel.refreshHomeDashboardChartTransferInfo() {
        val state = _uiState.value
        if (state.refreshScene != RefreshScene.DASHBOARD) return
        val profiles = state.serverProfiles
        if (profiles.isEmpty()) return
        val requestedProfileIds = normalizeProfileIdsForRefresh(profiles)

        val activeProfileId = state.activeServerProfileId
        val transferInfoByProfileId = supervisorScope {
            profiles.map { profile ->
                async {
                    val settings = resolveProfileSettings(profile.id)
                        ?: return@async null
                    val result = if (
                        profile.id == activeProfileId &&
                        repository.isConnected(profile.id)
                    ) {
                        repository.fetchTransferInfo(profile.id)
                    } else {
                        repository.fetchTransferInfo(settings)
                    }
                    result.getOrNull()?.let { transferInfo ->
                        profile.id to transferInfo
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .toMap()
        }
        if (transferInfoByProfileId.isEmpty()) return
        if (state.dashboardServerSnapshots.isEmpty()) return

        val latestState = _uiState.value
        if (latestState.refreshScene != RefreshScene.DASHBOARD) return
        val latestProfileIds = normalizeProfileIdsForRefresh(latestState.serverProfiles)
        if (latestProfileIds != requestedProfileIds) return

        val chartTransferInfo = buildHomeChartTransferInfo(transferInfoByProfileId.values)
        val scopeKey = resolveHomeRealtimeSpeedScopeKey(latestState.dashboardServerSnapshots)
        val chartSeries = realtimeSpeedTracker.mutex.withLock {
            ensureHomeRealtimeSpeedSeriesLoadedLocked(scopeKey)
            sampleHomeRealtimeSpeedPointLocked(
                transferInfo = chartTransferInfo,
                onlineServerCount = transferInfoByProfileId.size.coerceAtLeast(1),
                scopeKey = scopeKey,
            )
        }

        val latestStateAfterSampling = _uiState.value
        if (latestStateAfterSampling.refreshScene != RefreshScene.DASHBOARD) return
        val latestProfileIdsAfterSampling = normalizeProfileIdsForRefresh(latestStateAfterSampling.serverProfiles)
        if (latestProfileIdsAfterSampling != requestedProfileIds) return

        _uiState.update { current ->
            current.copy(
                dashboardAggregate = applyHomeChartRefreshToAggregate(
                    aggregate = current.dashboardAggregate,
                    chartTransferInfo = chartTransferInfo,
                    chartSeries = chartSeries,
                ),
            )
        }
    }

