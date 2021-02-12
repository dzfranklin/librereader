package org.danielzfranklin.librereader.ui.reader.displayModel

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.text.*
import androidx.core.content.ContextCompat
import nl.siegmann.epublib.domain.Resources
import org.danielzfranklin.librereader.R
import org.xml.sax.XMLReader
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import kotlin.math.max
import kotlin.math.min


class BookSectionDisplay(
    private val context: Context,
    private val book: BookDisplay,
    index: Int
) {
    private val ref = book.epub.spine.spineReferences[index]

    val title
        get() = ref.resource.title ?: book.title

    private val text = computeText()

    val textLength = text.length

    private var _pages: List<Spanned>? = null
    fun pages(): List<Spanned> {
        val cached = _pages
        return if (cached != null) {
            cached
        } else {
            val computed = computePageSpans()
            _pages = computed
            computed
        }
    }

    private fun computePageSpans(): List<Spanned> {
        if (book.pageDisplay.width < 0 || book.pageDisplay.height < 0) {
            return listOf(SpannedString(""))
        }

        val layout = StaticLayout.Builder
            .obtain(
                text,
                0,
                text.length,
                book.pageDisplay.style.toPaint(context),
                book.pageDisplay.width
            )
            .build()

        val pages = mutableListOf<Spanned>()

        var heightToCurrent = 0
        var currentStart = 0
        for (i in 0 until layout.lineCount) {
            val lineBottom = layout.getLineBottom(i)

            val isOverHeight = lineBottom - heightToCurrent > book.pageDisplay.height
            val isLastPage = i == layout.lineCount - 1

            if (isOverHeight) {
                val currentEnd = layout.getLineEnd(i - 1)

                pages.add(subSpan(text, currentStart, currentEnd))

                heightToCurrent = layout.getLineBottom(i - 1)
                currentStart = currentEnd
            } else if (isOverHeight || isLastPage) {
                val currentEnd = layout.getLineEnd(i)

                pages.add(subSpan(text, currentStart, currentEnd))

                heightToCurrent = lineBottom
                currentStart = currentEnd
            }
        }

        return pages.toList()
    }

    private fun subSpan(span: Spanned, startIndex: Int, endIndex: Int): Spanned {
        val chars = span.subSequence(startIndex, endIndex)
        val spansToCopy = span.getSpans(startIndex, endIndex, Object::class.java)

        val out = SpannableStringBuilder(chars)
        for (i in spansToCopy) {
            val start = max(0, span.getSpanStart(i) - startIndex)
            val end = min(endIndex - startIndex, span.getSpanEnd(i) - startIndex)
            out.setSpan(span, start, end, span.getSpanFlags(i))
        }

        return out
    }

    private fun computeText(): Spanned {
        val res = ref.resource

        val htmlBytes = ByteBuffer.wrap(res.data)
        val html = findCharset(res.inputEncoding).decode(htmlBytes).toString()

        return Html.fromHtml(
            html,
            0,
            ImageGetter(
                context,
                book.epub.resources,
                book.pageDisplay.width,
                book.pageDisplay.height
            ),
            TagHandler()
        )
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

    private class ImageGetter(
        private val context: Context,
        private val resources: Resources,
        private val maxWidth: Int,
        private val maxHeight: Int
    ) : Html.ImageGetter {
        override fun getDrawable(src: String?): Drawable {
            val res = resources.getByHref(src)
                ?: return ContextCompat.getDrawable(context, R.drawable.ic_help_center)!!

            val source = ImageDecoder.createSource(ByteBuffer.wrap(res.data))
            val drawable = ImageDecoder.decodeDrawable(source)

            var width = drawable.intrinsicWidth
            var height = drawable.intrinsicHeight

            if (width > maxWidth) {
                height = (height.toFloat() * maxWidth.toFloat() / width.toFloat()).toInt()
                width = maxWidth
            }

            if (height > maxHeight) {
                width = (width.toFloat() * maxHeight.toFloat() / height.toFloat()).toInt()
                height = maxHeight
            }

            drawable.setBounds(0, 0, width, height)
            return drawable
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
