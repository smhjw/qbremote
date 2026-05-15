package com.hjw.qbremote.ui

internal data class HomeServerStackDropPlan(
    val reorderedIds: List<String>,
    val finalTargetIndex: Int,
    val finalOffsetY: Float,
    val shouldCommitReorder: Boolean,
)

internal fun resolveHomeServerStackDropPlan(
    state: VerticalReorderDragState<String>,
    commit: Boolean,
): HomeServerStackDropPlan {
    val profileId = state.item
    val finalTargetIndex = resolveVerticalReorderFinalTargetIndex(
        state = state,
        commit = commit,
    )
    val finalOffsetY = resolveVerticalReorderRestingOffset(
        state = state,
        commit = commit,
    )
    val reorderedIds = if (commit) {
        reorderHomeServerProfileIds(
            current = state.session.items,
            profileId = profileId,
            targetIndex = finalTargetIndex,
        )
    } else {
        state.session.items
    }
    val shouldCommitReorder = commit && reorderedIds != state.session.items
    return HomeServerStackDropPlan(
        reorderedIds = reorderedIds,
        finalTargetIndex = finalTargetIndex,
        finalOffsetY = finalOffsetY,
        shouldCommitReorder = shouldCommitReorder,
    )
}

private fun reorderHomeServerProfileIds(
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
