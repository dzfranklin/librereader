package org.danielzfranklin.librereader.repo.model

import kotlinx.coroutines.flow.MutableStateFlow
import nl.siegmann.epublib.domain.Book

data class Book(
    val style: BookStyle,
    val position: BookPosition,
    val epub: Book
)