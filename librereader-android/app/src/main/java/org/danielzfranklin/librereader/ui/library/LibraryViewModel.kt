package org.danielzfranklin.librereader.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import org.danielzfranklin.librereader.repo.Repo

class LibraryViewModel: ViewModel() {
    private val repo = Repo.get()

    val books = repo.listBooks()

    suspend fun import(uri: Uri) =
        repo.importBook(uri)
}