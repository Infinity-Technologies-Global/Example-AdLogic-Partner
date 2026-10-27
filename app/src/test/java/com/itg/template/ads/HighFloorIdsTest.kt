package com.itg.template.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HighFloorIdsTest {

    private val high = AdUnitConfig(id = "high", isEnable = true)
    private val normal = AdUnitConfig(id = "normal", isEnable = true)

    @Test
    fun resolve_putsHighBeforeNormal() {
        assertEquals(listOf("high", "normal"), HighFloorIds.resolve(high, normal))
    }

    @Test
    fun resolve_skipsDisabledHigh() {
        assertEquals(listOf("normal"), HighFloorIds.resolve(high.copy(isEnable = false), normal))
    }

    @Test
    fun resolve_skipsBlankHigh() {
        assertEquals(listOf("normal"), HighFloorIds.resolve(high.copy(id = " "), normal))
    }

    @Test
    fun resolve_skipsMissingHigh() {
        assertEquals(listOf("normal"), HighFloorIds.resolve(null, normal))
    }

    @Test
    fun resolve_skipsDisabledNormal() {
        assertEquals(listOf("high"), HighFloorIds.resolve(high, normal.copy(isEnable = false)))
    }

    @Test
    fun resolve_keepsSameIdOnce() {
        assertEquals(listOf("normal"), HighFloorIds.resolve(high.copy(id = "normal"), normal))
    }

    @Test
    fun resolve_returnsEmptyWhenBothDisabled() {
        assertEquals(
            emptyList<String>(),
            HighFloorIds.resolve(high.copy(isEnable = false), normal.copy(isEnable = false))
        )
    }

    @Test
    fun idOf_returnsNullForDisabledUnit() {
        assertNull(HighFloorIds.idOf(high.copy(isEnable = false)))
    }
}
