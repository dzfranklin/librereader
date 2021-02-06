package org.danielzfranklin.librereader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spanned
import android.text.SpannedString
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.databinding.ReaderPagesViewBinding
import kotlin.coroutines.CoroutineContext

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ReaderPagesView(
    context: Context,
    override val coroutineContext: CoroutineContext,
    private val book: Book,
    private val initialPosition: BookPosition
) : LinearLayout(context), View.OnLayoutChangeListener, CoroutineScope {
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = ReaderPagesViewBinding.inflate(inflater, this, true)

    private val pageStyle = PageStyle()
    private lateinit var sectionPages: List<Spanned>
    private var pageIndex = 0

    private val emptySpan = SpannedString("")
    private var nextPage = PageView(context, this, emptySpan, pageStyle, 1f)
    private var prevPage = PageView(context, this, emptySpan, pageStyle, 0f)
    private var currentPage = PageView(context, this, emptySpan, pageStyle, 1f)

    private val turnState = MutableStateFlow<TurnState>(TurnState.Initial)
    private val pageWidth = MutableStateFlow(0f)
    val gestureDetector = ReaderPagesGestureDetector(context, turnState, pageWidth)

    init {
        launch {
            turnState.collect { turn ->
                when (turn) {
                    is TurnState.BeganTurnBack -> {
                        disableSelection()

                        val toRecycle = prevPage
                        prevPage = nextPage.apply {
                            percentTurned = 0f
                            text = sectionPages.getOrNull(pageIndex - 2)
                        }
                        nextPage = currentPage.apply {
                            bringToFront()
                        }
                        currentPage = toRecycle.apply {
                            percentTurned = 0f
                            bringToFront()
                        }
                    }

                    is TurnState.BeganTurnForward -> {
                        disableSelection()
                    }

                    is TurnState.TurningBackwards -> {
                        currentPage.percentTurned = turn.percent
                    }

                    is TurnState.TurningForwards -> {
                        currentPage.percentTurned = 1f - turn.percent
                    }

                    is TurnState.CompletingTurnBack -> {
                        currentPage.percentTurned = 1f
                        currentPage.setTextIsSelectable(true)
                        pageIndex--
                    }

                    is TurnState.CompletingTurnForward -> {
                        pageIndex++

                        val toRecycle = prevPage
                        prevPage = currentPage
                        currentPage = nextPage.apply {
                            percentTurned = 1f
                            setTextIsSelectable(true)
                            bringToFront()
                        }
                        nextPage = toRecycle.apply {
                            percentTurned = 1f
                            text = sectionPages.getOrNull(pageIndex + 1)
                        }
                    }

                    is TurnState.CancellingTurnBack -> {
                        val toRecycle = prevPage
                        prevPage = currentPage.apply {
                            percentTurned = 0f
                        }
                        currentPage = nextPage.apply {
                            percentTurned = 1f
                            setTextIsSelectable(true)
                            bringToFront()
                        }
                        nextPage = toRecycle.apply {
                            percentTurned = 1f
                            text = sectionPages.getOrNull(pageIndex + 2)
                        }
                    }

                    is TurnState.CancellingTurnForward -> {
                        currentPage.percentTurned = 1f
                        currentPage.setTextIsSelectable(true)
                    }
                }
            }
        }
    }

    private fun disableSelection() {
        prevPage.setTextIsSelectable(false)
        nextPage.setTextIsSelectable(false)
        currentPage.setTextIsSelectable(false)
    }

    init {
        addOnLayoutChangeListener(this)
    }

    private val textPaint = binding.measurementDummy.apply {
        style = pageStyle
    }.paint

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
            return
        }

        binding.parent.removeAllViews()

        sectionPages = computeSectionPages(initialPosition, textPaint)

        prevPage.text = sectionPages.getOrNull(pageIndex - 1)
        currentPage.text = sectionPages.getOrNull(pageIndex)
        nextPage.text = sectionPages.getOrNull(pageIndex + 1)

        binding.parent.addView(prevPage, pageLayoutParams)
        binding.parent.addView(nextPage, pageLayoutParams)
        binding.parent.addView(currentPage, pageLayoutParams)

        pageWidth.value = width.toFloat()
    }

    private fun computeSectionPages(pos: BookPosition, textPaint: TextPaint): List<Spanned> {
        if (pos !is BookPosition.Position) {
            TODO()
        }

        val section = book.sections[pos.sectionIndex]
        val props = BookSection.PageDisplayProperties(
            binding.root.width - pageStyle.padding * 2,
            binding.root.height - pageStyle.padding * 2,
            textPaint
        )
        return section.paginate(props)
    }

    companion object {
        private val pageLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}