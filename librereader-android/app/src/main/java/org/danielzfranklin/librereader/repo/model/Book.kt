package org.danielzfranklin.librereader.repo.model

import kotlinx.coroutines.flow.MutableStateFlow
import nl.siegmann.epublib.domain.Book

data class Book(
    val style: MutableStateFlow<BookStyle>,
    val position: MutableStateFlow<BookPosition>,
    val epub: Book
)