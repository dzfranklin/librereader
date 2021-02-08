package org.danielzfranklin.librereader.ui.reader.overviewView

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.ui.reader.PageView
import org.danielzfranklin.librereader.ui.reader.displayModel.BookDisplay
import kotlin.math.min

class OverviewPagesAdapter(
    private val book: BookDisplay,
    private val onClick: (position: BookPosition) -> Unit
) : RecyclerView.Adapter<OverviewPagesAdapter.ViewHolder>() {

    override fun getItemCount() = book.pageCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val scaleFactor = min(
            parent.width.toFloat() / book.pageDisplay.width.toFloat(),
            parent.height.toFloat() / book.pageDisplay.height.toFloat()
        )

        return ViewHolder(OverviewPageView(parent.context).apply {
            style = book.pageDisplay.style
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            scaleX = scaleFactor
            scaleY = scaleFactor
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, pageIndex: Int) {
        val position = BookPosition.fromPageIndex(book, pageIndex)!!
        holder.position = position
        holder.view.text = position.page(book)
        holder.onClick = onClick
    }

    class ViewHolder(val view: OverviewPageView) : RecyclerView.ViewHolder(view) {
        lateinit var position: BookPosition

        var onClick: (position: BookPosition) -> Unit = { }

        init {
            view.onClickListener = { onClick(position) }
        }
    }
}
