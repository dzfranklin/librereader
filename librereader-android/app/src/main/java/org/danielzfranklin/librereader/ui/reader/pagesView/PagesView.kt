package org.danielzfranklin.librereader.ui.reader.pagesView

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.PageView
import org.danielzfranklin.librereader.ui.reader.PositionProcessor
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.util.toInspectString
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class PagesView(
    context: Context,
    override val coroutineContext: CoroutineContext,
    private val book: BookDisplay,
    private val positionProcessor: PositionProcessor,
    private val showOverview: MutableStateFlow<Boolean>
) : ConstraintLayout(context), CoroutineScope {
    private fun createPage(position: BookPosition?, percentTurned: Float) =
        PageView(context).apply {
            style = book.pageDisplay.style
            displaySpan(position?.page(book))
            manager = this@PagesView
            this.percentTurned = percentTurned
            width = book.pageDisplay.width
            height = book.pageDisplay.height
        }

    private var prevPage = createPage(positionProcessor.position.movedBy(book, -1), 0f)
    private var nextPage = createPage(positionProcessor.position.movedBy(book, 1), 1f)
    private var currentPage = createPage(positionProcessor.position, 1f)

    init {
        // NOTE: Order of adding matters
        addView(prevPage, pageLayoutParams)
        addView(nextPage, pageLayoutParams)
        addView(currentPage, pageLayoutParams)
    }

    private val turnState = MutableStateFlow<TurnState>(TurnState.Initial)
    private val pageWidth = book.pageDisplay.width
    val gestureDetector =
        PagesGestureDetector(context, turnState, showOverview, pageWidth.toFloat())

    init {
        launch {
            showOverview.collect {
                visibility = if (it) {
                    GONE
                } else {
                    VISIBLE
                }
            }
        }

        launch {
            turnState.collect { state ->
                when (state) {
                    is TurnState.BeganTurnBack -> {
                        disableSelection()

                        val toRecycle = prevPage
                        prevPage = nextPage.apply {
                            percentTurned = 0f
                            displaySpan(positionProcessor.position.movedBy(book, -2)?.page(book))
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
                        currentPage.percentTurned = state.percent
                    }

                    is TurnState.TurningForwards -> {
                        currentPage.percentTurned = 1f - state.percent
                    }

                    is TurnState.CompletingTurnBack -> {
                        animateTurn(state.fromPercent, 1f) {
                            currentPage.percentTurned = 1f
                            currentPage.setTextIsSelectable(true)
                            positionProcessor.position.movedBy(book, -1)?.let {
                                positionProcessor.set(this@PagesView, it)
                            }
                        }
                    }

                    is TurnState.CompletingTurnForward -> {
                        animateTurn(1 - state.fromPercent, 0f) {
                            positionProcessor.position.movedBy(book, 1)?.let {
                                positionProcessor.set(this@PagesView, it)
                            }

                            val toRecycle = prevPage
                            prevPage = currentPage
                            currentPage = nextPage.apply {
                                percentTurned = 1f
                                setTextIsSelectable(true)
                                bringToFront()
                            }
                            nextPage = toRecycle.apply {
                                percentTurned = 1f
                                displaySpan(positionProcessor.position.movedBy(book, 1)?.page(book))
                            }
                        }
                    }

                    is TurnState.CancellingTurnBack -> {
                        animateTurn(state.fromPercent, 0f) {
                            val currentPosition = positionProcessor.position
                            val sectionPages = book.sections[currentPosition.sectionIndex].pages()
                            val pageIndex = currentPosition.sectionPageIndex(book)

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
                                displaySpan(sectionPages.getOrNull(pageIndex + 2))
                            }
                        }
                    }

                    is TurnState.CancellingTurnForward -> {
                        animateTurn(1 - state.fromPercent, 1f) {
                            currentPage.percentTurned = 1f
                            currentPage.setTextIsSelectable(true)
                        }
                    }
                }
            }
        }

        launch {
            positionProcessor.events.collect {
                Timber.d(
                    "Displaying ${it.position}. ${it.position.page(book).toInspectString()}"
                )

                if (book.isFirstPage(it.position)) {
                    gestureDetector.disableTurnBackwards()
                } else {
                    gestureDetector.enableTurnBackwards()
                }

                if (book.isLastPage(it.position)) {
                    gestureDetector.disableTurnForwards()
                } else {
                    gestureDetector.enableTurnForwards()
                }

                if (it.changer != this@PagesView) {
                    prevPage.displaySpan(it.position.movedBy(book, -1)?.page(book))
                    currentPage.displaySpan(it.position.page(book))
                    nextPage.displaySpan(it.position.movedBy(book, 1)?.page(book))
                }
            }
        }
    }

    /**
     * Not that from and to are the percent the current page is turned, not the percent the current
     * turn is complete.
     */
    private fun animateTurn(from: Float, to: Float, onEnd: () -> Unit) {
        val animator = ObjectAnimator.ofFloat(currentPage, "percentTurned", from, to)
        animator.duration = (pageAnimationTimePerPercent * abs(from - to) * 100).toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                onEnd()
            }
        })
        animator.start()
    }

    private fun disableSelection() {
        prevPage.setTextIsSelectable(false)
        nextPage.setTextIsSelectable(false)
        currentPage.setTextIsSelectable(false)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        gestureDetector.onWindowVisibilityChanged(visibility)
        super.onWindowVisibilityChanged(visibility)
    }

    companion object {
        private val pageLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        private const val pageAnimationTimePerPercent = 3f
    }
}