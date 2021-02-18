package org.danielzfranklin.librereader.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.repo.Repo

class ReaderViewModel(val id: BookID) : ViewModel(), CoroutineScope {
    override val coroutineContext = viewModelScope.coroutineContext

    class Factory(private val bookId: BookID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass != ReaderViewModel::class.java) {
                throw IllegalStateException("Factory can only create ReaderViewModel")
            }
            return ReaderViewModel(bookId) as T
        }
    }

    private val repo = Repo.get()

    data class Loading(
        val cover: Deferred<Bitmap>,
        val data: Deferred<ReaderFragment.DisplayIndependentData>,
    )

    var loading: Loading? = null
    fun load(): Loading {
        val cache = loading
        if (cache != null) return cache

        val cover = async { repo.getCover(id)!! }

        val data = async {
            val epub = async { repo.getEpub(id)!! }
            val position = async { repo.getPosition(id)!! }

            ReaderFragment.DisplayIndependentData(
                id,
                repo.getBookStyleFlow(id).map { it!! },
                PositionProcessor(this@ReaderViewModel.coroutineContext, position.await()),
                epub.await()
            )
        }

        val result = Loading(cover, data)
        loading = result
        return result
    }
}
