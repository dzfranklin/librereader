package org.danielzfranklin.librereader.model

import kotlinx.coroutines.flow.StateFlow
import nl.siegmann.epublib.domain.Book

data class Book(
    val id: BookID,
    val style: BookStyle,
    val position: StateFlow<BookPosition>,
    val epub: Book
)