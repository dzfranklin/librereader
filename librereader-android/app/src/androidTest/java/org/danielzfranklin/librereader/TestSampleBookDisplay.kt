package org.danielzfranklin.librereader

import android.content.Context
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay

class TestSampleBookDisplay(context: Context, pageDisplay: BookPageDisplay) :
    BookDisplay(
        context,
        BookID("testbook:1"),
        loadTestEpub(), pageDisplay
    ) {

    companion object {
        fun loadTestEpub(): Book {
            return EpubReader().readEpub(getResource("testbook1.epub")!!)
        }
    }
}