package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.LocalRepo
import org.danielzfranklin.librereader.util.offset
import org.danielzfranklin.librereader.util.round
import org.danielzfranklin.librereader.util.size
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.roundToInt

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
        Pages(current)
    }
}

@Composable
fun Pages(book: ReaderModel.Book) {
    val section = book.epub.section(2)!!
    val padding = 15.dp

    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize(1f)) {
        val innerWidthPx: Float
        val innerHeightPx: Float
        val innerOffset: Offset
        with(density) {
            innerWidthPx = (minWidth - padding * 2f).toPx()
            innerHeightPx = (minHeight - padding * 2f).toPx()
            innerOffset = Offset(padding.toPx(), padding.toPx())
        }

        val contents = MultiParagraph(
            section.text,
            TextStyle(fontSize = 20.sp),
            listOf(),
            Int.MAX_VALUE,
            false,
            innerWidthPx,
            density,
            LocalFontLoader.current
        )

        val contentsBitmap =
            ImageBitmap(
                ceil(contents.width).toInt(),
                ceil(contents.height).toInt(),
                ImageBitmapConfig.Argb8888
            )
        val contentsCanvas = Canvas(contentsBitmap)
        contents.paint(contentsCanvas)

        var finalLine = 0
        var clipHeight = 0f
        var nextLineHeight = contents.getLineHeight(finalLine)

        while (finalLine < contents.lineCount - 1 && clipHeight + nextLineHeight <= innerHeightPx) {
            clipHeight += nextLineHeight
            finalLine++
            nextLineHeight = contents.getLineHeight(finalLine)
        }

        Page(
            contentsBitmap,
            Rect(Offset.Zero, Size(innerWidthPx, clipHeight)),
            innerOffset,
            Modifier.offset((-200).dp, 0.dp).zIndex(1f)
        )

        val startHeight = clipHeight
        clipHeight = 0f
        while (finalLine < contents.lineCount - 1 && clipHeight + nextLineHeight <= innerHeightPx) {
            clipHeight += nextLineHeight
            finalLine++
            nextLineHeight = contents.getLineHeight(finalLine)
        }

        Page(
            contentsBitmap,
            Rect(Offset(0f, startHeight), Size(innerWidthPx, clipHeight)),
            innerOffset,
            Modifier.zIndex(0f)
        )
    }
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