package org.danielzfranklin.librereader.ui.library

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.danielzfranklin.librereader.databinding.LibraryBookBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import kotlin.math.roundToInt

class BooksAdapter(private val onClick: (BookID) -> Unit) :
    RecyclerView.Adapter<BooksAdapter.ViewHolder>() {

    private var list = emptyList<BookMeta>()

    fun update(newList: List<BookMeta>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size

            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldIndex: Int, newIndex: Int) =
                list[oldIndex].id == newList[newIndex].id

            override fun areContentsTheSame(oldIndex: Int, newIndex: Int) =
                list[oldIndex] == newList[newIndex]
        })
        list = newList
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LibraryBookBinding.inflate(inflater, parent, false)
        return ViewHolder(onClick, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    class ViewHolder(private val onClick: (BookID) -> Unit, val binding: LibraryBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: BookMeta) {
            val isUpdate = binding.root.tag == book.id

            binding.root.apply {
                tag = book.id
                background.setTint(book.coverBgColor)
                outlineProvider = ViewOutlineProvider.BOUNDS
                clipToOutline = true
            }
            binding.bookCover.apply {
                setImageDrawable(book.cover)
            }
            binding.bookProgress.apply {
                setIndicatorColor(book.coverTextColor)
                trackColor = Color.TRANSPARENT
                trackCornerRadius = 0
                setProgressCompat(
                    (book.position.percent * 100).roundToInt(),
                    isUpdate
                )

            }
            binding.bookTitle.apply {
                setTextColor(book.coverTextColor)
                text = book.title.trim()
            }

            binding.root.setOnClickListener {
                onClick(book.id)
            }
        }
    }
}