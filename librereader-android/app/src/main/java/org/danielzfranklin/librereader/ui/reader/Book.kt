package org.danielzfranklin.librereader.ui.reader

import kotlinx.coroutines.CoroutineScope
import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class Book(
    source: InputStream,
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    private val book = EpubReader().readEpub(source)

    val title = book.title ?: "Untitled"

    val sections = book.spine.spineReferences
        .map { BookSection.from(book, it) }

    val textLength = sections.sumBy { it.textLength }
}