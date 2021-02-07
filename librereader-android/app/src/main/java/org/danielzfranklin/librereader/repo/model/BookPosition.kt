package org.danielzfranklin.librereader.repo.model

import android.text.Spanned
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import kotlin.math.floor

data class BookPosition(val id: BookID, val sectionIndex: Int, val charIndex: Int) {
    fun movedBy(display: BookDisplay, deltaPages: Int): BookPosition? {
        var newPage = pageIndex(display) + deltaPages
        var newSectionIndex = sectionIndex

        while (newPage < 0 || newPage > display.sections[newSectionIndex].pages.size - 1) {
            if (newPage < 0) {
                newSectionIndex--

                if (newSectionIndex < 0) {
                    return null
                }

                newPage += display.sections[newSectionIndex].pages.size
            } else {
                newSectionIndex++

                if (newSectionIndex > display.sections.size - 1) {
                    return null
                }

                newPage -= display.sections[newSectionIndex - 1].pages.size
            }
        }

        val newCharIndex = display.sections[newSectionIndex].pages
            .subList(0, newPage).sumBy { it.length }

        return BookPosition(id, newSectionIndex, newCharIndex)
    }

    fun page(display: BookDisplay): Spanned {
        var runningIndex = 0

        for (page in display.sections[sectionIndex].pages) {
            runningIndex += page.length
            if (charIndex < runningIndex) {
                return page
            }
        }

        throw IllegalArgumentException(
            "Unreachable: charIndex $charIndex outside of section $sectionIndex text"
        )
    }

    fun pageIndex(display: BookDisplay): Int {
        var runningIndex = 0

        for ((pageIndex, page) in display.sections[sectionIndex].pages.withIndex()) {
            runningIndex += page.length
            if (charIndex < runningIndex) {
                return pageIndex
            }
        }

        throw IllegalArgumentException(
            "Unreachable: charIndex $charIndex outside of section $sectionIndex text"
        )
    }

    fun toPercent(book: BookDisplay): Float {
        val lengthToSection =
            (0 until sectionIndex).sumBy { book.sections[it].textLength }
        val lengthToPosition = lengthToSection + charIndex + 1
        return lengthToPosition.toFloat() / book.textLength.toFloat()
    }

    companion object {
        fun fromPercent(book: BookDisplay, percent: Float): BookPosition {
            if (percent < 0f || percent > 1f) {
                throw IllegalStateException("Invalid percent $percent")
            }

            val targetLength = floor(book.textLength.toFloat() * percent).toInt()

            var runningLength = 0
            for ((sectionIndex, section) in book.sections.withIndex()) {
                if (runningLength + section.textLength >= targetLength) {
                    val charIndex = targetLength - runningLength - 1
                    return BookPosition(book.id, sectionIndex, charIndex)
                }
                runningLength += section.textLength
            }

            throw IllegalStateException(
                "Unreachable: percent: $percent, targetLength: $targetLength"
            )
        }

        fun startOf(book: BookDisplay) = BookPosition(book.id, 0, 0)

        fun endOf(book: BookDisplay): BookPosition {
            val sectionIndex = book.sections.size - 1
            val charIndex = book.sections[sectionIndex].textLength - 1
            return BookPosition(book.id, sectionIndex, charIndex)
        }
    }
}