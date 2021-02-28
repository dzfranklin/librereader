package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import timber.log.Timber
import kotlin.math.floor
import kotlin.math.max
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
    val turnPercent = remember { derivedStateOf { position.value - index.value } }

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

        // If we've turned past the end of the section, the next section is responsible for
        // rendering the next page
        val page = pages.getOrNull(index.value + 1)
        if (page != null) {
            Page(page)
        }

        Page(pages[index.value], turnPercent)
    }
}

@Composable
private fun Page(page: PageRenderer, turn: State<Float> = mutableStateOf(0f)) {
    val density = LocalDensity.current
    val shadow = ShadowConfig(
        maxAtPercent = 0.3f,
        maxWidthPx = with(density) { 20.dp.toPx() },
    )
    val selectionEnabled = derivedStateOf { true }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.minWidth.toFloat()

        Box(
            Modifier
                .fillMaxSize()
                .pageTurn(turn, width, shadow)
                .background(page.background)
                .page(page)
                .selectablePage(page, selectionEnabled)
        )
    }
}

private fun Modifier.page(page: PageRenderer) = drawBehind {
    drawIntoCanvas { canvas ->
        Timber.i("Drawing page")
        page.paint(canvas)
    }
}

private fun Modifier.pageTurn(turn: State<Float>, pageWidthPx: Float, shadow: ShadowConfig) =
    graphicsLayer {
        val current = turn.value
        if (current != 0f) {
            translationX = -pageWidthPx * current

            val shadowPx = shadow.elevation(current)
            shadowElevation = shadowPx
            shape = PageShape
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

private fun Modifier.selectablePage(page: PageRenderer, enabled: State<Boolean>) =
    if (!enabled.value) this else graphicsLayer().composed {
        // Based on Compose MultiWidgetSelectionDelegate, with many features we don't need stripped out

        val selection = rememberSaveable(page) { mutableStateOf<Selection?>(null) }
        val colors = LocalTextSelectionColors.current

        pointerInput(page) {
            var dragBegin: Offset? = null

            coroutineScope {
                var tapDetector: Job? = null

                launch {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            selection.value = getSelectionInfo(
                                page = page,
                                startPosition = offset,
                                endPosition = offset,
                                previous = null,
                            )

                            dragBegin = offset

                            tapDetector = launch {
                                detectTapGestures {
                                    selection.value = null
                                    dragBegin = null
                                    tapDetector = null
                                    cancel("Cancel on tap")
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            selection.value = getSelectionInfo(
                                page = page,
                                startPosition = dragBegin!!,
                                endPosition = change.position,
                                previous = selection.value!!
                            )
                            change.consumeAllChanges()
                        })
                }
            }

            // TODO: Handles
        } then graphicsLayer().drawBehind {
            val range = selection.value?.toTextRange() ?: return@drawBehind
            if (range.min == range.max) return@drawBehind

            val path = page.getPathForRange(range.min, range.max)
            drawPath(path, colors.backgroundColor)
        }
    }

private fun getSelectionInfo(
    page: PageRenderer,
    startPosition: Offset,
    endPosition: Offset,
    previous: Selection?,
    wordBased: Boolean = true,
    isStartHandleDragged: Boolean = false,
): Selection? {
    // Based on Compose MultiWidgetSelectionDelegate getSelectionInfo, processAsSingleComposable,
    // and getRefinedSelectionInfo, with many features we don't need stripped out

    var startOffset = page.getOffsetForPosition(startPosition)
    var endOffset = page.getOffsetForPosition(endPosition)

    if (startOffset == endOffset) {
        // If the start and end offset are at the same character, and it's not the initial
        // selection, then bound to at least one character.
        val textRange = ensureAtLeastOneChar(
            offset = startOffset,
            lastOffset = page.lastOffset,
            previousSelection = previous?.toTextRange(),
            isStartHandle = isStartHandleDragged,
            handlesCrossed = previous?.handlesCrossed ?: false
        )
        startOffset = textRange.start
        endOffset = textRange.end
    }

    // nothing is selected
    if (startOffset == -1 && endOffset == -1) return null

    // Check if the start and end handles are crossed each other.
    val handlesCrossed = startOffset > endOffset

    // If under long press, update the selection to word-based.
    if (wordBased) {
        val startWordBoundary = page.getWordBoundary(startOffset.coerceIn(0, page.lastOffset))
        val endWordBoundary = page.getWordBoundary(endOffset.coerceIn(0, page.lastOffset))

        // If handles are not crossed, start should be snapped to the start of the word containing the
        // start offset, and end should be snapped to the end of the word containing the end offset.
        // If handles are crossed, start should be snapped to the end of the word containing the start
        // offset, and end should be snapped to the start of the word containing the end offset.
        startOffset = if (handlesCrossed) startWordBoundary.end else startWordBoundary.start
        endOffset = if (handlesCrossed) endWordBoundary.start else endWordBoundary.end
    }


    return Selection(
        start = Selection.AnchorInfo(
            direction = page.getBidiRunDirection(startOffset),
            offset = startOffset,
        ),
        end = Selection.AnchorInfo(
            direction = page.getBidiRunDirection(max(endOffset - 1, 0)),
            offset = endOffset,
        ),
        handlesCrossed = handlesCrossed
    )
}


/**
 * Copied from Compose
 *
 * This method adjusts the raw start and end offset and bounds the selection to one character. The
 * logic of bounding evaluates the last selection result, which handle is being dragged, and if
 * selection reaches the boundary.
 *
 * @param offset unprocessed start and end offset calculated directly from input position, in
 * this case start and offset equals to each other.
 * @param lastOffset last offset of the text. It's actually the length of the text.
 * @param previousSelection previous selected text range.
 * @param isStartHandle true if the start handle is being dragged
 * @param handlesCrossed true if the selection handles are crossed
 *
 * @return the adjusted [TextRange].
 */
private fun ensureAtLeastOneChar(
    offset: Int,
    lastOffset: Int,
    previousSelection: TextRange?,
    isStartHandle: Boolean,
    handlesCrossed: Boolean
): TextRange {
    // When lastOffset is 0, it can only return an empty TextRange.
    // When previousSelection is null, it won't start a selection and return an empty TextRange.
    if (lastOffset == 0 || previousSelection == null) return TextRange(offset, offset)

    // When offset is at the boundary, the handle that is not dragged should be at [offset]. Here
    // the other handle's position is computed accordingly.
    if (offset == 0) {
        return if (isStartHandle) {
            TextRange(1, 0)
        } else {
            TextRange(0, 1)
        }
    }

    if (offset == lastOffset) {
        return if (isStartHandle) {
            TextRange(lastOffset - 1, lastOffset)
        } else {
            TextRange(lastOffset, lastOffset - 1)
        }
    }

    // In other cases, this function will try to maintain the current cross handle states.
    // Only in this way the selection can be stable.
    return if (isStartHandle) {
        if (!handlesCrossed) {
            // Handle is NOT crossed, and the start handle is dragged.
            TextRange(offset - 1, offset)
        } else {
            // Handle is crossed, and the start handle is dragged.
            TextRange(offset + 1, offset)
        }
    } else {
        if (!handlesCrossed) {
            // Handle is NOT crossed, and the end handle is dragged.
            TextRange(offset, offset + 1)
        } else {
            // Handle is crossed, and the end handle is dragged.
            TextRange(offset, offset - 1)
        }
    }
}

/**
 * Copied from Compose
 * Information about the current Selection.
 */
@Immutable
@OptIn(ExperimentalTextApi::class)
internal data class Selection(
    /**
     * Information about the start of the selection.
     */
    val start: AnchorInfo,

    /**
     * Information about the end of the selection.
     */
    val end: AnchorInfo,
    /**
     * The flag to show that the selection handles are dragged across each other. After selection
     * is initialized, if user drags one handle to cross the other handle, this is true, otherwise
     * it's false.
     */
    // If selection happens in single widget, checking [TextRange.start] > [TextRange.end] is
    // enough.
    // But when selection happens across multiple widgets, this value needs more complicated
    // calculation. To avoid repeated calculation, making it as a flag is cheaper.
    val handlesCrossed: Boolean = false
) {
    /**
     * Contains information about an anchor (start/end) of selection.
     */
    @Immutable
    internal data class AnchorInfo(
        /**
         * Text direction of the character in selection edge.
         */
        val direction: ResolvedTextDirection,

        /**
         * Character offset for the selection edge. This offset is within the page
         */
        val offset: Int,
    )

    /**
     * Returns the selection offset information as a [TextRange]
     */
    fun toTextRange(): TextRange {
        return TextRange(start.offset, end.offset)
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
