package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

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
}

class SectionRenderer(
    val outerWidth: Dp,
    val outerHeight: Dp,
    private val padding: Dp,
    private val annotatedString: AnnotatedString,
    private val baseStyle: TextStyle,
    private val density: Density,
    private val fontLoader: Font.ResourceLoader
) {
    private val innerWidthPx = with(density) { (outerWidth - padding * 2f).toPx() }
    private val innerHeightPx = with(density) { (outerHeight - padding * 2f).toPx() }
    private val paddingPx = with(density) { padding.toPx() }

    private var measures: MultiParagraph? = null
    var pages: List<PageRenderer>? = null
        private set
    val lastPage get() = pages?.let { it.size - 1 }

    fun findPage(char: Int): PageRenderer? {
        val pages = pages ?: throw IllegalStateException("Not laid out")
        return pages.find { it.startChar <= char && char <= it.endChar }
    }

    fun layout() {
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

    val background = baseStyle.background

    /** Canvas expected to be of size width x height */
    fun paintPage(canvas: Canvas, page: PageRenderer) {
        val measures = measures ?: throw IllegalStateException("Not loaded")

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
