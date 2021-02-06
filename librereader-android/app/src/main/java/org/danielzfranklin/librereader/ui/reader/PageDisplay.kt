package org.danielzfranklin.librereader.ui.reader

import android.view.ViewGroup
import org.danielzfranklin.librereader.repo.model.BookStyle

data class PageDisplay(val width: Int, val height: Int, val style: BookStyle) {
    companion object {
        fun fitParent(parent: ViewGroup, style: BookStyle) = PageDisplay(
            parent.width - style.padding * 2,
            parent.height - style.padding * 2,
            style
        )
    }
}