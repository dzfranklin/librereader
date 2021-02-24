package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.TargetAnimation
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.smoothScrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalAnimationClock
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.util.lruCache
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.epub.Epub
import org.danielzfranklin.librereader.epub.EpubSection
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.ui.LocalRepo
import org.danielzfranklin.librereader.util.clamp
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalCoroutinesApi::class)
fun ReaderScreen(bookId: BookID) {
    val repo = LocalRepo.current
    val model = viewModel(
        ReaderModel::class.java,
        key = bookId.toString(),
        factory = ReaderModel.Factory(repo, bookId)
    )

    val book: State<ReaderModel.Book?> = produceState(null) {
        value = model.book.await()
    }

    val current = book.value
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.fillMaxWidth(0.5f).aspectRatio(1f))
        }
    } else {
        Pages(
            current.epub,
            current.position.collectAsState(),
            model::updatePosition
        )
    }
}

@Preview(widthDp = 411, heightDp = 731)
@Composable
fun PagesPreview() {
    val epub = remember { mutableStateOf<Epub?>(null) }

    val string = stringResource(R.string.preview_section_text)
    LaunchedEffect(true) {
        val text = with(AnnotatedString.Builder()) {
            for ((i, para) in string.split("{{para_sep}}").withIndex()) {
                pushStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp)))

                if (i == 0) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                } else {
                    pushStyle(SpanStyle())
                }

                append(para)
                pop()
                pop()
            }
            toAnnotatedString()
        }
        val id = BookID("sample:1")
        epub.value = Epub(
            id,
            0,
            getSection = { if (it == 0) EpubSection(id, 0, text) else null },
            listOf(text.length)
        )
    }

    val cached = epub.value
    if (cached != null) {
        Pages(
            cached,
            remember { mutableStateOf(BookPosition.startOf(cached)) },
            onPosition = remember { { Timber.i("onPosition %s", it) } }
        )
    }
}

@Composable
fun Pages(epub: Epub, position: State<BookPosition>, onPosition: (BookPosition) -> Unit) {
    val padding = 40.dp

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val state = rememberPagesState(
            epub,
            TextStyle(fontSize = 20.sp),
            minWidth,
            minHeight,
            padding,
            onPosition
        )

        LaunchedEffect(position, onPosition) {
            snapshotFlow { position.value }
                .collect { state.jumpTo(it) }
        }

        Box(
            Modifier.scrollable(
                Orientation.Horizontal,
                state.turn.controller,
                enabled = state.turn.enabled.value
            ).background(state.pageBackground)
        ) {
            val next = state.nextPage.value
            val current = state.currentPage.value

            if (next != null) {
                Page(next)
            }

            if (current != null) {
                Page(current, state.turn.current)
            }

            if (!state.turn.enabled.value) {
                // TODO: Style muted
                CircularProgressIndicator()
            }

            // TODO: Work in pixels not percents?
            // TODO: Do something inspired by LazyColumn? PaginatedLazyRow?
            LazyColumn(content = { /*TODO*/ })
        }
    }
}

@Composable
fun rememberPagesState(
    epub: Epub,
    baseStyle: TextStyle,
    width: Dp,
    height: Dp,
    padding: Dp,
    onPosition: (BookPosition) -> Unit
): PagesState {
    val clock = LocalAnimationClock.current.asDisposableClock()
    val density = LocalDensity.current
    val fontLoader = LocalFontLoader.current
    val coroutineContext = rememberCoroutineScope().coroutineContext
    // TODO: Do we need to add a remember call here, or will the composable change tracking solve this anyway?
    return remember(
        epub,
        baseStyle,
        width,
        height,
        padding,
        onPosition,
        clock,
        density,
        fontLoader,
        coroutineContext
    ) {
        Timber.i("Computing pages state")
        PagesState(
            coroutineContext,
            epub,
            baseStyle,
            width,
            height,
            padding,
            onPosition,
            clock,
            density,
            fontLoader
        )
    }
}

class PagesState(
    override val coroutineContext: CoroutineContext,
    internal val epub: Epub,
    private val baseStyle: TextStyle,
    private val outerWidth: Dp,
    outerHeight: Dp,
    private val padding: Dp,
    private val onPosition: (BookPosition) -> Unit,
    internal val clock: AnimationClockObservable,
    private val density: Density,
    private val fontLoader: Font.ResourceLoader,
) : CoroutineScope {
    internal val pageBackground = baseStyle.background

    private val innerWidth = outerWidth - padding * 2f
    private val innerHeight = outerHeight - padding * 2f
    internal val outerWidthPx = with(density) { outerWidth.toPx() }

    internal val _currentPage = mutableStateOf<PageState?>(null)
    internal val _nextPage = mutableStateOf<PageState?>(null)
    val currentPage: State<PageState?> = _currentPage
    val nextPage: State<PageState?> = _nextPage

    val turn = PagesTurnState(coroutineContext, this)

    init {
        launch {
            snapshotFlow { _currentPage.value?.measures?.value?.start }
                .filterNotNull()
                .collect { onPosition(it) }
        }
    }

    internal val renderers = lruCache<Int, SectionRenderer>(2, create = { sectionIndex ->
        epub.section(sectionIndex)?.let { section ->
            SectionRenderer(
                innerWidth,
                innerHeight,
                padding,
                section,
                makePosition = { BookPosition(epub, section.index, it) },
                baseStyle,
                density,
                fontLoader
            )
        }
    })

    fun jumpTo(newPosition: BookPosition) {
        val measures = _currentPage.value?.measures?.value
        if (measures != null && measures.start <= newPosition && newPosition <= measures.end) {
            Timber.i("Skipping jumpTo because newPosition within currentPage")
            return
        }

        _currentPage.value?.cancel("Setting a new position")
        _nextPage.value?.cancel("Setting a new position")

        val newPage = PageState.at(this, newPosition)
        _currentPage.value = newPage
        _nextPage.value = newPage.after()

        Timber.i("Jumped to position")
    }
}

class PagesTurnState(
    override val coroutineContext: CoroutineContext,
    private val pagesState: PagesState
) : CoroutineScope {
    private val _currentPage = pagesState._currentPage
    private val _nextPage = pagesState._nextPage
    private val clock = pagesState.clock
    private val widthPx = pagesState.outerWidthPx

    private val _current = mutableStateOf(0f)

    /** Range: 0f <= turn <= 1f. 0f is fully in view, 1f is fully out of view */
    val current: State<Float> = _current

    val enabled = derivedStateOf { _currentPage.value?.measures?.value != null }

    private fun updatedCurrent(deltaPx: Float) = current.value - deltaPx / widthPx
    private fun turnPercentToPx(percent: Float) = percent * widthPx

    /* Examples:

    Turning forwards:
    deltaPx = [-10, -20, -10]
    We just change turn from 0f towards 1f
    To continue: add -delta as % to turn
    To abort: Animate turn back to 0f
    To complete: Animate turn to 1f, then update currentPage = nextPage

    Turning backwards:
    deltaPx = [10, 20, 10]
    We change current to current.before, then change turn from 1f to 0f
    To continue: add -delta as % to turn
    To abort: Animate turn back to 1f, then update currentPage = nextPage
    To complete: Animate turn to 0f
    */

    // We handle starting and continuing in the controller. We monitor scroll ends elsewhere and
    // either complete or start a new fake scroll to end up back in here, and then finally end
    // up back in the monitor to finish an abort/complete.

    private val flingConfig = FlingConfig(
        adjustTarget = {
            val percent = updatedCurrent(it)
            val target = when {
                percent < 0.5f -> 0f
                percent > 0.5f -> 1f
                else -> return@FlingConfig null
            }
            TargetAnimation(target)
        },
        decayAnimation = FloatExponentialDecaySpec(),
    )

    val controller = ScrollableController(consumeScrollDelta = { deltaPx ->
        if (!enabled.value) {
            Timber.w("turnController called, but turning should be disabled")
            return@ScrollableController 0f
        }
        val currentPage = _currentPage.value ?: return@ScrollableController 0f

        val unclampedNewCurrent = updatedCurrent(deltaPx)

        if (this.current.value == 0f) {
            // starting a turn
            if (deltaPx < 0) {
                // ...forwards
                if (_nextPage.value == null) {
                    _nextPage.value = currentPage.after() ?: return@ScrollableController 0f
                }
            } else {
                // ...backwards
                _currentPage.value = currentPage.before() ?: return@ScrollableController 0f
                _nextPage.value = currentPage
                _current.value = 0f
            }
        }

        val newCurrent = unclampedNewCurrent.clamp(0f, 1f)
        _current.value = newCurrent

        val overagePercent = unclampedNewCurrent - newCurrent
        val overagePx = turnPercentToPx(overagePercent)

        deltaPx - overagePx
    }, flingConfig, clock)

    init {
        launch {
            snapshotFlow { controller.isAnimationRunning }
                .drop(1)
                .collect {
                    if (it) return@collect

                    if (current.value > 0.009) {
                        // Finished a turn forwards

                        // If null, wouldn't have allowed turn start
                        val newCurrent = _nextPage.value!!

                        _currentPage.value = newCurrent
                        _nextPage.value = newCurrent.after()
                        _current.value = 0f

                        Timber.i("Completed turn forwards")
                    } else if (current.value < 0.001) {
                        Timber.i("Completing turn backwards")
                    } else {
                        val targetPercent = if (current.value < 0.5f) 0f else 1f
                        Timber.i("Completing turn from %s to %s", current.value, targetPercent)
                        val deltaPx =
                            turnPercentToPx(current.value) - turnPercentToPx(targetPercent)
                        launch {
                            controller.smoothScrollBy(deltaPx)
                        }
                    }
                }
        }
    }
}

class PageState private constructor(
    private val pagesState: PagesState,
    private val section: Int,
    private val page: Int
) : CoroutineScope {
    override val coroutineContext = pagesState.coroutineContext

    val background = pagesState.pageBackground

    private val _measures: MutableState<MeasuredPage?> = mutableStateOf(null)

    /** Null while loading */
    val measures: State<MeasuredPage?> = _measures

    /** May only be called after measures is loaded */
    fun paint(canvas: Canvas) {
        val measures = measures.value ?: throw IllegalStateException("Can only paint loaded page")
        getRenderer()!!.paintPage(canvas, measures)
    }

    init {
        launch {
            _measures.value = getRenderer()?.computePage(page)
                ?: throw IllegalStateException("Nonexistent page, constructors shouldn't have created")
        }
    }

    private fun getRenderer(): SectionRenderer? = pagesState.renderers[section]

    /** Null means nonexistent or not knowable without expensive computations. */
    fun before(): PageState? {
        val measures = measures.value ?: return null

        val section: Int
        val page: Int
        when {
            measures.pageIndex == 0 && measures.sectionIndex == 0 ->
                return null

            measures.pageIndex == 0 -> {
                section = measures.sectionIndex - 1
                page = 0
            }

            else -> {
                section = measures.sectionIndex
                page = measures.pageIndex - 1
            }
        }

        return PageState(pagesState, section, page)
    }

    /** False means nonexistent or not knowable without expensive computations. */
    fun hasAfter(): Boolean {
        val measures = measures.value ?: return false
        return !(measures.isLastPageOfSection && measures.sectionIndex == pagesState.epub.maxSection)
    }

    /** Null means nonexistent or not knowable without expensive computations. */
    fun after(): PageState? {
        val measures = measures.value ?: return null

        val section: Int
        val page: Int
        when {
            measures.isLastPageOfSection && measures.sectionIndex == pagesState.epub.maxSection ->
                return null

            measures.isLastPageOfSection -> {
                section = measures.sectionIndex + 1
                page = 0
            }

            else -> {
                section = measures.sectionIndex
                page = measures.pageIndex + 1
            }
        }

        return PageState(pagesState, section, page)
    }

    companion object {
        /** This function can be very expensive to call because it will need to measure all pages
         * in the target section up to the target page.
         *
         * Only use this function to jump to a new position, use before/after to navigate around
         * when possible.
         */
        fun at(pagesState: PagesState, position: BookPosition): PageState {
            val section = position.sectionIndex
            val renderer = pagesState.renderers[section]

            var page = 0
            while (true) {
                val pageRender = renderer.computePage(page)
                    ?: throw IllegalArgumentException("Invalid position $position")

                if (pageRender.start <= position && position <= pageRender.end) {
                    break
                }

                page++
            }

            return PageState(pagesState, section, page)
        }
    }
}

@Composable
fun Page(
    state: PageState,
    turn: State<Float> = mutableStateOf(0f),
) {
    // TODO: Look at CoreText TextController for how to handle selection
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Surface(
            Modifier
                .fillMaxSize()
                .offset {
                    // NOTE: Computing the offset in here means we only re-layout, not re-compose
                    IntOffset((-maxWidth.toPx() * turn.value).roundToInt(), 0)
                }.background(state.background),
            elevation = 15.dp
        ) {
            Timber.i("Re-rendering page %s", state)
            val measures = state.measures.value
            if (measures != null) {
                Canvas(Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        state.paint(canvas)
                    }
                }
            }
        }
    }
}
