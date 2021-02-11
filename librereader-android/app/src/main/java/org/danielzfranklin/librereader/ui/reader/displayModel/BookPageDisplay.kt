package org.danielzfranklin.librereader.ui.reader.displayModel

import android.view.ViewGroup
import org.danielzfranklin.librereader.model.BookStyle

data class BookPageDisplay(val width: Int, val height: Int, val style: BookStyle) {
    companion object {
        fun fitParent(parent: ViewGroup, style: BookStyle): BookPageDisplay {
            val padding = style.computePaddingPixels(parent.context)
            return BookPageDisplay(
                parent.width - padding * 2,
                parent.height - padding * 2,
                style
            )
        }
    }
}