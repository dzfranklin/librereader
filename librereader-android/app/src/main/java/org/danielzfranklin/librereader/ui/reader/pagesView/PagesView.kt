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
import org.danielzfranklin.librereader.model.BookPosition
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

    private class Pages(val prev: PageView, val next: PageView, val current: PageView) {
        init {
            // Useful in testing and in debugging with the layout inspector
            prev.tag = "prevPage"
            next.tag = "nextPage"
            current.tag = "currentPage"

            prev.elevation = 0f
            next.elevation = 1f
            current.elevation = 2f
        }
    }

    private var pages = Pages(
        prev = createPage(positionProcessor.position.movedBy(book, -1), 0f),
        next = createPage(positionProcessor.position.movedBy(book, 1), 1f),
        current = createPage(positionProcessor.position, 1f)
    )

    init {
        // NOTE: Order of adding matters
        addView(pages.prev, pageLayoutParams)
        addView(pages.next, pageLayoutParams)
        addView(pages.current, pageLayoutParams)
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

                        pages = Pages(
                            prev = pages.next.apply {
                                percentTurned = 0f
                                displaySpan(
                                    positionProcessor.position.movedBy(book, -2)?.page(book)
                                )
                            },
                            next = pages.current,
                            current = pages.prev.apply {
                                percentTurned = 0f
                            }
                        )

                    }

                    is TurnState.BeganTurnForward -> {
                        disableSelection()
                    }

                    is TurnState.TurningBackwards -> {
                        pages.current.percentTurned = state.percent
                    }

                    is TurnState.TurningForwards -> {
                        pages.current.percentTurned = 1f - state.percent
                    }

                    is TurnState.CompletingTurnBack -> {
                        animateTurn(state.fromPercent, 1f) {
                            pages.current.percentTurned = 0f
                            pages.current.setTextIsSelectable(true)
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

                            pages = Pages(
                                prev = pages.current,
                                next = pages.prev.apply {
                                    percentTurned = 1f
                                    displaySpan(
                                        positionProcessor.position.movedBy(book, 1)?.page(book)
                                    )
                                },
                                current = pages.next.apply {
                                    percentTurned = 1f
                                    setTextIsSelectable(true)
                                }
                            )
                        }
                    }

                    is TurnState.CancellingTurnBack -> {
                        animateTurn(state.fromPercent, 0f) {
                            val currentPosition = positionProcessor.position
                            val sectionPages = book.sections[currentPosition.sectionIndex].pages()
                            val pageIndex = currentPosition.sectionPageIndex(book)

                            pages = Pages(
                                prev = pages.current.apply {
                                    percentTurned = 0f
                                },
                                current = pages.next.apply {
                                    percentTurned = 1f
                                    setTextIsSelectable(true)
                                },
                                next = pages.prev.apply {
                                    percentTurned = 1f
                                    displaySpan(sectionPages.getOrNull(pageIndex + 2))
                                }
                            )
                        }
                    }

                    is TurnState.CancellingTurnForward -> {
                        animateTurn(1 - state.fromPercent, 1f) {
                            pages.current.percentTurned = 1f
                            pages.current.setTextIsSelectable(true)
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

                if (it.changer != this@PagesView.hashCode()) {
                    pages.prev.displaySpan(it.position.movedBy(book, -1)?.page(book))
                    pages.current.displaySpan(it.position.page(book))
                    pages.next.displaySpan(it.position.movedBy(book, 1)?.page(book))
                }
            }
        }
    }

    /**
     * Not that from and to are the percent the current page is turned, not the percent the current
     * turn is complete.
     */
    private fun animateTurn(from: Float, to: Float, onEnd: () -> Unit) {
        val animator = ObjectAnimator.ofFloat(pages.current, "percentTurned", from, to)
        animator.duration = (pageAnimationTimePerPercent * abs(from - to) * 100).toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                onEnd()
            }
        })
        animator.start()
    }

    private fun disableSelection() {
        pages.prev.setTextIsSelectable(false)
        pages.next.setTextIsSelectable(false)
        pages.current.setTextIsSelectable(false)
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