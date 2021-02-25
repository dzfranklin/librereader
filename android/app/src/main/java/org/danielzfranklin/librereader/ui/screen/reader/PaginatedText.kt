package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import org.danielzfranklin.librereader.R
import timber.log.Timber
import kotlin.math.*

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

@Preview(device = Devices.PIXEL_2)
@Composable
fun PaginatedTextSectionPreview() =
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val string = stringResource(R.string.preview_section_text)
        val annotatedString = remember {
            with(AnnotatedString.Builder()) {
                for (para in string.split("{{para_sep}}")) {
                    pushStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp)))
                    append(para)
                    pop()
                }
                toAnnotatedString()
            }
        }

        val renderer = rememberRenderer(
            minWidth,
            minHeight,
            15.dp,
            annotatedString,
            TextStyle(fontSize = 22.sp),
            LocalDensity.current,
            LocalFontLoader.current
        )
        renderer.layout() // TODO: Off render thread
        val maxPosition = renderer.pages!!.size.toFloat()

        val animatedPosition = remember { mutableStateOf(0f) }
        val targetPosition = remember { mutableStateOf(animatedPosition.value) }

        if (targetPosition.value != animatedPosition.value) {
            LaunchedEffect(targetPosition) {
                animate(
                    initialValue = animatedPosition.value,
                    targetValue = targetPosition.value,
                    animationSpec = TweenSpec(1000)
                ) { value, _ ->
                    animatedPosition.value = value
                }
            }
        }

        PaginatedTextSection(renderer, animatedPosition.value)

        Row(Modifier.padding(vertical = 5.dp).align(Alignment.Center).alpha(0.7f)) {
            Button(
                { targetPosition.value = max(0f, round(targetPosition.value - 1)) },
                Modifier.padding(horizontal = 5.dp)
            ) {
                Text("Prev")
            }

            Button(
                { targetPosition.value = min(maxPosition, round(targetPosition.value + 1)) },
                Modifier.padding(horizontal = 5.dp)
            ) {
                Text("Next")
            }
        }
    }

/**
 *  @param position 1.7 means current page is 1, and we're 70% turned to 2
 */
@Composable
fun PaginatedTextSection(renderer: SectionRenderer, position: Float) {
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
fun PageCanvas(page: PageRenderer?, turn: Dp = 0.dp) {
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
            },
        elevation = 15.dp
    ) {
        Canvas(
            Modifier
                .fillMaxSize()

        ) {
            drawIntoCanvas {
                Timber.i("Drawing canvas")
                page?.paint(it)
            }
        }
    }
}

@Composable
fun rememberRenderer(
    outerWidth: Dp,
    outerHeight: Dp,
    padding: Dp,
    annotatedString: AnnotatedString,
    baseStyle: TextStyle,
    density: Density,
    fontLoader: Font.ResourceLoader
) = remember(annotatedString, outerWidth, outerHeight, padding, baseStyle, density, fontLoader) {
    SectionRenderer(
        outerWidth,
        outerHeight,
        padding,
        annotatedString,
        baseStyle,
        density,
        fontLoader
    )
}
