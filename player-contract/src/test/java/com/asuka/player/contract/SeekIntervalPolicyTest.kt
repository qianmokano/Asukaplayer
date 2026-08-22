package com.asuka.player.contract

import kotlin.test.Test
import kotlin.test.assertEquals

class SeekIntervalPolicyTest {
    @Test
    fun normalizeSeconds_acceptsSupportedValues() {
        assertEquals(SeekIntervalPolicy.MIN_SECONDS, SeekIntervalPolicy.normalizeSeconds(1))
        assertEquals(30, SeekIntervalPolicy.normalizeSeconds(30))
        assertEquals(SeekIntervalPolicy.MAX_SECONDS, SeekIntervalPolicy.normalizeSeconds(60))
    }

    @Test
    fun normalizeSeconds_invalidValuesFallBackToDefault() {
        assertEquals(SeekIntervalPolicy.DEFAULT_SECONDS, SeekIntervalPolicy.normalizeSeconds(0))
        assertEquals(SeekIntervalPolicy.DEFAULT_SECONDS, SeekIntervalPolicy.normalizeSeconds(61))
        assertEquals(SeekIntervalPolicy.DEFAULT_SECONDS, SeekIntervalPolicy.normalizeSeconds(Int.MIN_VALUE))
    }

    @Test
    fun toMilliseconds_normalizesBeforeConverting() {
        assertEquals(15_000L, SeekIntervalPolicy.toMilliseconds(15))
        assertEquals(10_000L, SeekIntervalPolicy.toMilliseconds(-1))
    }
}
