package org.danielzfranklin.librereader.epub

import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.model.BookID

class Epub(val id: BookID, val maxSection: Int, private val getSection: (Int) -> EpubSection?) {
    constructor(id: BookID, epub: Book) : this(
        id,
        epub.spine.spineReferences.size - 1,
        { if (it < epub.spine.spineReferences.size) EpubSection(id, epub, it) else null }
    )

    private val sections = mutableMapOf<Int, EpubSection>()

    fun section(index: Int): EpubSection? {
        if (index > maxSection || index < 0) return null

        val cached = sections[index]
        if (cached != null) return cached

        val value = getSection(index) ?: return null
        sections[index] = value
        return value
    }
}