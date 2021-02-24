package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import androidx.core.util.lruCache
import org.danielzfranklin.librereader.epub.EpubSection
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.util.round
import kotlin.math.round
import kotlin.math.roundToInt

@Immutable
data class MeasuredPage(
    val start: BookPosition,
    val end: BookPosition,
    val sectionIndex: Int,
    val pageIndex: Int,
    val isLastPageOfSection: Boolean,
    internal val charLength: Int,
    /** Must not be empty (Will contain empty paragraph instead) */
    internal val paras: List<MeasuredParagraph>,
    /** Pixels of height to skip at the top of the first paragraph */
    internal val topClip: Float,
    /** Pixels of height to cut at the bottom of the last paragraph */
    internal val bottomClip: Float,
    /** The next page starts at lastParaLastLineIncluded + 1 */
    internal val lastParaLastLineIncluded: Int
)

@Immutable
data class MeasuredParagraph(
    internal val index: Int,
    internal val value: Paragraph,
)

class SectionRenderer(
    innerWidth: Dp,
    innerHeight: Dp,
    padding: Dp,
    private val section: EpubSection,
    private val makePosition: (charIndex: Int) -> BookPosition,
    private val baseStyle: TextStyle,
    private val density: Density,
    private val fontLoader: Font.ResourceLoader,
) {
    private val innerWidthPx: Float
    private val innerHeightPx: Float
    private val paddingPx: Float

    init {
        with(density) {
            innerWidthPx = innerWidth.toPx()
            innerHeightPx = innerHeight.toPx()
            paddingPx = padding.toPx()
        }
    }

    private fun paraExists(index: Int) =
        0 <= index && index <= section.text.paragraphStyles.size - 1

    private val parasCache = mutableMapOf<Int, MeasuredParagraph>()
    private fun para(index: Int): MeasuredParagraph? {
        if (!paraExists(index)) return null

        val cached = parasCache[index]
        if (cached != null) return cached

        val paraStyle = section.text.paragraphStyles[index]
        val intrinsics = ParagraphIntrinsics(
            text = section.text.substring(paraStyle.start, paraStyle.end),
            style = baseStyle.merge(paraStyle.item),
            spanStyles = section.text.spanStyles, // TODO: These use indexes, which don't work when applied to different text starting point
            placeholders = emptyList(),
            density = density,
            resourceLoader = fontLoader
        )
        val para = MeasuredParagraph(
            index,
            Paragraph(intrinsics, Int.MAX_VALUE, false, innerWidthPx)
        )

        parasCache[index] = para
        return para
    }

    private val pages = mutableListOf<MeasuredPage>()

    fun computePage(index: Int): MeasuredPage? {
        if (index < 0) return null

        val cached = pages.getOrNull(index)
        if (cached != null) return cached

        val start: BookPosition
        var charLength = 0

        var para: MeasuredParagraph
        var nextLine = 0
        var topClip = 0f

        if (index == 0) {
            // ensure each section has at least one page
            para = para(0) ?: MeasuredParagraph(-1, emptyParagraph())
            start = makePosition(0)
        } else {
            val prevPage = computePage(index - 1)!!
            start = makePosition(prevPage.start.charIndex + prevPage.charLength)

            val lastParaOnPrevPage = prevPage.paras.last()
            if (prevPage.lastParaLastLineIncluded == lastParaOnPrevPage.value.lineCount - 1) {
                // Bail if there are no paragraphs left in the section
                para = para(lastParaOnPrevPage.index + 1) ?: return null
            } else {
                para = lastParaOnPrevPage
                nextLine = prevPage.lastParaLastLineIncluded + 1
                topClip = para.value.getLineTop(nextLine)
            }
        }

        val lastLineIncluded: Int
        val bottomClip: Float

        var runningHeight = 0f
        val includedParas = mutableListOf<MeasuredParagraph>()
        while (true) {
            if (nextLine <= para.value.lineCount - 1) {
                val lineHeight = para.value.getLineHeight(nextLine)

                if (runningHeight + lineHeight > innerHeightPx) {
                    includedParas.add(para)
                    lastLineIncluded = nextLine - 1
                    bottomClip = para.value.height - para.value.getLineBottom(lastLineIncluded)
                    break
                }

                runningHeight += lineHeight
                nextLine++

                val lastCharOnLinePosition = Offset(innerWidthPx, runningHeight - lineHeight / 2f)
                charLength += para.value.getOffsetForPosition(lastCharOnLinePosition)
            } else {
                includedParas.add(para)

                val nextPara = para(para.index + 1)
                if (nextPara == null) {
                    // bail if we have no more content in the section
                    lastLineIncluded = nextLine - 1
                    bottomClip = 0f
                    break
                }

                para = nextPara
                nextLine = 0
            }
        }

        val isEndOfSection =
            lastLineIncluded == para.value.lineCount - 1 && !paraExists(para.index + 1)

        val end = makePosition(start.charIndex + charLength)

        val result = MeasuredPage(
            start,
            end,
            section.index,
            index,
            isEndOfSection,
            charLength,
            includedParas,
            topClip,
            bottomClip,
            lastLineIncluded
        )
        pages.add(index, result)

        return result
    }

    private val bitmapCache = lruCache<Int, ImageBitmap>(
        maxSize = (innerHeightPx * innerWidthPx * BITMAP_CACHE_SIZE.toFloat()).roundToInt(),
        sizeOf = { _, v -> v.height * v.width }
    )

    private fun bitmap(para: MeasuredParagraph): ImageBitmap {
        val cached = bitmapCache.get(para.index)
        if (cached != null) return cached

        val bitmap =
            ImageBitmap(
                round(innerWidthPx).toInt(),
                round(para.value.height).toInt(),
                ImageBitmapConfig.Argb8888
            )
        val canvas = Canvas(bitmap)
        para.value.paint(canvas)

        bitmapCache.put(para.index, bitmap)
        return bitmap
    }

    /** Canvas expected to be of size innerWidth + 2 * padding, innerHeight + 2 * padding */
    fun paintPage(canvas: Canvas, page: MeasuredPage) {
        var dstOffset = IntOffset(paddingPx.roundToInt(), paddingPx.roundToInt())
        val size = Size(innerWidthPx, innerHeightPx).round()

        for ((index, para) in page.paras.withIndex()) {
            val bitmap = bitmap(para)

            var srcOffset = IntOffset(0, 0)
            var heightUsed = para.value.height
            if (index == 0) {
                srcOffset = IntOffset(0, page.topClip.roundToInt())
                heightUsed -= page.topClip
            }

            if (index == page.paras.size - 1) {
                canvas.save()
                canvas.clipRect(
                    Rect(
                        dstOffset.toOffset(),
                        Size(innerWidthPx, para.value.height - page.bottomClip)
                    )
                )
            }

            canvas.drawImageRect(
                bitmap,
                srcOffset = srcOffset,
                srcSize = size,
                dstOffset = dstOffset,
                dstSize = size,
                paint = imagePaint
            )

            if (index == page.paras.size - 1) {
                canvas.restore()
            }

            dstOffset += IntOffset(0, heightUsed.roundToInt())
        }
    }

    private fun emptyParagraph(): Paragraph {
        val intrinsics = ParagraphIntrinsics(
            text = "",
            style = TextStyle(),
            spanStyles = section.text.spanStyles,
            placeholders = emptyList(),
            density = density,
            resourceLoader = fontLoader
        )
        return Paragraph(intrinsics, Int.MAX_VALUE, false, innerWidthPx)
    }

    companion object {
        private val imagePaint = Paint()

        // Cache at most enough bitmaps to cover this number of pages
        private const val BITMAP_CACHE_SIZE = 10
    }
}