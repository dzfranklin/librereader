package org.danielzfranklin.librereader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.InputStream

class ReaderViewModel(application: Application, source: InputStream) :
    AndroidViewModel(application) {
    class Factory(
        private val application: Application,
        private val source: InputStream
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass == ReaderViewModel::javaClass) {
                throw IllegalArgumentException("Can only create ReaderViewModel")
            }
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel(application, source) as T
        }

    }

    val book = BookDisplay(getApplication(), source, viewModelScope.coroutineContext)

    val initialPosition: BookPosition = BookPosition.Position(2, 0)
}