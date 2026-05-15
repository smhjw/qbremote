package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Method

class DashboardDensitySupportTest {

    @Test
    fun trimDashboardBarEntries_keepsTopThreeByValue() {
        val entries = listOf(
            DashboardBarSeedEntry(
                label = LegendLabelSpec.Raw("JP"),
                value = 120L,
                valueKind = LegendValueKind.BYTES,
            ),
            DashboardBarSeedEntry(
                label = LegendLabelSpec.Raw("US"),
                value = 950L,
                valueKind = LegendValueKind.BYTES,
            ),
            DashboardBarSeedEntry(
                label = LegendLabelSpec.Raw("SG"),
                value = 300L,
                valueKind = LegendValueKind.BYTES,
            ),
            DashboardBarSeedEntry(
                label = LegendLabelSpec.Raw("DE"),
                value = 700L,
                valueKind = LegendValueKind.BYTES,
            ),
        )

        val trimmed = invokeTrimDashboardBarEntries(entries, maxEntries = 3)

        assertEquals(listOf(950L, 700L, 300L), trimmed.map { it.value })
        assertEquals(
            listOf("US", "DE", "SG"),
            trimmed.map { (it.label as LegendLabelSpec.Raw).text },
        )
    }

    @Test
    fun trimDashboardBarEntries_limitsResultSizeToMaxEntries() {
        val entries = listOf(
            DashboardBarSeedEntry(LegendLabelSpec.Raw("A"), 10L, LegendValueKind.BYTES),
            DashboardBarSeedEntry(LegendLabelSpec.Raw("B"), 20L, LegendValueKind.BYTES),
            DashboardBarSeedEntry(LegendLabelSpec.Raw("C"), 30L, LegendValueKind.BYTES),
            DashboardBarSeedEntry(LegendLabelSpec.Raw("D"), 40L, LegendValueKind.BYTES),
            DashboardBarSeedEntry(LegendLabelSpec.Raw("E"), 50L, LegendValueKind.BYTES),
        )

        val trimmed = invokeTrimDashboardBarEntries(entries, maxEntries = 3)

        assertEquals(3, trimmed.size)
        assertEquals(listOf(50L, 40L, 30L), trimmed.map { it.value })
    }

    private fun invokeTrimDashboardBarEntries(
        entries: List<DashboardBarSeedEntry>,
        maxEntries: Int,
    ): List<DashboardBarSeedEntry> {
        val method = findRequiredStaticMethod(
            containerClassName = "com.hjw.qbremote.ui.DashboardComponentsKt",
            functionName = "trimDashboardBarEntries",
            parameterTypes = arrayOf(
                List::class.java,
                Int::class.javaPrimitiveType ?: Int::class.java,
            ),
        )
        val result = method.invoke(null, entries, maxEntries)
            ?: error("`trimDashboardBarEntries` returned null.")
        @Suppress("UNCHECKED_CAST")
        return result as? List<DashboardBarSeedEntry>
            ?: error("`trimDashboardBarEntries` should return List<DashboardBarSeedEntry>.")
    }

    private fun findRequiredStaticMethod(
        containerClassName: String,
        functionName: String,
        parameterTypes: Array<Class<*>>,
    ): Method {
        val container = Class.forName(containerClassName)
        return runCatching {
            container.getDeclaredMethod(functionName, *parameterTypes)
        }.getOrElse {
            val signature = parameterTypes.joinToString(", ") { it.simpleName }
            error("Expected `${container.simpleName}.$functionName($signature)` as planned top-level helper.")
        }.also { it.isAccessible = true }
    }
}
