package org.danielzfranklin.librereader.ui.reader.displayModel

import android.content.Context
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.repo.model.BookID
import org.danielzfranklin.librereader.repo.model.BookPosition

class BookDisplay(
    private val context: Context,
    val id: BookID,
    val epub: Book,
    val pageDisplay: PageDisplay,
) {
    val title = epub.title ?: "Untitled"

    val sections = epub.spine.spineReferences
        .mapIndexed { index, _ -> BookSectionDisplay(context, this, index) }

    val textLength = sections.sumBy { it.textLength }

    fun isFirstPage(position: BookPosition) =
        position.sectionIndex == 0 && position.sectionPageIndex(this) == 0

    fun isLastPage(position: BookPosition) =
        position.sectionIndex == sections.size - 1 &&
                position.sectionPageIndex(this) == sections[position.sectionIndex].pages.size - 1
}