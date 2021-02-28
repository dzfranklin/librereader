package org.danielzfranklin.librereader.ui.screen.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.danielzfranklin.librereader.epub.Epub
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.repo.Repo

class ReaderModel(
    private val repo: Repo,
    private val bookId: BookID,
) : ViewModel(), CoroutineScope {
    class Factory(
        private val repo: Repo,
        private val bookId: BookID,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass != ReaderModel::class.java) {
                throw IllegalArgumentException("Can only create ReaderModel, not $modelClass")
            }
            return ReaderModel(repo, bookId) as T
        }
    }

    override val coroutineContext = viewModelScope.coroutineContext

    data class Book(
        val id: BookID,
        val epub: Epub,
        val style: StateFlow<BookStyle>,
        val position: BookPosition
    )

    val book: Deferred<Book> = async {
        val epub = async { Epub(bookId, repo.getEpub(bookId)!!) }
        val style = async { repo.getBookStyleFlow(bookId).map { it!! }.stateIn(this@ReaderModel) }
        val position = repo.getPosition(bookId)!!
        val book = Book(bookId, epub.await(), style.await(), position)
        book
    }

    fun updatePosition(position: BookPosition) {
        launch {
            repo.updatePosition(position)
        }
    }
}