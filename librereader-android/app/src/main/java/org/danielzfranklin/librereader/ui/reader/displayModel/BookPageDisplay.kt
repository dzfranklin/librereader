package org.danielzfranklin.librereader.ui.reader.displayModel

import android.view.ViewGroup
import org.danielzfranklin.librereader.repo.model.BookStyle

data class BookPageDisplay(val width: Int, val height: Int, val style: BookStyle) {
    companion object {
        fun fitParent(parent: ViewGroup, style: BookStyle) = BookPageDisplay(
            parent.width - style.padding * 2,
            parent.height - style.padding * 2,
            style
        )
    }
}