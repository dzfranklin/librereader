package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.danielzfranklin.librereader.epub.Epub
import org.danielzfranklin.librereader.epub.EpubSection
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.LocalRepo
import org.danielzfranklin.librereader.util.offset
import org.danielzfranklin.librereader.util.round
import org.danielzfranklin.librereader.util.size
import timber.log.Timber
import kotlin.math.ceil
import org.danielzfranklin.librereader.R

@Composable
fun ReaderScreen(bookId: BookID) {
    val repo = LocalRepo.current
    val model = viewModel(
        ReaderModel::class.java,
        key = bookId.toString(),
        factory = ReaderModel.Factory(repo, bookId)
    )

    val book: State<ReaderModel.Book?> = produceState(null) {
        Timber.i("In produceState")
        value = model.book.await()
    }

    val current = book.value
    if (current == null) {
        Box(Modifier.fillMaxSize(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.fillMaxWidth(0.5f).aspectRatio(1f))
            }
    } else {
        Pages(current.epub)
    }
}

@Preview(widthDp = 411, heightDp = 731)
@Composable
fun PagesPreview() {
    val string = stringResource(R.string.preview_section_text)
    val text = with(AnnotatedString.Builder()) {
        for ((i, para) in string.split("{{para_sep}}").withIndex()) {
            if (i == 0) {
                pushStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 15.sp, restLine = 10.sp)))
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            } else {
                pushStyle(ParagraphStyle())
                pushStyle(SpanStyle())
            }

            append(para)
            pop()
            pop()
        }
        toAnnotatedString()
    }
    val epub = Epub(0) { if (it == 0) EpubSection(text) else null }
    Pages(epub)
}

@Composable
fun Pages(epub: Epub) {
    val section = epub.section(0)!!
    val padding = 15.dp

    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize(1f)) {
        val innerOffset = with(density) {
            Offset(padding.toPx(), padding.toPx())
        }

        val rendered = renderSection(section, minWidth - padding * 2f, minHeight - padding * 2f)

        Page(rendered.bitmap, rendered.pages[1], innerOffset)
        Page(rendered.bitmap, rendered.pages[0], innerOffset, Modifier.offset((-300).dp, 0.dp))
    }
}

@Immutable
data class RenderedSection(
    val paragraphs: MultiParagraph,
    val bitmap: ImageBitmap,
    val pages: List<Rect>
)

@Composable
fun renderSection(
    section: EpubSection,
    innerWidth: Dp,
    innerHeight: Dp,
): RenderedSection {
    val density = LocalDensity.current

    val innerWidthPx: Float
    val innerHeightPx: Float
    with(density) {
        innerWidthPx = innerWidth.toPx()
        innerHeightPx = innerHeight.toPx()
    }

    val paragraphs = MultiParagraph(
        section.text,
        TextStyle(fontSize = 20.sp),
        listOf(),
        Int.MAX_VALUE,
        false,
        innerWidthPx,
        density,
        LocalFontLoader.current
    )

    val bitmap =
        ImageBitmap(
            ceil(paragraphs.width).toInt(),
            ceil(paragraphs.height).toInt(),
            ImageBitmapConfig.Argb8888
        )
    val contentsCanvas = Canvas(bitmap)
    paragraphs.paint(contentsCanvas)

    val pages = mutableListOf<Rect>()

    var pageHeight = 0f
    for (line in 0 until paragraphs.lineCount) {
        val lineHeight = paragraphs.getLineHeight(line)
        if (pageHeight + lineHeight < innerHeightPx) {
            pageHeight += lineHeight
        } else {
            val offset = pages.lastOrNull()?.bottomLeft ?: Offset(0f, 0f)
            val size = Size(innerWidthPx, pageHeight)
            pages.add(Rect(offset, size))
            pageHeight = 0f
        }
    }

    return RenderedSection(paragraphs, bitmap, pages)
}

@Composable
fun Page(
    bitmap: ImageBitmap,
    bitmapClip: Rect,
    innerOffset: Offset,
    modifier: Modifier = Modifier
) {
    // TODO: Look at CoreText TextController for how to handle selection
    Surface(modifier.fillMaxSize(1f), elevation = 15.dp) {
        BoxWithConstraints(Modifier.fillMaxSize(1f)) {
            Canvas(Modifier.fillMaxSize(1f)) {
                drawRect(Color.White)

                drawImage(
                    bitmap,
                    srcOffset = bitmapClip.offset().round(),
                    srcSize = bitmapClip.size().round(),
                    dstOffset = innerOffset.round()
                )
            }
        }
    }
}