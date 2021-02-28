package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
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

        val position = rememberPositionState(
            PagePosition(
                initialPosition.section,
                renderer[initialPosition.section]!!.findPage(initialPosition.charIndex)!!.index.toFloat()
            ), renderer
        )

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
                                            position.animateBy(-1)

                                        offset.x > constraints.minWidth * 0.7f ->
                                            position.animateBy(1)
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
            Text(position.position.value.toString())
        }
    }

}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun PaginatedTextPreview() {
    val text = rememberAnnotatedStringPreview()

    PaginatedText(
        PaginatedTextPosition(0, 0),
        { text },
        2,
        TextStyle(fontSize = 22.sp)
    )
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

        val animPosition = rememberPositionState(PagePosition(0, 0f), renderer)

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
                    enabled = !animPosition.isAnimating.value
                ) {
                    Text("Prev")
                }

                Button(
                    { animPosition.animateBy(2, 1000) },
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
    private var pageAnim = AnimationState(initialPosition.page)

    private val _positionBacking = mutableStateOf(initialPosition)
    private var _position
        get() = _positionBacking.value
        set(value) {
            _positionBacking.value = value
            pageAnim = pageAnim.copy(value = position.value.page)
        }
    val position: State<PagePosition> = _positionBacking

    private val _isAnimating = mutableStateOf(false)
    val isAnimating: State<Boolean> = _isAnimating

    suspend fun animateToNearest() {
        _isAnimating.value = true

        val nearest = _position.page.roundToInt()
        val sectionMax = renderer[_position.section]!!.lastPage
        val spec = spring<Float>(stiffness = Spring.StiffnessLow)
        when {
            nearest > sectionMax -> {
                pageAnim.animateTo(
                    sectionMax + NEARLY_ONE,
                    spec,
                    sequentialAnimation = false,
                    createOnAnim()
                )
                val newSection = _position.section + 1
                if (newSection > renderer.maxSection) return
                _position = PagePosition(newSection, 0f)
            }

            nearest < 0 -> {
                val newSection = _position.section - 1
                val newPage = renderer[newSection]!!.lastPage.toFloat()
                _position = PagePosition(newSection, newPage + NEARLY_ONE)
                pageAnim.animateTo(
                    newPage,
                    spec,
                    sequentialAnimation = false,
                    createOnAnim()
                )
                if (newSection < 0) return
            }

            else -> {
                pageAnim.animateTo(
                    nearest.toFloat(),
                    spec,
                    sequentialAnimation = false,
                    createOnAnim()
                )
            }
        }

        _isAnimating.value = false
    }

    /** Returns unused delta */
    fun jumpBy(delta: Float): Float {
        val sectionMax = renderer[position.value.section]!!.lastPage
        val start = position.value.page

        val sectionDelta = delta.coerceIn(-start, sectionMax.toFloat() - start)
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

                _position = PagePosition(newSection, renderer[newSection]!!.lastPage.toFloat() + NEARLY_ONE)
                jumpBy(remainingDelta)
            }

            else -> 0f
        }
    }

    fun animateBy(delta: Int, durationMillis: Int = 600, sequentialAnimation: Boolean = false) {
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

        // TODO: Instead of splitting duration, cancel animations and continue them to new targets
        //  so that we can use spring spec

        _isAnimating.value = true

        val sectionMax = renderer[position.value.section]!!.lastPage
        val start = position.value.page.roundToInt()

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
                sequential,
                createOnAnim()
            )
            animationHasBegun = true
        }

        when {
            remainingDelta > 0 -> {
                if (position.value.section == renderer.maxSection) {
                    _isAnimating.value = false
                    return
                }

                pageAnim.animateTo(
                    sectionMax + NEARLY_ONE,
                    if (animationHasBegun) tween(durationMillis) else tween(
                        (durationMillis * NEARLY_ONE / abs(
                            delta
                        ).toFloat()).roundToInt()
                    ),
                    animationHasBegun,
                    createOnAnim()
                )
                animationHasBegun = true

                _position = PagePosition(_position.section + 1, 0f)
                val nextDelta = remainingDelta - 1
                animateBySuspended(
                    nextDelta,
                    durationMillis,
                    animationHasBegun
                )
            }

            remainingDelta < 0 -> {
                if (position.value.section == 0) {
                    _isAnimating.value = false
                    return
                }

                val nextSectionMax = renderer[position.value.section]!!.lastPage
                _position =
                    PagePosition(_position.section - 1, nextSectionMax.toFloat() + NEARLY_ONE)
                pageAnim.animateTo(
                    nextSectionMax.toFloat(),
                    if (animationHasBegun) tween(durationMillis) else tween(
                        (durationMillis * NEARLY_ONE / abs(
                            delta
                        ).toFloat()).roundToInt()
                    ),
                    animationHasBegun,
                    createOnAnim()
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
                _isAnimating.value = false
            }
        }
    }

    private fun createOnAnim(): AnimationScope<Float, AnimationVector1D>.() -> Unit {
        return {
            _position = _position.copy(page = value)
        }
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
