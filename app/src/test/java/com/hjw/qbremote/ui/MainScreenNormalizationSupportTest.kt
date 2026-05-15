package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenNormalizationSupportTest {

    @Test
    fun buildSortedDistinctTrimmedStrings_removesBlankAndDuplicateValues() {
        val normalized = buildSortedDistinctTrimmedStrings(
            listOf("  movies ", "", "linux", "Movies", "linux", " "),
        )

        assertEquals(listOf("Movies", "linux", "movies"), normalized)
    }

    @Test
    fun buildSortedDistinctTrimmedStrings_preservesSortedResult() {
        val normalized = buildSortedDistinctTrimmedStrings(
            listOf("zeta", "alpha", "beta"),
        )

        assertEquals(listOf("alpha", "beta", "zeta"), normalized)
    }
}
