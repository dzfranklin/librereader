package org.danielzfranklin.librereader.epub

import kotlinx.coroutines.CoroutineScope
import nl.siegmann.epublib.domain.Book
import kotlin.coroutines.CoroutineContext

class Epub(val maxSection: Int, private val getSection: (Int) -> EpubSection?) {
    constructor(epub: Book) : this(
        epub.spine.spineReferences.size - 1,
        { if (it < epub.spine.spineReferences.size) EpubSection(epub, it) else null }
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