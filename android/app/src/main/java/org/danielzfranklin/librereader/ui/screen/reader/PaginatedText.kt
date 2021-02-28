package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

@Immutable
data class PaginatedTextPosition(val section: Int, val charIndex: Int)

@Composable
fun PaginatedText(
    initialPosition: PaginatedTextPosition,
    onPosition: (PaginatedTextPosition) -> Unit,
    makeSection: (Int) -> AnnotatedString,
    maxSection: Int,
    baseStyle: TextStyle,
    padding: Dp = 15.dp,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val renderer = rememberRenderer(
            outerWidth = minWidth,
            outerHeight = minHeight,
            padding = padding,
            baseStyle = baseStyle,
            density = LocalDensity.current,
            fontLoader = LocalFontLoader.current,
            maxSection = maxSection,
            makeSection = makeSection
        )

        val initialPagePosition = remember {
            PagePosition(
                initialPosition.section,
                renderer[initialPosition.section]!!.findPage(initialPosition.charIndex)!!.index.toFloat()
            )
        }
        val position = rememberPositionState(initialPagePosition, renderer, onPosition)

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(constraints, position) {
                    coroutineScope {
                        var inDrag = false

                        launch {
                            detectTapGestures(onTap = { offset ->
                                if (!inDrag && !position.isAnimating.value) {
                                    when {
                                        offset.x < constraints.minWidth * 0.3f ->
                                            launch { position.animateBy(-1f) }

                                        offset.x > constraints.minWidth * 0.7f ->
                                            launch { position.animateBy(1f) }
                                    }
                                }
                            })
                        }

                        launch {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    inDrag = true
                                    val delta = change.position - change.previousPosition
                                    position.jumpBy(-delta.x / constraints.minWidth.toFloat())
                                    change.consumeAllChanges()
                                },
                                onDragCancel = {
                                    inDrag = false
                                    launch {
                                        position.animateToNearest()
                                    }
                                },
                                onDragEnd = {
                                    inDrag = false
                                    launch {
                                        position.animateToNearest()
                                    }
                                }
                            )
                        }
                    }
                }
        ) {
            PaginatedSections(position.position.value, renderer)
        }
    }

}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun PaginatedTextPreview() {
    val text = rememberAnnotatedStringPreview()

    val initialPosition = PaginatedTextPosition(0, 0)
    val position = remember { mutableStateOf(initialPosition) }

    Box {
        PaginatedText(
            initialPosition,
            { position.value = it },
            { text },
            2,
            TextStyle(fontSize = 22.sp)
        )
        Text(position.value.toString())
    }
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

        val animPosition = rememberPositionState(PagePosition(0, 0f), renderer, {})
        val animScope = rememberCoroutineScope()

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
                    { animScope.launch { animPosition.animateBy(-2f) } },
                    Modifier.padding(horizontal = 5.dp),
                    enabled = !animPosition.isAnimating.value
                ) {
                    Text("Prev")
                }

                Button(
                    { animScope.launch { animPosition.animateBy(2f) } },
                    Modifier.padding(horizontal = 5.dp),
                    enabled = !animPosition.isAnimating.value
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun rememberPositionState(
    initialPosition: PagePosition,
    renderer: Renderer,
    onPosition: (PaginatedTextPosition) -> Unit
): SectionsAnimationState {
    return remember(initialPosition, renderer, onPosition) {
        SectionsAnimationState(initialPosition, renderer, onPosition)
    }
}

private class SectionsAnimationState constructor(
    initialPosition: PagePosition,
    private val renderer: Renderer,
    private val onPosition: (PaginatedTextPosition) -> Unit,
) {
    private val _positionBacking = mutableStateOf(initialPosition)
    private var lastPageReportedToOnPosition = initialPosition.page.roundToInt()
    private var _position
        get() = _positionBacking.value
        set(value) {
            _positionBacking.value = value

            val page = floor(value.page).toInt()
            if (page != lastPageReportedToOnPosition) {
                val char = renderer[value.section]!!.pages[page].startChar
                onPosition(PaginatedTextPosition(value.section, char))
                lastPageReportedToOnPosition = page
            }
        }
    val position: State<PagePosition> = _positionBacking

    private val _isAnimating = mutableStateOf(false)
    val isAnimating: State<Boolean> = _isAnimating

    /** Returns unused delta */
    fun jumpBy(delta: Float): Float {
        val sectionMax = renderer[position.value.section]!!.lastPage
        val start = position.value.page

        val sectionDelta = delta.coerceIn(-start, (sectionMax + NEARLY_ONE) - start)
        val remainingDelta = delta - sectionDelta
        _position = _position.copy(page = start + sectionDelta)

        return when {
            remainingDelta > 0 -> {
                val newSection = _position.section + 1
                if (newSection > renderer.maxSection) {
                    return remainingDelta
                }
                _position = PagePosition(newSection, 0f)
                jumpBy(remainingDelta)
            }

            remainingDelta < 0 -> {
                val newSection = _position.section - 1
                if (newSection < 0) {
                    return remainingDelta
                }

                _position =
                    PagePosition(newSection, renderer[newSection]!!.lastPage.toFloat() + NEARLY_ONE)
                jumpBy(remainingDelta)
            }

            else -> 0f
        }
    }

    suspend fun animateBy(
        delta: Float,
        spec: AnimationSpec<Float> = spring(stiffness = 100f)
    ) {
        _isAnimating.value = true

        var prev = 0f
        animate(0f, delta, animationSpec = spec) { value, _ ->
            jumpBy(value - prev)
            prev = value
        }

        _isAnimating.value = false
    }

    suspend fun animateToNearest() {
        val delta = round(_position.page) - _position.page
        animateBy(delta, spring(stiffness = Spring.StiffnessLow))
    }

    companion object {
        private const val NEARLY_ONE = 0.9999f
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
