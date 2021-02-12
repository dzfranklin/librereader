package org.danielzfranklin.librereader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.siegmann.epublib.domain.Book
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookStyle
import org.danielzfranklin.librereader.repo.Repo
import timber.log.Timber

class ReaderViewModel(val bookId: BookID) : ViewModel(), CoroutineScope {
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

    sealed class LoadState {
        object Loading : LoadState()
        data class LoadingHasMeta(val meta: BookMeta) : LoadState()
        data class Loaded(
            val meta: BookMeta,
            val styleUpdates: Flow<BookStyle>,
            val epub: Book,
            val positionProcessor: PositionProcessor
        ) : LoadState()
    }

    private fun loadedState(prev: LoadState.LoadingHasMeta, epub: Book) = LoadState.Loaded(
        prev.meta,
        repo.getBookStyleFlow(bookId),
        epub,
        PositionProcessor(coroutineContext, prev.meta.position)
    )

    private val _state = MutableStateFlow<LoadState>(LoadState.Loading)
    val state = _state.asStateFlow()

    init {
        launch {
            val meta = repo.getBook(bookId)
            _state.value = LoadState.LoadingHasMeta(meta)
        }

        launch {
            val epub = repo.getEpub(bookId)
            val current = _state.value
            if (current is LoadState.LoadingHasMeta) {
                _state.value = loadedState(current, epub)
            } else {
                Timber.w("Got EPUB before meta, waiting")
                _state.collect {
                    if (it is LoadState.LoadingHasMeta) {
                        _state.value = loadedState(it, epub)
                        cancel("done")
                    }
                }
            }
        }
    }

    val showOverview = MutableStateFlow(false)
}