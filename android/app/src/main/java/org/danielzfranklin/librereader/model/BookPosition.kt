package org.danielzfranklin.librereader.model

import androidx.compose.runtime.Immutable
import org.danielzfranklin.librereader.epub.Epub
import kotlin.math.round

@Immutable
data class BookPosition(
    val id: BookID,
    val percent: Float,
    val sectionIndex: Int,
    val charIndex: Int
) {
    constructor(epub: Epub, sectionIndex: Int, charIndex: Int) : this(
        epub.id,
        epub.computePercent(sectionIndex, charIndex),
        sectionIndex,
        charIndex
    )

    override fun toString(): String {
        return "BookPosition $sectionIndex/$charIndex $id (${round(percent * 100 * 100f) / 100f}%)"
    }

    companion object {
        fun startOf(epub: Epub) =
            BookPosition(epub.id, 0f, 0, 0)
    }

    operator fun compareTo(other: BookPosition) =
        when {
            /*
            left > right    left.compareTo(right) > 0
            left < right	left.compareTo(right) < 0
            left >= right	left.compareTo(right) >= 0
            left <= right	left.compareTo(right) <= 0
            */
            sectionIndex > other.sectionIndex -> 1
            sectionIndex < other.sectionIndex -> -1
            charIndex > other.charIndex -> 1
            charIndex < other.charIndex -> -1
            else -> 0
        }
}