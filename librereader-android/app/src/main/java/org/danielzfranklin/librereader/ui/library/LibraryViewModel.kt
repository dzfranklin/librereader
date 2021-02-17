package org.danielzfranklin.librereader.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.util.atomicUpdate

class LibraryViewModel : ViewModel(), CoroutineScope {
    override val coroutineContext = viewModelScope.coroutineContext

    private val repo = Repo.get()

    val books = repo.listBooks()

    private val _importsInProgress = MutableStateFlow(0)
    val isImportInProgress = _importsInProgress
        .map { it > 0 }

    suspend fun import(uri: Uri) {
        _importsInProgress.atomicUpdate { it + 1 }
        repo.importBook(uri)
        _importsInProgress.atomicUpdate { it - 1 }
    }

    suspend fun getCover(id: BookID) =
        repo.getCover(id)
}