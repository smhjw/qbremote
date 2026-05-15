package com.hjw.qbremote.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.hjw.qbremote.R
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.ui.theme.qbGlassStrongContainerColor

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.torrentListPageContent(
    showContent: Boolean,
    showRestorePlaceholder: Boolean,
    showSearchBar: Boolean,
    animatePlacement: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterState: TorrentListFilterState,
    onStateFilterChange: (TorrentStateFilter) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onTagFilterChange: (String) -> Unit,
    categoryOptions: List<String>,
    tagOptions: List<String>,
    visibleItems: List<VisibleTorrentItem>,
    isPendingAction: (String) -> Boolean,
    onOpenDetails: (TorrentInfo) -> Unit,
    onOpenConnection: () -> Unit,
) {
    if (showContent) {
        if (showSearchBar) {
            stickyHeader(key = "torrent_search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(qbGlassStrongContainerColor())
                        .padding(bottom = 8.dp),
                    label = { Text(stringResource(R.string.search_torrent_label)) },
                    placeholder = { Text(stringResource(R.string.search_torrent_placeholder)) },
                    singleLine = true,
                )
            }
            stickyHeader(key = "torrent_filter_chips") {
                FilterChipRow(
                    filterState = filterState,
                    onStateFilterChange = onStateFilterChange,
                    onCategoryFilterChange = onCategoryFilterChange,
                    onTagFilterChange = onTagFilterChange,
                    categoryOptions = categoryOptions,
                    tagOptions = tagOptions,
                )
            }
        }
        items(
            items = visibleItems,
            key = { item -> item.torrent.hash.ifBlank { item.identityKey } },
        ) { item ->
            val itemModifier = if (animatePlacement) {
                Modifier.animateItemPlacement(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 380f,
                    )
                )
            } else {
                Modifier
            }
            Box(modifier = itemModifier) {
                TorrentCard(
                    torrent = item.torrent,
                    crossSeedCount = item.crossSeedCount,
                    isPending = isPendingAction(item.torrent.hash),
                    onOpenDetails = { onOpenDetails(item.torrent) },
                )
            }
        }
        if (visibleItems.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_torrent_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else if (showRestorePlaceholder) {
        item {
            PageRestorePlaceholder()
        }
    } else {
        item {
            NeedConnectionCard(
                onOpenConnection = onOpenConnection,
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    filterState: TorrentListFilterState,
    onStateFilterChange: (TorrentStateFilter) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onTagFilterChange: (String) -> Unit,
    categoryOptions: List<String>,
    tagOptions: List<String>,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        selectedLabelColor = MaterialTheme.colorScheme.primary,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(qbGlassStrongContainerColor())
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Row 1: State filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TorrentStateFilter.entries.forEach { stateFilter ->
                FilterChip(
                    selected = filterState.stateFilter == stateFilter,
                    onClick = { onStateFilterChange(stateFilter) },
                    label = { Text(stringResource(stateFilter.labelKey)) },
                    colors = chipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = true,
                        selected = filterState.stateFilter == stateFilter,
                    ),
                )
            }
        }
        // Row 2: Category filters
        if (categoryOptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                categoryOptions.forEach { category ->
                    FilterChip(
                        selected = filterState.categoryFilter == category,
                        onClick = { onCategoryFilterChange(category) },
                        label = { Text(category) },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            enabled = true,
                            selected = filterState.categoryFilter == category,
                        ),
                    )
                }
            }
        }
        // Row 3: Tag filters
        if (tagOptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tagOptions.forEach { tag ->
                    FilterChip(
                        selected = filterState.tagFilter == tag,
                        onClick = { onTagFilterChange(tag) },
                        label = { Text(tag) },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            enabled = true,
                            selected = filterState.tagFilter == tag,
                        ),
                    )
                }
            }
        }
    }
}

internal fun shouldAnimateTorrentPlacement(
    previousKeys: List<String>,
    currentKeys: List<String>,
    sortOption: TorrentListSortOption? = null,
): Boolean {
    if (sortOption == TorrentListSortOption.UPLOAD_SPEED || sortOption == TorrentListSortOption.DOWNLOAD_SPEED) {
        return false
    }
    if (previousKeys.size != currentKeys.size) return false
    if (previousKeys == currentKeys) return false
    if (buildTorrentPlacementKeyCountMap(previousKeys) != buildTorrentPlacementKeyCountMap(currentKeys)) {
        return false
    }
    return true
}

private fun buildTorrentPlacementKeyCountMap(keys: List<String>): Map<String, Int> {
    val counts = linkedMapOf<String, Int>()
    keys.forEach { key ->
        counts[key] = (counts[key] ?: 0) + 1
    }
    return counts
}

@Composable
private fun SwipeableTorrentItem(
    modifier: Modifier = Modifier,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit,
    isPending: Boolean,
    content: @Composable () -> Unit,
) {
    val buttonWidth = 72.dp
    val buttonWidthPx = with(LocalDensity.current) { buttonWidth.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val isSwipedLeft = offsetX < -buttonWidthPx * 0.3f
    val isSwipedRight = offsetX > buttonWidthPx * 0.3f
    val animatedOffset by animateDpAsState(
        targetValue = when {
            offsetX > buttonWidthPx * 0.5f -> buttonWidth
            offsetX < -buttonWidthPx * 0.5f -> -buttonWidth
            else -> 0.dp
        },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "swipeOffset",
    )
    val offsetPx = with(LocalDensity.current) { animatedOffset.roundToPx() }

    Box(modifier = modifier) {
        // Background: small action button visible only when actively swiped
        val buttonSize = 40.dp
        if (isSwipedRight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = (buttonWidth - buttonSize) / 2),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(10.dp))
                        .clickable { onPauseResume(); offsetX = 0f },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        } else if (isSwipedLeft) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = (buttonWidth - buttonSize) / 2),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .background(Color(0xFFE53935), RoundedCornerShape(10.dp))
                        .clickable { onDelete(); offsetX = 0f },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Foreground card with drag
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPx, 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = buttonWidthPx * 0.4f
                            offsetX = when {
                                offsetX > threshold -> buttonWidthPx
                                offsetX < -threshold -> -buttonWidthPx
                                else -> 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-buttonWidthPx, buttonWidthPx)
                        },
                    )
                },
        ) {
            content()
        }
    }
}
