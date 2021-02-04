package org.danielzfranklin.librereader.ui.reader

import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import timber.log.Timber

class BookURLSpan(private val link: String): ClickableSpan() {
    override fun onClick(widget: View) {
        Timber.i("Clicked link %s", link)
    }
}