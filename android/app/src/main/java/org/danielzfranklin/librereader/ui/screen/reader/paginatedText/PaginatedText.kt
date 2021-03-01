package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.ui.screen.reader.paginatedText.PageTextSelectionManager
import org.danielzfranklin.librereader.ui.screen.reader.paginatedText.selectablePageText
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

        val initialPagePosition = remember(initialPosition) {
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
            PaginatedSections(position.position, renderer)
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
            TextStyle(fontSize = 22.sp, background = Color.White)
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

        PaginatedSections(animPosition.position, renderer)

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

        val sectionDelta = if (_position.section < renderer.maxSection) {
            delta.coerceIn(-start, (sectionMax + NEARLY_ONE) - start)
        } else {
            delta.coerceIn(-start, sectionMax - start)
        }
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
private fun PaginatedSections(position: State<PagePosition>, renderer: Renderer) {
    val currentSection = remember {
        derivedStateOf {
            renderer[position.value.section]
                ?: throw IllegalArgumentException("Position $position does not exist")
        }
    }
    val nextSection = remember { derivedStateOf { renderer[position.value.section + 1] } }
    val page = remember { derivedStateOf { position.value.page } }

    // If last page in the section is partly turned
    val next = nextSection.value
    if (next != null && page.value > currentSection.value.lastPage) {
        // ... show the first page of the next section behind it
        PaginatedSection(next, mutableStateOf(0f))
    }

    PaginatedSection(currentSection.value, page)
}

/**
 *  @param position 1.7 means current page is 1, and we're 70% turned to 2
 */
@Composable
private fun PaginatedSection(renderer: SectionRenderer, position: State<Float>) {
    val index = remember { derivedStateOf { floor(position.value).roundToInt() } }
    val selectionEnabled = snapshotFlow { position.value == index.value.toFloat() }
        .collectAsState(true)

    val shadow = ShadowConfig(
        maxAtPercent = 0.3f,
        maxWidthPx = with(LocalDensity.current) { 20.dp.toPx() },
    )
    val pages = renderer.pages

    Layout(content = {
        Page(pages[index.value], selectionEnabled)

        // If we've turned past the end of the section, the next section is responsible for
        // rendering the next page
        val page = pages.getOrNull(index.value + 1)
        if (page != null) {
            Page(page, remember { mutableStateOf(false) })
        } else {
            Box(Modifier.fillMaxSize())
        }
    }) { (current, next), constraints ->
        require(renderer.outerWidth.toPx().roundToInt() == constraints.maxWidth)
        require(renderer.outerHeight.toPx().roundToInt() == constraints.maxHeight)

        val placedCurrent = current.measure(constraints)
        val placedNext = next.measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            placedCurrent.placeWithLayer(0, 0, 1f) {
                val turnPercent = position.value - index.value
                translationX = turnPercent * -constraints.maxWidth.toFloat()
                shadowElevation = shadow.elevation(turnPercent)
                shape = PageShape
            }

            placedNext.place(0, 0)
        }
    }
}

@Composable
private fun Page(page: PageRenderer, selectionEnabled: State<Boolean>) {
    val selectionManager = rememberSelectionManager(page)

    Box(
        Modifier
            .fillMaxSize()
            .background(page.background)
            .page(page)
            .selectablePageText(page, selectionEnabled, selectionManager)
    )
}

@Composable
private fun rememberSelectionManager(page: PageRenderer): PageTextSelectionManager {
    val textToolbar = LocalTextToolbar.current
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val context = rememberCoroutineScope().coroutineContext
    return rememberSaveable(
        context,
        page,
        textToolbar,
        saver = PageTextSelectionManager.saver(context, page, textToolbar, clipboardManager, density)
    ) { PageTextSelectionManager(context, page, textToolbar, clipboardManager, density) }
}

private var drawsDebug = 0
private const val SHOW_DEBUG_DRAW_COUNT = true
private val drawsDebugPaint = if (SHOW_DEBUG_DRAW_COUNT) {
    NativePaint().apply {
        color = Color.Red.toArgb()
        textSize = 100f
        isFakeBoldText = true
    }
} else null

private fun Modifier.page(page: PageRenderer) = drawBehind {
    drawIntoCanvas { canvas ->
        page.paint(canvas)

        if (SHOW_DEBUG_DRAW_COUNT) {
            drawsDebug++
            canvas.nativeCanvas.drawText(
                drawsDebug.toString(),
                size.width - 210f,
                200f,
                drawsDebugPaint!!
            )
        }
    }
}

private data class ShadowConfig(
    val maxWidthPx: Float,
    val maxAtPercent: Float
) {
    fun elevation(percent: Float) =
        (percent / maxAtPercent).coerceIn(0f, 1f) * maxWidthPx
}

private val PageShape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) =
        // Draw the top and bottom shadow outside the visible area
        Outline.Rectangle(Rect(Offset(0f, -100f), Size(size.width, size.height + 200f)))
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
