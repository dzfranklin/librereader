package org.danielzfranklin.librereader.model

import android.text.Spanned
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import kotlin.math.abs

data class BookPosition(val id: BookID, val sectionIndex: Int, val charIndex: Int) {
    fun movedBy(display: BookDisplay, deltaPages: Int): BookPosition? {
        return when {
            deltaPages == 0 -> this
            deltaPages < 0 -> movedDownBy(display, abs(deltaPages))
            else -> movedUpBy(display, deltaPages)
        }
    }

    private fun movedUpBy(display: BookDisplay, count: Int): BookPosition? {
        val section = display.sections[sectionIndex].pages()
        val currentPage = sectionPageIndex(display)

        return if (currentPage + count < section.size) {
            val charIndex = section
                .subList(0, currentPage + count)
                .sumBy { it.length }

            BookPosition(display.id, sectionIndex, charIndex)
        } else {
            if (sectionIndex >= display.sections.size - 1) {
                null
            } else {
                startOfSection(display, sectionIndex + 1)
                    .movedUpBy(display, count - (section.size - currentPage))
            }
        }
    }

    private fun movedDownBy(display: BookDisplay, count: Int): BookPosition? {
        val section = display.sections[sectionIndex]
        val currentPage = sectionPageIndex(display)

        return if (currentPage - count >= 0) {
            val charIndex = section.pages()
                .subList(0, currentPage - count)
                .sumBy { it.length }

            BookPosition(display.id, sectionIndex, charIndex)
        } else {
            if (sectionIndex <= 0) {
                null
            } else {
                endOfSection(display, sectionIndex - 1)
                    .movedDownBy(display, count - 1 - currentPage)
            }
        }
    }

    fun page(display: BookDisplay): Spanned {
        var runningIndex = 0

        for (page in display.sections[sectionIndex].pages()) {
            runningIndex += page.length
            if (charIndex < runningIndex) {
                return page
            }
        }

        throw IllegalArgumentException(
            "Unreachable: charIndex $charIndex outside of section $sectionIndex text"
        )
    }

    fun sectionPageIndex(display: BookDisplay): Int {
        var runningIndex = -1
        for ((pageIndex, page) in display.sections[sectionIndex].pages().withIndex()) {
            runningIndex += page.length
            if (charIndex <= runningIndex) {
                return pageIndex
            }
        }

        throw IllegalArgumentException(
            "Unreachable: charIndex $charIndex outside of section $sectionIndex text"
        )
    }

    fun pageIndex(display: BookDisplay): Int {
        var pageIndex = display.sections.subList(0, sectionIndex)
            .sumBy { it.pages().size }

        var firstCharOnNextPage = 0
        for (page in display.sections[sectionIndex].pages()) {
            firstCharOnNextPage += page.length
            if (charIndex < firstCharOnNextPage) {
                return pageIndex
            }
            pageIndex++
        }

        throw IllegalStateException("Position outside of book")
    }

    override fun toString(): String {
        return "BookPosition $sectionIndex/$charIndex $id"
    }

    companion object {
        /**
         * @param pageIndex Relative to the start of the book
         */
        fun fromPageIndex(book: BookDisplay, pageIndex: Int): BookPosition? {
            if (pageIndex == 0) {
                return startOf(book)
            }

            var runningIndex = 0
            for ((sectionIndex, section) in book.sections.withIndex()) {
                val sectionPages = section.pages()
                if (runningIndex + sectionPages.size - 1 >= pageIndex) {
                    val charIndex =
                        sectionPages.subList(0, pageIndex - runningIndex).sumBy { it.length }
                    return BookPosition(book.id, sectionIndex, charIndex)
                }
                runningIndex += sectionPages.size
            }

            return null
        }

        fun startOf(book: BookDisplay) = BookPosition(book.id, 0, 0)

        fun endOf(book: BookDisplay): BookPosition {
            val sectionIndex = book.sections.size - 1
            val charIndex = book.sections[sectionIndex].textLength - 1
            return BookPosition(book.id, sectionIndex, charIndex)
        }

        fun startOfSection(book: BookDisplay, sectionIndex: Int): BookPosition {
            return BookPosition(book.id, sectionIndex, 0)
        }

        fun endOfSection(book: BookDisplay, sectionIndex: Int): BookPosition {
            val charIndex = book.sections[sectionIndex].textLength - 1
            return BookPosition(book.id, sectionIndex, charIndex)
        }
    }
}