package org.danielzfranklin.librereader.epub

import androidx.compose.runtime.Immutable
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.model.BookID

@Immutable
data class Epub(
    val id: BookID,
    val maxSection: Int,
    private val getSection: (Int) -> EpubSection?,
    private val sectionLengths: List<Int>
) {
    constructor(id: BookID, epub: Book) : this(
        id,
        epub.spine.spineReferences.size - 1,
        { if (it < epub.spine.spineReferences.size) EpubSection(id, epub, it) else null },
        TODO()
    )

    /** Unit: Characters */
    private val length = sectionLengths.sum()

    private val sectionsCache = mutableMapOf<Int, EpubSection>()

    fun section(index: Int): EpubSection? {
        if (index > maxSection || index < 0) return null

        val cached = sectionsCache[index]
        if (cached != null) return cached

        val value = getSection(index) ?: return null
        sectionsCache[index] = value
        return value
    }

    fun computePercent(sectionIndex: Int, charIndex: Int): Float {
        val chars = sectionLengths.subList(0, sectionIndex).sum() + charIndex
        return chars.toFloat() / length.toFloat()
    }
}