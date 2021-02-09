package org.danielzfranklin.librereader.ui.reader.overviewView

import android.text.style.ImageSpan
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.PageView
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import kotlin.math.min
import kotlin.math.roundToInt

class OverviewPagesAdapter(private val book: BookDisplay) :
    RecyclerView.Adapter<OverviewPagesAdapter.ViewHolder>() {

    override fun getItemCount() = book.pageCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val padding = book.pageDisplay.style.computePaddingPixels(parent.context)
        val pageWidth = book.pageDisplay.width.toFloat() + padding.toFloat() * 2f
        val pageHeight = book.pageDisplay.height.toFloat() + padding.toFloat() * 2f
        val scaleFactor = min(
            (parent.width.toFloat() / pageWidth) * 0.7f,
            parent.height.toFloat() / pageHeight
        )

        val scaledWidth = pageWidth * scaleFactor
        val scaledHeight = pageHeight * scaleFactor
        val gap = parent.context.resources.getDimension(R.dimen.overviewPageGap).roundToInt()
        val marginY = ((parent.height.toFloat() - scaledHeight) / 2).roundToInt()
        val layoutParams = ViewGroup.MarginLayoutParams(
            scaledWidth.roundToInt(),
            scaledHeight.roundToInt()
        ).apply {
            topMargin = marginY
            bottomMargin = marginY
        }

        return ViewHolder(PageView(parent.context).apply {
            style = book.pageDisplay.style
            this.layoutParams = layoutParams
            textSize = style.computeTextSizePixels(parent.context) * scaleFactor
            // recompute to avoid rounding twice
            setPadding((style.computePaddingPixels(parent.context) * scaleFactor).toInt())
        }, scaleFactor, gap)
    }

    override fun onBindViewHolder(holder: ViewHolder, pageIndex: Int) {
        val position = BookPosition.fromPageIndex(book, pageIndex)!!
        val text = position.page(book).toSpannable()
        for (img in text.getSpans(0, text.length - 1, ImageSpan::class.java)) {
            val bounds = img.drawable.bounds
            val width = ((bounds.right - bounds.left).toFloat() * holder.scaleFactor).roundToInt()
            val height = ((bounds.bottom - bounds.top).toFloat() * holder.scaleFactor).roundToInt()
            img.drawable.setBounds(
                bounds.left,
                bounds.top,
                bounds.left + width,
                bounds.top + height
            )
        }
        (holder.view.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginEnd = holder.gap
            if (pageIndex == 0) {
               marginStart = holder.gap
            }
        }
        holder.view.displaySpan(text)
    }

    class ViewHolder(
        val view: PageView,
        val scaleFactor: Float,
        val gap: Int
    ) : RecyclerView.ViewHolder(view)
}
