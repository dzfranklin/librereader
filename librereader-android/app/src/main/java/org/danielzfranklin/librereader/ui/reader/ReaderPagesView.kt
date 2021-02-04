package org.danielzfranklin.librereader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.TextPaint
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.ReaderPagesViewBinding

@SuppressLint("ViewConstructor")
class ReaderPagesView(
    context: Context,
    private val book: Book,
    private val initialPosition: BookPosition
) : LinearLayout(context) {
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = ReaderPagesViewBinding.inflate(inflater, this, true)

    private lateinit var sectionPages: List<Spanned>

    private val fontSize = 25f
    private val padding = 50

    private var page = 0

    private var transitionListener = object : TransitionAdapter() {
        override fun onTransitionStarted(layout: MotionLayout?, startId: Int, endId: Int) {
            when (endId) {
                R.id.goPrev -> setPageText(binding.bottomPage, page - 1)
                R.id.goNext -> setPageText(binding.bottomPage, page + 1)
            }
        }

        override fun onTransitionCompleted(layout: MotionLayout?, currentId: Int) {
            if (layout == null) {
                return
            }

            when (currentId) {
                R.id.goNext -> page++
                R.id.goPrev -> page--
                else -> return
            }

            // Go to state=rest
            // (0% of the way in either direction would work)
            layout.progress = 0f
            layout.setTransition(R.id.rest, R.id.goNext)
            setPageText(binding.topPage, page)
        }
    }

    init {
        initializePage(binding.bottomPage)
        initializePage(binding.topPage)

        val textPaint = binding.bottomPage.paint

        binding.motionLayout.setTransitionListener(transitionListener)

        // Enqueue for when layout is done. See <https://stackoverflow.com/a/24035591>
        post {
            sectionPages = computeSectionPages(initialPosition, textPaint)
            setPageText(binding.topPage, page)
        }

        propagateCapturedGestures()
    }

    private fun setPageText(view: TextView, pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= sectionPages.size) {
            view.text = "UNLOADED"
        } else {
            view.text = sectionPages[pageIndex]
        }
    }

    private fun initializePage(page: TextView) {
        page.setBackgroundColor(Color.WHITE)
        page.textSize = fontSize
        page.setPadding(padding, padding, padding, padding)
        page.scrollIndicators = 0
    }

    private fun computeSectionPages(pos: BookPosition, textPaint: TextPaint): List<Spanned> {
        if (pos !is BookPosition.Position) {
            TODO()
        }

        val section = book.sections[pos.sectionIndex]
        val props = BookSection.PageDisplayProperties(
            binding.root.width - padding * 2,
            binding.root.height - padding * 2,
            textPaint
        )
        return section.paginate(props)
    }

    @SuppressLint("ClickableViewAccessibility")
    // TextView & MotionLayout are responsible for calling performClick, this shouldn't override
    private fun propagateCapturedGestures() {
        // Required to make text selection and motion gestures work at the same time
        binding.topPage.setOnTouchListener { _, event ->
            binding.motionLayout.onTouchEvent(event)
        }
    }
}