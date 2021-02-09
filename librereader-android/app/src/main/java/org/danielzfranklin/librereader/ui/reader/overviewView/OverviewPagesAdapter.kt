package org.danielzfranklin.librereader.ui.reader.overviewView

import android.text.style.ImageSpan
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.PageView
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import kotlin.math.min
import kotlin.math.roundToInt

class OverviewPagesAdapter(rv: RecyclerView, private val book: BookDisplay) :
    RecyclerView.Adapter<OverviewPagesAdapter.ViewHolder>() {

    override fun getItemCount() = book.pageCount()

    private val pageStyle = book.pageDisplay.style
    private val pageWidth = book.pageDisplay.width.toFloat() + pageStyle.padding * 2
    private val pageHeight = book.pageDisplay.height.toFloat() + pageStyle.padding * 2
    private val scaleFactor = min(
        (rv.width.toFloat() / pageWidth) * 0.7f,
        rv.height.toFloat() / pageHeight
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        )

        return ViewHolder(PageView(parent.context).apply {
            style = book.pageDisplay.style
            layoutParams = RecyclerView.LayoutParams(
                (pageWidth * scaleFactor).roundToInt(),
                (pageHeight * scaleFactor).roundToInt()
            )
            textSize = style.textSize * scaleFactor
            setPadding((style.padding * scaleFactor).toInt())
        }, scaleFactor)
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
        holder.view.displaySpan(text)
    }

    class ViewHolder(val view: PageView, val scaleFactor: Float) : RecyclerView.ViewHolder(view)
}
