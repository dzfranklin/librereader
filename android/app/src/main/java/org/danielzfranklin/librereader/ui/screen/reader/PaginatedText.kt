package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
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

        val animPosition = rememberSectionsAnimationState(PagePosition(0, 0f), renderer)

        PaginatedSections(animPosition.position.value, renderer)

        Column(
            Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth()
                .alpha(0.7f)
        ) {
            Text(
                "${animPosition.position.value}",
                Modifier
                    .padding(2.dp)
                    .background(Color.Gray)
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
                    { animPosition.animateBy(-2, 1000) },
                    Modifier.padding(horizontal = 5.dp),
                    enabled = !animPosition.isRunning.value
                ) {
                    Text("Prev")
                }

                Button(
                    { animPosition.animateBy(2, 1000) },
                    Modifier.padding(horizontal = 5.dp),
                    enabled = !animPosition.isRunning.value
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun rememberSectionsAnimationState(
    initialPosition: PagePosition,
    renderer: Renderer
): SectionsAnimationState {
    val scope = rememberCoroutineScope()
    return remember(initialPosition, renderer) {
        SectionsAnimationState(
            initialPosition,
            renderer,
            scope.coroutineContext
        )
    }
}

private class SectionsAnimationState constructor(
    initialPosition: PagePosition,
    val renderer: Renderer,
    override val coroutineContext: CoroutineContext,
) : CoroutineScope {
    private var section = mutableStateOf(initialPosition.section)

    private var pageAnim = AnimationState(initialPosition.page)

    val position = derivedStateOf { PagePosition(section.value, pageAnim.value) }

    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> = _isRunning

    fun animateBy(delta: Int, durationMillis: Int, sequentialAnimation: Boolean = false) {
        launch {
            animateBySuspended(delta, durationMillis, sequentialAnimation)
        }
    }

    private suspend fun animateBySuspended(
        delta: Int,
        durationMillis: Int,
        sequential: Boolean = false
    ) {
        // NOTE: We take a duration instead of an AnimationSpec because we need to break the
        // animation into section-sized chunks and that makes it much simpler.

        _isRunning.value = true

        val sectionMax = renderer[section.value]!!.lastPage
        val start = pageAnim.value.roundToInt()

        val sectionDelta = delta.coerceIn(-start, sectionMax - start)
        val remainingDelta = delta - sectionDelta

        var animationHasBegun = sequential
        if (sectionDelta != 0) {
            // TODO: sequentialAnimation appears not to work correctly here when we animate forwards
            // by two, and this is called on the recursive call
            pageAnim.animateTo(
                (start + sectionDelta).toFloat(),
                if (sequential) tween(durationMillis) else tween(
                    (durationMillis * abs(sectionDelta).toFloat() / abs(
                        delta
                    ).toFloat()).roundToInt()
                ),
                sequentialAnimation = sequential
            )
            animationHasBegun = true
        }

        when {
            remainingDelta > 0 -> {
                if (section.value == renderer.maxSection)  {
                    _isRunning.value = false
                    return
                }

                pageAnim.animateTo(
                    sectionMax + NEARLY_ONE,
                    if (animationHasBegun) tween(durationMillis) else tween(
                        (durationMillis * NEARLY_ONE / abs(
                            delta
                        ).toFloat()).roundToInt()
                    ),
                    animationHasBegun
                )
                animationHasBegun = true

                section.value++
                setAnimValue(0f)
                val nextDelta = remainingDelta - 1
                animateBySuspended(
                    nextDelta,
                    durationMillis,
                    animationHasBegun
                )
            }

            remainingDelta < 0 -> {
                if (section.value == 0) {
                    _isRunning.value = false
                    return
                }

                section.value--
                val nextSectionMax = renderer[section.value]!!.lastPage
                setAnimValue(nextSectionMax.toFloat() + 0.999f)
                pageAnim.animateTo(
                    nextSectionMax.toFloat(),
                    if (animationHasBegun) tween(durationMillis) else tween(
                        (durationMillis * NEARLY_ONE / abs(
                            delta
                        ).toFloat()).roundToInt()
                    ),
                    animationHasBegun
                )
                animationHasBegun = true

                val nextDelta = remainingDelta + 1
                animateBySuspended(
                    nextDelta,
                    durationMillis,
                    animationHasBegun
                )
            }

            else -> {
                _isRunning.value = false
            }
        }
    }

    private fun setAnimValue(value: Float) {
        pageAnim = pageAnim.copy(value = value)
    }

    companion object {
        private const val NEARLY_ONE = 0.999f
    }
}

@Composable
private fun PaginatedSections(position: PagePosition, renderer: Renderer) {
    val currentSection = renderer[position.section]
        ?: throw IllegalArgumentException("Position $position does not exist")
    val nextSection = renderer[position.section + 1]

    // If last page in the section is partly turned
    if (nextSection != null && position.page > currentSection.lastPage) {
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
        val pages = renderer.pages

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

        // If we've turned past the end of the section, the next section is responsible for
        // rendering the next page
        if (turnPercent != 0f && position < renderer.lastPage) {
            val page = pages.getOrNull(index + 1)
            if (page == null) {
                BlankPage(renderer.background)
            } else {
                Page(page)
            }
        }

        Page(pages[index], turn)
    }
}

@Composable
private fun BlankPage(background: Color, turn: Dp = 0.dp) {
    Surface(
        Modifier
            .fillMaxSize()
            .background(background)
            .offset {
                IntOffset(
                    -turn
                        .toPx()
                        .roundToInt(), 0
                )
            },
        elevation = 15.dp
    ) {}
}

@Composable
private fun Page(page: PageRenderer, turn: Dp = 0.dp) {
    Surface(
        Modifier
            .fillMaxSize()
            .background(page.background)
            .offset {
                IntOffset(
                    -turn
                        .toPx()
                        .roundToInt(), 0
                )
            },
        elevation = 15.dp
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas {
                page.paint(it)
            }
        }
    }
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
