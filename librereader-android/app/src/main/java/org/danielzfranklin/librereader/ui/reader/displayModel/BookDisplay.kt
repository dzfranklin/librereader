package org.danielzfranklin.librereader.ui.reader.displayModel

import android.content.Context
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.repo.model.BookID

class BookDisplay(
    private val context: Context,
    val id: BookID,
    val epub: Book,
    val pageDisplay: PageDisplay,
) {
    val title = epub.title ?: "Untitled"

    // TODO: parse and paginate lazily
    val sections = epub.spine.spineReferences
        .mapIndexed { index, _ -> BookSectionDisplay(context, this, index) }

    val textLength = sections.sumBy { it.textLength }
}