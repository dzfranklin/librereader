package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import timber.log.Timber

@Immutable
data class PageRenderer(
    private val sectionRenderer: SectionRenderer,
    val startChar: Int,
    val endChar: Int,
    val index: Int,
    internal val topClip: Float,
    internal val bottomClip: Float,
) {
    fun paint(canvas: Canvas) =
        sectionRenderer.paintPage(canvas, this)

    val background = sectionRenderer.background

    val lastOffset = endChar - startChar

    /** Returns the character offset (in page space) closest to the given graphical position
     *  (in page space).
     */
    fun getOffsetForPosition(position: Offset): Int {
        val globalPosition = Offset(-padding, topClip - padding) + position
        val globalOffset = sectionRenderer.measures.getOffsetForPosition(globalPosition)
        return (globalOffset - startChar).coerceIn(0, endChar)
    }

    /** Returns the TextRange (in page space) of the word at the given character offset (in page
     * space).
     */
    fun getWordBoundary(offset: Int): TextRange {
        val globalOffset = startChar + offset
        val globalRange = sectionRenderer.measures.getWordBoundary(globalOffset)
        return TextRange(
            (globalRange.start - startChar).coerceIn(0, lastOffset),
            (globalRange.end - startChar).coerceIn(0, lastOffset)
        )
    }

    /** Get the text direction of the character at the given offset (in page space) */
    fun getBidiRunDirection(offset: Int) =
        sectionRenderer.measures.getBidiRunDirection(startChar + offset)

    /** Returns path (in page space) that enclose the given text range (in page space). */
    fun getPathForRange(start: Int, end: Int): Path {
        val path = sectionRenderer.measures.getPathForRange(startChar + start, startChar + end)
        path.translate(Offset(padding, -topClip + padding))
        return path
    }

    /** Returns the bounding box (in page space) as Rect of the character for given character offset
     * (in page space). Rect includes the top, bottom, left and right of a character.
     */
    fun getBoundingBox(offset: Int): Rect {
        val globalRect = sectionRenderer.measures.getBoundingBox(startChar + offset)
        return globalRect.translate(Offset(padding, -topClip + padding))
    }

    fun getText(firstChar: Int, lastChar: Int) =
        sectionRenderer.annotatedString.subSequence(firstChar, lastChar)

    private val padding = sectionRenderer.paddingPx
}

@Immutable
class SectionRenderer(
    val outerWidth: Dp,
    val outerHeight: Dp,
    private val padding: Dp,
    internal val annotatedString: AnnotatedString,
    baseStyle: TextStyle,
    density: Density,
    fontLoader: Font.ResourceLoader
) {
    val background = baseStyle.background

    private val outerWidthPx = with(density) { outerWidth.toPx() }
    private val outerHeightPx = with(density) { outerHeight.toPx() }
    internal val paddingPx = with(density) { padding.toPx() }

    private val innerWidthPx = outerWidthPx - paddingPx * 2f
    private val innerHeightPx = outerHeightPx - paddingPx * 2f

    internal var measures: MultiParagraph
    val pages: List<PageRenderer>
    val lastPage: Int

    init {
        val measures = MultiParagraph(
            annotatedString,
            baseStyle,
            emptyList(),
            Int.MAX_VALUE,
            false,
            innerWidthPx,
            density,
            fontLoader
        )

        val pages = mutableListOf<PageRenderer>()

        var currentTop = 0f
        var currentBottom = 0f
        for (line in 0 until measures.lineCount) {
            val nextBottom = measures.getLineBottom(line)
            val prev = pages.lastOrNull()

            if ((nextBottom - currentTop) > innerHeightPx) {
                val lastLineHeight = measures.getLineHeight(line)
                pages.add(
                    makeMeasuredPage(
                        measures,
                        prev,
                        currentTop,
                        currentBottom,
                        lastLineHeight
                    )
                )
                currentTop = measures.getLineTop(line)
                currentBottom = currentTop
            } else {
                currentBottom = nextBottom
            }
        }

        if (currentBottom != currentTop) {
            val lastLineHeight = measures.getLineHeight(measures.lineCount - 1)
            pages.add(
                makeMeasuredPage(
                    measures,
                    pages.lastOrNull(),
                    currentTop,
                    currentBottom,
                    lastLineHeight
                )
            )
        }

        this.measures = measures
        this.pages = pages
        lastPage = pages.size - 1
    }

    private fun makeMeasuredPage(
        measures: MultiParagraph,
        prev: PageRenderer?,
        top: Float,
        bottom: Float,
        lastLineHeight: Float
    ): PageRenderer {
        val endCharPosition = Offset(innerWidthPx, bottom - lastLineHeight / 2f)
        return PageRenderer(
            this,
            index = prev?.let { it.index + 1 } ?: 0,
            startChar = prev?.let { it.endChar + 1 } ?: 0,
            endChar = measures.getOffsetForPosition(endCharPosition),
            topClip = top,
            bottomClip = bottom,
        )
    }

    fun findPage(char: Int): PageRenderer? {
        val pages = pages
        return pages.find { it.startChar <= char && char <= it.endChar }
    }

    /** Canvas expected to be of size width x height */
    fun paintPage(canvas: Canvas, page: PageRenderer) {
        val measures = measures

        canvas.save()
        canvas.translate(paddingPx, -page.topClip + paddingPx)
        canvas.clipRect(
            Rect(
                Offset(0f, page.topClip),
                Size(innerWidthPx, page.bottomClip - page.topClip)
            )
        )

        measures.paint(canvas)

        canvas.restore()
    }
}
