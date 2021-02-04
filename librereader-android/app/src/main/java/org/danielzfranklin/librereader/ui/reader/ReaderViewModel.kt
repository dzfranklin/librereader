package org.danielzfranklin.librereader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.InputStream

class ReaderViewModel(source: InputStream) : ViewModel() {
    class Factory(private val source: InputStream) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass == ReaderViewModel::javaClass) {
                throw IllegalArgumentException("Can only create ReaderViewModel")
            }
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel(source) as T
        }

    }

    val book = Book(source, viewModelScope.coroutineContext)

    val initialPosition: BookPosition = BookPosition.Position(2, 0)
}