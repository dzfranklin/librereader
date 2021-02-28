package org.danielzfranklin.librereader.ui.screen.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.ui.LocalRepo

@Composable
@OptIn(ExperimentalCoroutinesApi::class)
fun ReaderScreen(bookId: BookID) {
    val repo = LocalRepo.current
    val model = viewModel(
        ReaderModel::class.java,
        key = bookId.toString(),
        factory = ReaderModel.Factory(repo, bookId)
    )

    val book: State<ReaderModel.Book?> = produceState(null) {
        value = model.book.await()
    }

    val current = book.value
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.fillMaxWidth(0.5f).aspectRatio(1f))
        }
    } else {
        val initialPosition = remember {
            PaginatedTextPosition(
                current.position.sectionIndex,
                current.position.charIndex
            )
        }
        val epub = current.epub

        PaginatedText(
            initialPosition = initialPosition,
            onPosition = { model.updatePosition(BookPosition(epub, it.section, it.charIndex)) },
            makeSection = { epub.section(it)!!.text },
            maxSection = epub.maxSection,
            baseStyle = TextStyle(fontSize = 18.sp)
        )
    }
}
