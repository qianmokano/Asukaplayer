package com.asuka.player.app

import android.provider.MediaStore

internal data class GenerationBaseline(
    val addedAfterExclusive: Long,
    val modifiedAfterExclusive: Long,
)

internal fun buildSelection(
    base: String?,
    generationBaseline: GenerationBaseline?,
    modifiedSinceInclusive: Long?,
): String? {
    if (generationBaseline != null) {
        return buildString {
            appendBaseSelection(base)
            append("(")
            append(MediaStore.MediaColumns.GENERATION_ADDED)
            append("> ? OR ")
            append(MediaStore.MediaColumns.GENERATION_MODIFIED)
            append("> ?)")
        }
    }
    val modified = modifiedSinceInclusive ?: return base
    return buildString {
        appendBaseSelection(base)
        append("${MediaStore.Video.Media.DATE_MODIFIED}>=?")
    }
}

internal fun buildSelectionArgs(
    generationBaseline: GenerationBaseline?,
    modifiedSinceInclusive: Long?,
): Array<String>? {
    return when {
        generationBaseline != null -> arrayOf(
            generationBaseline.addedAfterExclusive.toString(),
            generationBaseline.modifiedAfterExclusive.toString(),
        )
        modifiedSinceInclusive != null -> arrayOf(modifiedSinceInclusive.toString())
        else -> null
    }
}

internal fun buildSortOrder(generationBaseline: GenerationBaseline?): String {
    return if (generationBaseline != null) {
        "${MediaStore.MediaColumns.GENERATION_MODIFIED} ASC, ${MediaStore.Video.Media._ID} ASC"
    } else {
        "${MediaStore.Video.Media.DATE_MODIFIED} ASC, ${MediaStore.Video.Media._ID} ASC"
    }
}

private fun StringBuilder.appendBaseSelection(base: String?) {
    if (!base.isNullOrBlank()) {
        append(base)
        append(" AND ")
    }
}
