package org.danielzfranklin.librereader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.model.BookID

class ReaderViewModel(val bookId: BookID) : ViewModel() {
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

    private val book = repo.getBook(bookId)!!
    val style = MutableStateFlow(book.style).asStateFlow()
    val positionProcessor = PositionProcessor(
        viewModelScope.coroutineContext,
        bookId,
        book.position
    )
    val epub = book.epub

    val showOverview = MutableStateFlow(false)
}