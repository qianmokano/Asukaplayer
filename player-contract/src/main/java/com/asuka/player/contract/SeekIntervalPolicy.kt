package com.asuka.player.contract

object SeekIntervalPolicy {
    const val DEFAULT_SECONDS = 10
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 60

    fun normalizeSeconds(seconds: Int): Int {
        return seconds.takeIf { it in MIN_SECONDS..MAX_SECONDS } ?: DEFAULT_SECONDS
    }

    fun toMilliseconds(seconds: Int): Long {
        return normalizeSeconds(seconds) * 1_000L
    }
}
