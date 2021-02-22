package org.danielzfranklin.librereader.epub

import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Html
import android.text.Spanned
import androidx.compose.ui.text.AnnotatedString
import nl.siegmann.epublib.domain.Resources
import nl.siegmann.epublib.domain.SpineReference
import org.jsoup.Jsoup
import org.xml.sax.XMLReader
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException

class EpubSection(
    private val epub: Epub,
    private val ref: SpineReference,
) {
    val text: AnnotatedString

    init {
        val res = ref.resource
        val htmlBytes = ByteBuffer.wrap(res.data)
        val html = findCharset(res.inputEncoding).decode(htmlBytes).toString()
        val doc = Jsoup.parse(html)
        // TODO: Parse doc for styles
        text = AnnotatedString(doc.text())
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