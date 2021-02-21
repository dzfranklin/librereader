package org.danielzfranklin.librereader.ui.screen.library

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.mapLatest
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.repo.Repo

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryModel(private val repo: Repo) : ViewModel(), CoroutineScope {
    class Factory(private val repo: Repo) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass != LibraryModel::class.java) {
                throw IllegalArgumentException("Can only create LibraryModel, not $modelClass")
            }
            return LibraryModel(repo) as T
        }
    }

    override val coroutineContext = viewModelScope.coroutineContext

    data class Book(
        val id: BookID,
        val title: String,
        val textColor: Color,
        val bgColor: Color,
        val cover: ImageBitmap,
        val progress: Float
    )

    val books = repo.listBooks()
        .mapLatest { metas ->
            metas
                .map {
                    async {
                        val cover = repo.getCover(it.id)!!.asImageBitmap()
                        Book(
                            it.id,
                            it.title,
                            Color(it.coverTextColor),
                            Color(it.coverBgColor),
                            cover,
                            it.position.percent
                        )
                    }
                }
                .awaitAll()
        }

    /** Launches in global scope to ensure leaving the screen doesn't cancel */
    fun launchImport(uri: Uri) {
        launch {
            repo.importBook(uri)
        }
    }
}