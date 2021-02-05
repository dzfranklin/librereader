package org.danielzfranklin.librereader.ui.reader

import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.*
import android.text.style.URLSpan
import androidx.core.text.toSpannable
import androidx.core.text.toSpanned
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.SpineReference
import org.xml.sax.XMLReader
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException


class BookSection(val title: String, private val contents: Spanned) {
    val textLength = contents.length

    data class PageDisplayProperties(val width: Int, val height: Int, val paint: TextPaint)

    fun paginate(props: PageDisplayProperties): List<Spanned> {
        if (props.width < 0 || props.height < 0) {
            return listOf(SpannedString(""))
        }

        val layout = StaticLayout.Builder
            .obtain(contents, 0, contents.length, props.paint, props.width)
            .build()

        val pages = mutableListOf<Spanned>()

        var heightToCurrent = 0
        var currentStart = 0
        for (i in 0 until layout.lineCount) {
            val lineBottom = layout.getLineBottom(i)

            val isOverHeight = lineBottom - heightToCurrent > props.height
            val isLastPage = i == layout.lineCount - 1

            if (isOverHeight) {
                val currentEnd = layout.getLineEnd(i - 1)

                pages.add(subSpan(contents, currentStart, currentEnd))

                heightToCurrent = layout.getLineBottom(i - 1)
                currentStart = currentEnd
            } else if (isOverHeight || isLastPage) {
                val currentEnd = layout.getLineEnd(i)

                pages.add(subSpan(contents, currentStart, currentEnd))

                heightToCurrent = lineBottom
                currentStart = currentEnd
            }
        }

        return pages.toList()
    }

    companion object {
        fun from(book: Book, sectionRef: SpineReference): BookSection {
            val res = sectionRef.resource

            val htmlBytes = ByteBuffer.wrap(res.data)
            val html = findCharset(res.inputEncoding).decode(htmlBytes).toString()
            val spanned = Html.fromHtml(html, 0, ImageGetter(book), TagHandler())
            val contents = processSpanned(spanned)

            return BookSection(res.title ?: book.title, contents)
        }

        private fun subSpan(span: Spanned, startIndex: Int, endIndex: Int): Spanned {
            val chars = span.subSequence(startIndex, endIndex)
            val spansToCopy = span.getSpans(startIndex, endIndex, Object::class.java)

            val out = SpannableStringBuilder(chars)
            for (i in spansToCopy) {
                out.setSpan(span, span.getSpanStart(i), span.getSpanEnd(i), span.getSpanFlags(i))
            }

            return out
        }

        private fun processSpanned(spanned: Spanned): Spanned {
            val mut = spanned.toSpannable()

            val links = mut.getSpans(0, mut.length, URLSpan::class.java)
            for (link in links) {
                val url = link.url
                val start = mut.getSpanStart(link)
                val end = mut.getSpanEnd(link)

                if (start == -1 || end == -1) {
                    throw IllegalStateException("Failed to process link: Not attached")
                }

                mut.removeSpan(link)
                mut.setSpan(BookURLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                Timber.i("Processed link for %s", url)
            }

            return mut.toSpanned()
        }

        private fun drawableFromBytes(bytes: ByteArray): Drawable {
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            return ImageDecoder.decodeDrawable(source)
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

    private class ImageGetter(private val book: Book) : Html.ImageGetter {
        override fun getDrawable(src: String?): Drawable {
            val res = book.resources.getByHref(src)
            return drawableFromBytes(res.data)
        }
    }

    private class TagHandler : Html.TagHandler {
        override fun handleTag(
            opening: Boolean,
            tag: String?,
            output: Editable?,
            xmlReader: XMLReader?
        ) {
            if (opening) {
                Timber.d("Unrecognized tag %s opened", tag)
            } else {
                Timber.d("Unrecognized tag %s closed", tag)
            }
        }

    }
}
