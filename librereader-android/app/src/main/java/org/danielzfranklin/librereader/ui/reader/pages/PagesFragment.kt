package org.danielzfranklin.librereader.ui.reader.pages

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.PagesFragmentBinding
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.*
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import org.danielzfranklin.librereader.util.toInspectString
import timber.log.Timber
import kotlin.math.abs

class PagesFragment : ReaderFragment(R.layout.pages_fragment), CoroutineScope,
    PagesGestureDetector.Listener {

    override val coroutineContext = lifecycleScope.coroutineContext

    // initialized in onViewCreated
    private lateinit var binding: PagesFragmentBinding
    private var gestureDetector: PagesGestureDetector? = null
    private lateinit var data: Data
    private val position: PositionProcessor by lazy { data.position }
    private val book: StateFlow<BookDisplay> by lazy { data.display }
    private lateinit var pages: Pages
    private lateinit var visibleJob: Job

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreatedAndDataReceived(
        view: View,
        savedInstanceState: Bundle?,
        data: Data
    ) {
        this.data = data
        binding = PagesFragmentBinding.bind(view)

        binding.pages.setOnTouchListener { _, event ->
            gestureDetector?.onTouchEvent(event) ?: false
        }
    }

    // TODO: Use state machine?

    override fun onResume() {
        gestureDetector?.resume()

        visibleJob = launch {

            gestureDetector = PagesGestureDetector(
                coroutineContext,
                requireContext(),
                data.display.value.pageDisplay.width.toFloat(),
                this@PagesFragment
            )

            data.display.collectLatest { book ->
                var initialPosition: BookPosition? = position.value
                if (::pages.isInitialized) {
                    removePages()
                }
                pages = createPages(book, initialPosition!!)

                position.events.collect {
                    Timber.d(
                        "On ${it.position}. ${it.position.page(book).toInspectString()}"
                    )

                    if (book.isFirstPage(it.position)) {
                        gestureDetector?.disableTurnBackwards()
                    } else {
                        gestureDetector?.enableTurnBackwards()
                    }

                    if (book.isLastPage(it.position)) {
                        gestureDetector?.disableTurnForwards()
                    } else {
                        gestureDetector?.enableTurnForwards()
                    }

                    if (it.changer != this@PagesFragment.hashCode() && it.position != initialPosition) {
                        if (::pages.isInitialized) {
                            removePages()
                        }
                        pages = createPages(book, it.position)

                        // if we re-visit this page later we need to re-display
                        initialPosition = null
                    }
                }
            }
        }

        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        gestureDetector?.pause()
        visibleJob.cancel(CancellationException("onPause"))
    }

    override fun onShowOverview() {
        super.switchToOverview()
    }

    override fun onBeginTurnBack() {
        val book = book.value
        disableSelection(pages)
        pages = Pages(
            prev = pages.next.apply {
                percentTurned = 0f
                displaySpan(position.value.movedBy(book, -2)?.page(book))
            },
            next = pages.current,
            current = pages.prev.apply {
                percentTurned = 0f
            }
        )
    }

    override fun onBeginTurnForward() {
        disableSelection(pages)
    }

    override fun onTurnBackwards(percent: Float) {
        pages.current.percentTurned = percent
    }

    override fun onTurnForwards(percent: Float) {
        pages.current.percentTurned = 1f - percent
    }

    override fun onCompleteTurnBack(fromPercent: Float) {
        animateTurn(pages.current, fromPercent, 1f) {
            pages.current.percentTurned = 0f
            pages.current.setTextIsSelectable(true)
            position.value.movedBy(book.value, -1)?.let {
                position.set(this@PagesFragment, it)
            }
        }
    }

    override fun onCompleteTurnForward(fromPercent: Float) {
        val book = book.value
        animateTurn(pages.current, 1 - fromPercent, 0f) {
            position.value.movedBy(book, 1)?.let {
                position.set(this@PagesFragment, it)
            }

            pages = Pages(
                prev = pages.current,
                next = pages.prev.apply {
                    percentTurned = 1f
                    displaySpan(
                        position.value.movedBy(book, 1)?.page(book)
                    )
                },
                current = pages.next.apply {
                    percentTurned = 1f
                    setTextIsSelectable(true)
                }
            )
        }
    }

    override fun onCancelTurnBack(fromPercent: Float) {
        val book = book.value
        animateTurn(pages.current, fromPercent, 0f) {
            val currentPosition = position.value
            val sectionPages =
                book.sections[currentPosition.sectionIndex].pages()
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

    override fun onCancelTurnForward(fromPercent: Float) {
        animateTurn(pages.current, 1 - fromPercent, 1f) {
            pages.current.apply {
                percentTurned = 1f
                setTextIsSelectable(true)
            }
        }
    }

    /**
     * Note that from and to are the percent the current page is turned, not the percent the current
     * turn is complete.
     */
    private fun animateTurn(page: PageView, from: Float, to: Float, onEnd: () -> Unit) {
        val animator = ObjectAnimator.ofFloat(page, "percentTurned", from, to)
        animator.duration = (pageAnimationTimePerPercent * abs(from - to) * 100).toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                onEnd()
            }
        })
        animator.start()
    }

    private fun disableSelection(pages: Pages) {
        pages.prev.setTextIsSelectable(false)
        pages.next.setTextIsSelectable(false)
        pages.current.setTextIsSelectable(false)
    }

    private fun removePages() {
        binding.pages.removeView(pages.prev)
        binding.pages.removeView(pages.current)
        binding.pages.removeView(pages.next)
    }

    private fun createPages(book: BookDisplay, position: BookPosition): Pages {
        val pages = Pages(
            prev = createPage(book, position.movedBy(book, -1), 0f),
            next = createPage(book, position.movedBy(book, 1), 1f),
            current = createPage(book, position, 1f)
        )
        binding.pages.addView(pages.prev, pageLayoutParams)
        binding.pages.addView(pages.next, pageLayoutParams)
        binding.pages.addView(pages.current, pageLayoutParams)
        return pages
    }

    private fun createPage(book: BookDisplay, position: BookPosition?, percentTurned: Float) =
        PageView(requireContext()).apply {
            style = book.pageDisplay.style
            displaySpan(position?.page(book))
            propagateTouchEventsTo = { gestureDetector?.onTouchEvent(it) ?: false }
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

    companion object {
        private val pageLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        private const val pageAnimationTimePerPercent = 3f
    }
}