package org.danielzfranklin.librereader.ui.reader

import android.text.Spanned
import kotlin.math.floor

sealed class BookPosition {
    data class Position(val sectionIndex: Int, val charIndex: Int) : BookPosition() {
        fun closestPageIndex(pages: List<Spanned>): Int {
            var runningIndex = 0

            for ((pageIndex, page) in pages.withIndex()) {
                if (runningIndex >= charIndex) {
                    return pageIndex
                }

                runningIndex += page.length
            }

            throw IllegalArgumentException(
                "Unreachable: charIndex $charIndex outside of section $sectionIndex text"
            )
        }

        fun toPercent(book: Book): Float {
            val lengthToSection =
                (0 until sectionIndex).sumBy { book.sections[it].textLength }
            val lengthToPosition = lengthToSection + charIndex + 1
            return lengthToPosition.toFloat() / book.textLength.toFloat()
        }

        companion object {
            fun fromPercent(book: Book, percent: Float): Position {
                if (percent < 0f || percent > 1f) {
                    throw IllegalStateException("Invalid percent $percent")
                }

                val targetLength = floor(book.textLength.toFloat() * percent).toInt()

                val runningLength = 0
                for ((sectionIndex, section) in book.sections.withIndex()) {
                    if (runningLength + section.textLength >= targetLength) {
                        val charIndex = targetLength - runningLength
                        return Position(sectionIndex, charIndex)
                    }
                }

                throw IllegalStateException(
                    "Unreachable: percent: $percent, targetLength: $targetLength"
                )
            }
        }
    }

    object End : BookPosition()
}
