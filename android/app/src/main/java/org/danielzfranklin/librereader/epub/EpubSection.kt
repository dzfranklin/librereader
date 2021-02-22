package org.danielzfranklin.librereader.epub

import androidx.compose.ui.text.AnnotatedString
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.ResourceReference
import org.jsoup.Jsoup
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException

class EpubSection(val text: AnnotatedString) {
    constructor(epub: Book, index: Int) : this(parseFrom(epub.spine.spineReferences[index]))

    companion object {
        private fun parseFrom(ref: ResourceReference): AnnotatedString {
            val res = ref.resource
            val htmlBytes = ByteBuffer.wrap(res.data)
            val html = findCharset(res.inputEncoding).decode(htmlBytes).toString()
            val doc = Jsoup.parse(html)
            // TODO: Parse doc for styles
            return AnnotatedString(doc.text())
        }

        private fun findCharset(name: String?): Charset {
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