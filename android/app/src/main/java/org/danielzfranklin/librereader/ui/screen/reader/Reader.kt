package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.ui.LocalRepo

@Composable
fun ReaderScreen(bookId: BookID) {
    val repo = LocalRepo.current
    val model = viewModel(
        ReaderModel::class.java,
        key = bookId.toString(),
        factory = ReaderModel.Factory(repo, bookId)
    )

    val book = model.book.collectAsState(null)

    val current = book.value
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.fillMaxWidth(0.5f).aspectRatio(1f))
        }
    } else {
        Pages(model, current)
    }
}

@Composable
@OptIn(ExperimentalCoroutinesApi::class)
fun Pages(model: ReaderModel, book: ReaderModel.Book) {
    val initialPosition = PaginatedTextPosition(book.position.sectionIndex, book.position.charIndex)
    val epub = book.epub

    PaginatedText(
        initialPosition = initialPosition,
        onPosition = { model.updatePosition(BookPosition(epub, it.section, it.charIndex)) },
        makeSection = { epub.section(it)!!.text },
        maxSection = epub.maxSection,
        baseStyle = book.style.toTextStyle(LocalContext.current)
    )
}