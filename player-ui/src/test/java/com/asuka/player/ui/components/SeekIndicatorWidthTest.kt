package com.asuka.player.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SeekIndicatorWidthTest {

    @Test
    fun seekTimeHudMeasureText_usesDurationTemplateForBothSides() {
        assertEquals(
            "88:88 / 88:88",
            seekTimeHudMeasureText(120_000L),
        )
    }

    @Test
    fun seekTimeHudMeasureText_keepsStableWidthForWholeSeekSession() {
        assertEquals(
            "88:88:88 / 88:88:88",
            seekTimeHudMeasureText(43_200_000L),
        )
    }

    @Test
    fun seekTimeHudMeasureText_usesFallbackTemplateWhenDurationUnknown() {
        assertEquals(
            "888:88:88 / 888:88:88",
            seekTimeHudMeasureText(0L),
        )
    }

    @Test
    fun seekTimeHudDigitTemplate_preservesSeparatorsAndSpaces() {
        assertEquals(
            "88:88:88 / 88:88:88",
            seekTimeHudDigitTemplate("01:02:03 / 12:34:56"),
        )
    }
}
