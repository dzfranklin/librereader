package org.danielzfranklin.librereader.ui.screen.library

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.ui.LibreReaderTopAppBar
import org.danielzfranklin.librereader.ui.LocalRepo
import org.danielzfranklin.librereader.ui.Screen
import org.danielzfranklin.librereader.util.registerForActivityResult

@Composable
fun LibraryScreen(navController: NavHostController) {
    val model = viewModel(
        LibraryModel::class.java,
        factory = LibraryModel.Factory(LocalRepo.current)
    )

    val books: State<List<LibraryModel.Book>?> = model.books.collectAsState(initial = null)

    val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument(), onResult = { uri ->
            if (uri != null) { // if the user cancels
                model.launchImport(uri)
            }
        })

    Column {
        LibreReaderTopAppBar(stringResource(R.string.library)) {
            IconButton({
                openDocumentLauncher.launch(arrayOf(EPUB_MIME))
            }) {
                Icon(
                    painterResource(R.drawable.ic_import_book),
                    stringResource(R.string.import_book)
                )
            }
        }

        val currentBooks = books.value
        if (currentBooks != null) {
            BookShelf(
                currentBooks,
                onNavigateToBook = { id -> navController.navigate(Screen.Reader.path(id)) }
            )
        }
    }
}

// TOOD: Fade and slide the shelf in with https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/animation/animation-core/samples/src/main/java/androidx/compose/animation/core/samples/TransitionSamples.kt

private const val EPUB_MIME = "application/epub+zip"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookShelf(
    books: List<LibraryModel.Book>,
    modifier: Modifier = Modifier,
    onNavigateToBook: (BookID) -> Unit
) {
    val bookWidth = 120.dp
    val bookHeight = bookWidth * 1.8f

    // NOTE: We manually compute the number of columns instead of using GridCells.Adaptive so that
    // we can limit the padding between cells to a fixed value and put the extra padding on the
    // outside

    val bookHorizontalPadding = 4.dp
    val surroundingPadding = 10.dp

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols =
            ((maxWidth - surroundingPadding * 2f) / (bookWidth + bookHorizontalPadding * 2f)).toInt()

        LazyVerticalGrid(
            GridCells.Fixed(cols),
            modifier
                .width((bookWidth + bookHorizontalPadding * 2) * cols.toFloat())
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(surroundingPadding)
        ) {
            for (book in books) {
                item {
                    Book(
                        book,
                        Modifier
                            .size(bookWidth, bookHeight)
                            .padding(vertical = 10.dp, horizontal = bookHorizontalPadding)
                    ) { onNavigateToBook(book.id) }
                }
            }
        }
    }
}

@Composable
fun Book(book: LibraryModel.Book, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = book.bgColor,
        contentColor = book.textColor,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Box(Modifier
            // adding clickable here instead of to the surface ensures the ripple is correctly clipped
            .clickable(onClickLabel = stringResource(R.string.read_book)) { onClick() }
        ) {
            Image(
                book.cover,
                stringResource(R.string.cover),
                Modifier.fillMaxSize(1f),
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.TopCenter,
            )

            Column(Modifier.align(Alignment.BottomCenter)) {
                Box(
                    Modifier
                        .fillMaxWidth(1f)
                        .fillMaxHeight(0.4f)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to book.bgColor
                            ),
                            alpha = 0.9f
                        )
                )

                Box(Modifier.background(SolidColor(book.bgColor), alpha = 0.9f)) {
                    Text(
                        book.title,
                        Modifier
                            .padding(horizontal = 10.dp)
                            .padding(top = 5.dp, bottom = 8.dp)
                            .fillMaxWidth(1f),
                        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                LinearProgressIndicator(
                    progress = book.progress,
                    Modifier
                        .fillMaxWidth(1f)
                        .alpha(0.9f),
                    color = book.textColor,
                    backgroundColor = book.bgColor
                )
            }
        }
    }
}
