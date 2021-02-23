package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
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
        Pages(current.epub)
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
        epub.value = Epub(id, 2, getSection = { if (it == 2) EpubSection(id, 2, text) else null })
    }

    val cached = epub.value
    if (cached != null) {
        Pages(cached)
    }
}

class PagesState(
    override val coroutineContext: CoroutineContext,
    val epub: Epub,
    private val baseStyle: TextStyle,
    private val outerWidth: Dp,
    private val outerHeight: Dp,
    private val padding: Dp,
    private val onPosition: (BookPosition) -> Unit,
    private val flingConfig: FlingConfig,
    private val clock: AnimationClockObservable,
    private val density: Density,
    private val fontLoader: Font.ResourceLoader,
) : CoroutineScope {
    private val innerWidth = outerWidth - padding * 2f
    private val innerHeight = outerHeight - padding * 2f

    val currentPage = mutableStateOf<MeasuredPage?>(null)
    val nextPage = mutableStateOf<MeasuredPage?>(null)
    val percentTurned = mutableStateOf(1f)

    private val outerWidthPx = with(density) { outerWidth.toPx() }
    val scrollController = ScrollableController(consumeScrollDelta = {
        val deltaPercent = it / outerWidthPx

        percentTurned.value += deltaPercent

        // TODO: don't consume all if at start/end
        it
    }, flingConfig, clock)

    // TODO: have multiple rendering section states?
    val sectionIndex = 2
    var TODOPercentTODO = 0f
    val section =
        SectionRenderer(
            innerWidth, innerHeight, padding, epub.section(sectionIndex)!!,
            { BookPosition(epub.id, TODOPercentTODO, 2, it) }, baseStyle, density, fontLoader
        )

    fun setPosition(newPosition: BookPosition) {
        TODO()
    }

    init {
        // TODO: Temp
        launch {
            currentPage.value = section.computePage(1)!!
        }
        launch {
            nextPage.value = section.computePage(2)!!
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
    Timber.i("Computing pages state")
    val clock = LocalAnimationClock.current.asDisposableClock()
    val flingConfig = defaultFlingConfig()
    val density = LocalDensity.current
    val fontLoader = LocalFontLoader.current
    val coroutineContext = rememberCoroutineScope().coroutineContext
    // TODO: Do we need to add a remember call here, or will the composable change tracking solve this anyway?
    return PagesState(
        coroutineContext,
        epub,
        baseStyle,
        width,
        height,
        padding,
        onPosition,
        flingConfig,
        clock,
        density,
        fontLoader
    )
}

@Composable
fun Pages(epub: Epub) {
    val padding = 40.dp

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // TODO: Changing text changes layout, not text size???
        val state = rememberPagesState(
            epub,
            TextStyle(fontSize = 20.sp),
            minWidth,
            minHeight,
            padding,
            onPosition = { /*TODO*/ })

        Box(Modifier.scrollable(Orientation.Horizontal, state.scrollController)) {
            Page {
                drawRect(Color.White)
                val cached = state.nextPage.value
                if (cached != null) {
                    drawIntoCanvas { canvas ->
                        state.section.paintPage(canvas, cached)
                    }
                }
            }

            Page(state.percentTurned) {
                drawRect(Color.White)
                val cached = state.currentPage.value
                if (cached != null) {
                    drawIntoCanvas { canvas ->
                        state.section.paintPage(canvas, cached)
                    }
                }
            }
        }
    }
}

@Composable
fun Page(
    percentTurned: State<Float> = mutableStateOf(1f),
    draw: DrawScope.() -> Unit,
) {
    // TODO: Look at CoreText TextController for how to handle selection
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Surface(
            Modifier
                .fillMaxSize()
                .offset {
                    // NOTE: Computing the offset in here means we only re-layout, not re-compose
                    val actualPercentTurned = percentTurned.value.clamp(0f, 1f)

                    IntOffset(
                        (-maxWidth.toPx() * (1f - actualPercentTurned)).roundToInt(),
                        0
                    )
                },
            elevation = 15.dp
        ) {
            Canvas(Modifier.fillMaxSize(), draw)
        }
    }
}
