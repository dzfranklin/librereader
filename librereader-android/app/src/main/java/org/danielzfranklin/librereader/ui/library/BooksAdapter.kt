package org.danielzfranklin.librereader.ui.library

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.danielzfranklin.librereader.databinding.LibraryBookBinding
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import kotlin.math.roundToInt

class BooksAdapter(
    private val onClick: (BookID) -> Unit,
    private val getCover: suspend (BookID) -> Bitmap?
) : RecyclerView.Adapter<BooksAdapter.ViewHolder>() {

    private var list = emptyList<BookMeta>()
    private var covers = mutableMapOf<BookID, Bitmap>()

    suspend fun update(newList: List<BookMeta>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size

            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldIndex: Int, newIndex: Int) =
                list[oldIndex].id == newList[newIndex].id

            override fun areContentsTheSame(oldIndex: Int, newIndex: Int) =
                list[oldIndex] == newList[newIndex]
        })

        updateCovers(newList)
        list = newList
        diff.dispatchUpdatesTo(this)
    }

    private suspend fun updateCovers(newList: List<BookMeta>) = withContext(Dispatchers.Default) {
        val newCovers = newList
            .filter { !covers.containsKey(it.id) }
            .map {
                async {
                    // TODO: Replace missing with some default image
                    it.id to getCover(it.id)!!
                }
            }.awaitAll()

        for ((id, cover) in newCovers) {
            covers[id] = cover
        }
    }

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LibraryBookBinding.inflate(inflater, parent, false)
        return ViewHolder(onClick, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = list[position]
        holder.bind(book, covers[book.id]!!)
    }

    class ViewHolder(private val onClick: (BookID) -> Unit, val binding: LibraryBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: BookMeta, cover: Bitmap) {
            val isUpdate = binding.root.tag == book.id

            binding.root.apply {
                tag = book.id
                background.setTint(book.coverBgColor)
                outlineProvider = ViewOutlineProvider.BOUNDS
                clipToOutline = true
            }
            binding.bookCover.apply {
                setImageBitmap(cover)
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