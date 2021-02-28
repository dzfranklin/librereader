package org.danielzfranklin.librereader.ui.screen.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
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
        val style: BookStyle,
        val position: BookPosition
    )

    private val epub = async { repo.getEpub(bookId)?.let { Epub(bookId, it) } }
    private val style = repo.getBookStyleFlow(bookId)
    private val position = async { repo.getPosition(bookId) }

    /** Updates when style changes (and not when position changes)
     * When style changes it will be combined with the latest position
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val book: Flow<Book?> = style.mapLatest { style ->
        val epub = epub.await()
        val position = position.await()
        if (epub != null && style != null && position != null) {
            Book(bookId, epub, style, position)
        } else {
            null
        }
    }

    fun updatePosition(position: BookPosition) {
        launch {
            repo.updatePosition(position)
        }
    }
}