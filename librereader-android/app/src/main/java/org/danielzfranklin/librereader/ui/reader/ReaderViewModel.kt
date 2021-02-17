package org.danielzfranklin.librereader.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private val inOverview = MutableStateFlow(false)

    sealed class Progress {
        object Loading : Progress()
        data class LoadingHasCover(val cover: Bitmap) : Progress()
        data class Loaded(val data: ReaderFragment.DisplayIndependentData) : Progress()
    }

    private val _progress = MutableStateFlow<Progress>(Progress.Loading)

    /** Guarantee: Will never change after Loaded */
    val progress = _progress.asStateFlow()

    init {
        launch {
            val cover = async { repo.getCover(id) }
            val epub = async { repo.getEpub(id) }
            val position = async { repo.getPosition(id) }
            val style = async { repo.getBookStyleFlow(id).stateIn(this@launch) }
            _progress.value = Progress.LoadingHasCover(cover.await()!!)
            _progress.value = Progress.Loaded(
                ReaderFragment.DisplayIndependentData(
                    id,
                    style.await().map { it!! }.stateIn(this),
                    PositionProcessor(coroutineContext, position.await()!!),
                    epub.await()!!,
                    inOverview
                )
            )
        }
    }
}