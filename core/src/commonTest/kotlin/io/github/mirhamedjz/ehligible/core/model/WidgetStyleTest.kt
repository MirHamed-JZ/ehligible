package io.github.mirhamedjz.ehligible.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetStyleTest {
    @Test
    fun theSystemStyleFollowsDarkMode() {
        assertEquals(WidgetColors.Light, WidgetStyle.SYSTEM.colors(night = false))
        assertEquals(WidgetColors.Dark, WidgetStyle.SYSTEM.colors(night = true))
    }

    @Test
    fun theOtherStylesIgnoreDarkMode() {
        for (night in listOf(false, true)) {
            assertEquals(WidgetColors.Light, WidgetStyle.LIGHT.colors(night))
            assertEquals(WidgetColors.Dark, WidgetStyle.DARK.colors(night))
            assertEquals(WidgetColors.Maple, WidgetStyle.MAPLE.colors(night))
        }
    }

    @Test
    fun opacityReplacesTheAlpha() {
        assertEquals(0xFFFFF8F5, 0xFFFFF8F5.withOpacity(100))
        assertEquals(0x00FFF8F5L, 0xFFFFF8F5.withOpacity(0))
        assertEquals(0x80FFF8F5L, 0xFFFFF8F5.withOpacity(50))
        assertEquals(0x00FFF8F5L, 0xFFFFF8F5.withOpacity(-5))
        assertEquals(0xFF1A1112, 0x001A1112L.withOpacity(150))
    }
}
