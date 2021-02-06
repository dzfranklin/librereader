package org.danielzfranklin.librereader.ui.reader

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class BookDisplay(
    private val context: Context,
    source: InputStream,
//    TODO: pageDisplayProperties: PageDisplayProperties,
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    private val book = EpubReader().readEpub(source)

    val title = book.title ?: "Untitled"

    // TODO: parse and paginate lazily
    val sections = book.spine.spineReferences
        .map { BookSectionDisplay.from(context, book, it) }

    val textLength = sections.sumBy { it.textLength }
}