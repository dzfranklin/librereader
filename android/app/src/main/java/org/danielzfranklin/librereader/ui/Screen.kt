package org.danielzfranklin.librereader.ui

import org.danielzfranklin.librereader.model.BookID

sealed class Screen(val route: String) {
    object Library : Screen("library") {
        fun path() = "library"
    }

    object Reader : Screen("reader/{bookId}") {
        fun path(bookID: BookID) = "reader/$bookID"
    }
}