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
import org.danielzfranklin.librereader.databinding.ReaderPagesViewBinding

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ReaderPagesView(
    context: Context,
    private val book: Book,
    private val initialPosition: BookPosition
) : LinearLayout(context), View.OnLayoutChangeListener {
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = ReaderPagesViewBinding.inflate(inflater, this, true)

    private val pageStyle = PageStyle()
    private lateinit var sectionPages: List<Spanned>
    private var pageIndex = 0

    private val emptySpan = SpannedString("")
    var nextPage = PageView(context, this, emptySpan, pageStyle, 1f)
    var prevPage = PageView(context, this, emptySpan, pageStyle, 0f)
    var currentPage = PageView(context, this, emptySpan, pageStyle, 1f)

    fun turnTo(percent: Float) {
        currentPage.percentTurned = percent

        if (percent <= 0f) {
            pageIndex++

            val toRecycle = prevPage
            prevPage = currentPage
            currentPage = nextPage
            nextPage = toRecycle.apply {
                percentTurned = 1f
                text = sectionPages.getOrNull(pageIndex + 1)
            }

            currentPage.bringToFront()
        }
    }

    fun beginTurnBack() {
        pageIndex--

        val toRecycle = nextPage
        prevPage = toRecycle.apply {
            percentTurned = 0f
            text = sectionPages.getOrNull(pageIndex - 1)
        }
        currentPage = prevPage.apply {
            percentTurned = 0f
        }
        nextPage = currentPage

        currentPage.bringToFront()
    }

    val gestureDetector = ReaderPagesGestureDetector(context, this)

    init {
        gestureDetector
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