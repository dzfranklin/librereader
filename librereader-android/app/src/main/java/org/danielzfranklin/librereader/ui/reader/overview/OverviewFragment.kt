package org.danielzfranklin.librereader.ui.reader.overview

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.OverviewFragmentBinding
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.PositionProcessor
import org.danielzfranklin.librereader.ui.reader.ReaderFragment
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import timber.log.Timber


class OverviewFragment : ReaderFragment(R.layout.overview_fragment), CoroutineScope {
    override val coroutineContext = lifecycleScope.coroutineContext
    private lateinit var binding: OverviewFragmentBinding

    private var nextScrollNotUser = false
    private lateinit var data: Data
    private val book: StateFlow<BookDisplay> by lazy { data.display }
    private val position: PositionProcessor by lazy { data.position }
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var viewJob: Job

    override fun onViewCreatedAndDataReceived(
        view: View,
        savedInstanceState: Bundle?,
        data: Data
    ) {
        binding = OverviewFragmentBinding.bind(view)
        this.data = data

        binding.seeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPosition = BookPosition.fromPageIndex(book.value, progress)
                    if (newPosition == null) {
                        Timber.w("Null newPosition: %s", progress)
                        return
                    }
                    position.set(binding.seeker, newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        binding.pages.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private val detector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        if (e == null) {
                            return false
                        }

                        val child = binding.pages.findChildViewUnder(e.x, e.y) ?: return false
                        val pageIndex = binding.pages.getChildAdapterPosition(child)

                        val book = book.value

                        if (pageIndex != position.value.pageIndex(book)) {
                            // User tapped one of the pages on the side, not the center page
                            return false
                        }

                        exitTo(BookPosition.fromPageIndex(book, pageIndex)!!)

                        return true
                    }
                })

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return detector.onTouchEvent(e)
            }
        })

        binding.pages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (nextScrollNotUser) {
                    nextScrollNotUser = false
                    return
                }

                val pageIndex = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (pageIndex == RecyclerView.NO_POSITION) {
                    return
                }

                val newPosition = BookPosition.fromPageIndex(book.value, pageIndex)!!
                position.set(binding.pages, newPosition)
            }
        })

        viewJob = launch {
            data.display.collectLatest { book ->
                binding.seeker.max = book.pageCount() - 1

                val pagesAdapter = OverviewPagesAdapter(book)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.pages.layoutManager = layoutManager
                binding.pages.adapter = pagesAdapter
                LinearSnapHelper().attachToRecyclerView(binding.pages)

                position.events.collect {
                    when (it.changer) {
                        binding.pages.hashCode() -> {
                            updateSeeker(it.position)
                        }

                        binding.seeker.hashCode() -> {
                            updatePages(it.position)
                        }

                        else -> {
                            updateSeeker(it.position)
                            updatePages(it.position)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewJob.cancel(CancellationException("onDestroyView"))
    }

    private fun updateSeeker(position: BookPosition) {
        binding.seeker.progress = position.pageIndex(book.value)
    }

    private fun updatePages(position: BookPosition) {
        nextScrollNotUser = true
        layoutManager.scrollToPosition(position.pageIndex(book.value))
    }

    private fun exitTo(pos: BookPosition) {
        position.set(this, pos)
        super.switchToPages()
    }
}