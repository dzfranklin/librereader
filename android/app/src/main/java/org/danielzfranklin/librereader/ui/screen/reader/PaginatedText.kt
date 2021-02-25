package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.danielzfranklin.librereader.R
import timber.log.Timber
import kotlin.math.floor
import kotlin.math.roundToInt

@Immutable
data class PaginatedTextPosition(val section: Int, val charIndex: Int)

@Composable
fun PaginatedText(
    initialPosition: PaginatedTextPosition,
    makeSection: (Int) -> AnnotatedString,
    maxSection: Int,
    padding: Dp = 15.dp,
) {

}

@Immutable
private data class PagePosition(val section: Int, val page: Float)

@Preview(device = Devices.PIXEL_3)
@Composable
private fun PaginatedSectionsPreview() {
    val text = rememberAnnotatedStringPreview()

    val targetPosition = remember { mutableStateOf(PagePosition(0, 0f)) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val renderer = rememberRenderer(
            outerWidth = minWidth,
            outerHeight = minHeight,
            padding = 15.dp,
            baseStyle = TextStyle(fontSize = 22.sp),
            density = LocalDensity.current,
            fontLoader = LocalFontLoader.current,
            maxSection = 2,
            makeSection = { text }
        )

        PaginatedSections(targetPosition.value, renderer)

        Column(
            Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth()
                .alpha(0.7f)
        ) {
            Text(
                targetPosition.value.toString(),
                Modifier
                    .background(Color.Gray)
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally),
                color = Color.White
            )

            Row(
                Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    {
                        val prev = targetPosition.value
                        var target = prev.copy(page = prev.page - 1f)
                        if (target.page < 0) {
                            target = PagePosition(target.section - 1, 0f)
                        }
                        targetPosition.value = target
                    },
                    Modifier.padding(horizontal = 5.dp)
                ) {
                    Text("Prev")
                }

                Button(
                    {
                        val prev = targetPosition.value
                        var target = prev.copy(page = prev.page + 1f)
                        val section = renderer[target.section]
                        if (section != null) {
                            if (target.page > section.lastPage!!) {
                                target = PagePosition(target.section + 1, 0f)
                            }
                            targetPosition.value = target
                        }
                    },
                    Modifier.padding(horizontal = 5.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}


@Composable
private fun PaginatedSections(position: PagePosition, renderer: Renderer) {
    val currentSection = renderer[position.section]
        ?: throw IllegalArgumentException("Position $position does not exist")
    val nextSection = renderer[position.section + 1]

    // If last page in the section is partly turned
    if (nextSection != null && position.page > currentSection.lastPage!!) {
        // ... show the first page of the next section behind it
        PaginatedSection(nextSection, 0f)
    }

    PaginatedSection(currentSection, position.page)
}

/**
 *  @param position 1.7 means current page is 1, and we're 70% turned to 2
 */
@Composable
private fun PaginatedSection(renderer: SectionRenderer, position: Float) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pages = renderer.pages ?: throw IllegalArgumentException("Renderer not laid out")

        require(renderer.outerWidth == minWidth)
        require(renderer.outerHeight == minHeight)

        /*  Turn forwards:
              position = 1      position = 1.3
            |111111111|      |1111111|222|
            |111111111|  =>  |1111111|222|
            |111111111|      |1111111|222|

            Turn backwards:
             position = 2      position = 1.3
            |222222222|      |1111111|222|
            |222222222|  =>  |1111111|222|
            |222222222|      |1111111|222|

            Turn backwards:
             position = 2      position = 1.7
            |222222222|      |111|2222222|
            |222222222|  =>  |111|2222222|
            |222222222|      |111|2222222|
         */

        val index = floor(position).roundToInt()

        val effectivePageWidth = minWidth + 10.dp // add a little extra to account for shadow
        val turnPercent = position - index
        val turn = effectivePageWidth * turnPercent

        if (turnPercent != 0f) {
            PageCanvas(pages.getOrNull(index + 1))
        }

        PageCanvas(pages.getOrNull(index), turn)
    }
}

@Composable
private fun PageCanvas(page: PageRenderer?, turn: Dp = 0.dp) {
    Surface(
        Modifier
            .fillMaxSize()
            .background(page?.background ?: Color.Transparent)
            .offset {
                IntOffset(
                    -turn
                        .toPx()
                        .roundToInt(), 0
                )
            }
            .drawWithContent {
                drawContent()
                drawIntoCanvas {
                    Timber.i("Drawing canvas")
                    page?.paint(it)
                }
            },
        elevation = 15.dp
    ) {}
}

@Composable
private fun rememberAnnotatedStringPreview(): AnnotatedString {
    val string = stringResource(R.string.preview_section_text)
    return remember {
        with(AnnotatedString.Builder()) {
            for (para in string.split("{{para_sep}}")) {
                pushStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp)))
                append(para)
                pop()
            }
            toAnnotatedString()
        }
    }
}
