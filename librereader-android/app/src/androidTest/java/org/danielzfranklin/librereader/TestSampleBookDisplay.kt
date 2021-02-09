package org.danielzfranklin.librereader

import android.content.Context
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.repo.model.BookID
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.ui.reader.displayModel.BookPageDisplay

class TestBookDisplay(context: Context, pageDisplay: BookPageDisplay) :
    BookDisplay(
        context,
        BookID("testbook:1"),
        loadTestEpub(), pageDisplay
    ) {

    companion object {
        private val classLoader = TestSampleBookDisplay::class.java.classLoader!!

        fun loadTestEpub(): Book {
            val stream = classLoader.getResourceAsStream("testbook1.epub")!!
            return EpubReader().readEpub(stream)
        }
    }
}