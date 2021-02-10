package org.danielzfranklin.librereader.ui.reader.overviewView

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.databinding.OverviewViewBinding
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.PositionProcessor
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

@SuppressLint("ViewConstructor")
class OverviewView(
    context: Context,
    override val coroutineContext: CoroutineContext,
    private val book: BookDisplay,
    private val positionProcessor: PositionProcessor,
    private val showOverview: MutableStateFlow<Boolean>
) : LinearLayout(context), CoroutineScope {
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = OverviewViewBinding.inflate(inflater, this, true)

    private var isInitialized = false

    init {
        launch {
            showOverview.collect {
                if (it && !isInitialized) {
                    initialize()
                }

                visibility = if (it) {
                    VISIBLE
                } else {
                    GONE
                }
            }
        }
    }

    private var nextScrollNotUser = false
    private lateinit var layoutManager: LinearLayoutManager


    private fun initialize() {
        binding.seeker.max = book.pageCount() - 1

        binding.seeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPosition = BookPosition.fromPageIndex(book, progress)
                    if (newPosition == null) {
                        Timber.w("Null newPosition: %s", progress)
                        return
                    }
                    positionProcessor.set(binding.seeker, newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        val pagesAdapter = OverviewPagesAdapter(book)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.pages.layoutManager = layoutManager
        binding.pages.adapter = pagesAdapter
        LinearSnapHelper().attachToRecyclerView(binding.pages)

        updateSeeker(positionProcessor.position)
        updatePages(positionProcessor.position)

        binding.pages.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private val detector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        if (e == null) {
                            return false
                        }

                        val child = binding.pages.findChildViewUnder(e.x, e.y) ?: return false
                        val pageIndex = binding.pages.getChildAdapterPosition(child)

                        if (pageIndex != positionProcessor.position.pageIndex(book)) {
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

                val newPosition = BookPosition.fromPageIndex(book, pageIndex)!!
                positionProcessor.set(binding.pages, newPosition)
            }
        })

        launch {
            positionProcessor.events.collect {
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

        isInitialized = true
    }

    private fun updateSeeker(position: BookPosition) {
        binding.seeker.progress = position.pageIndex(book)
    }

    private fun updatePages(position: BookPosition) {
        nextScrollNotUser = true
        layoutManager.scrollToPosition(position.pageIndex(book))
    }

    private fun exitTo(position: BookPosition) {
        positionProcessor.set(this, position)
        showOverview.value = false
    }
}