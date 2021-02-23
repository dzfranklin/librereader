package org.danielzfranklin.librereader.epub

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.*
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.ResourceReference
import org.danielzfranklin.librereader.model.BookID
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException

@Immutable
data class EpubSection(val bookId: BookID, val index: Int, val text: AnnotatedString) {
    constructor(bookId: BookID, epub: Book, index: Int) : this(
        bookId,
        index,
        parseFrom(epub.spine.spineReferences[index])
    )

    companion object {
        private fun parseFrom(ref: ResourceReference): AnnotatedString {
            val res = ref.resource
            val htmlBytes = ByteBuffer.wrap(res.data)
            val html = findCharset(res.inputEncoding).decode(htmlBytes).toString()
            val doc = Jsoup.parse(html)
            // TODO: Parse doc for styles
            return AnnotatedString(doc.text())
        }

//        private data class ParseResult(
//            val spanStyles: List<SpanStyleRange>,
//            val paragraphStyles: List<ParagraphStyleRange>
//        )
//
//        private fun parse(element: Element): ParseResult {
//            element.te
//            for (child in node.childNodes()) {
//
//            }
//        }

        fun findCharset(name: String?): Charset {
            if (name == null) {
                return Charsets.UTF_8
            }

            return try {
                Charset.forName(name)
            } catch (e: IllegalCharsetNameException) {
                Timber.w("Defaulting to UTF-8 because charset `%s` not recognized", name)
                Charsets.UTF_8
            } catch (e: UnsupportedCharsetException) {
                Timber.w("Defaulting to UTF-8 because charset `%s` not supported", name)
                Charsets.UTF_8
            }
        }
    }
}