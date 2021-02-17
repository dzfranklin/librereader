package org.danielzfranklin.librereader.ui.reader

import android.text.Spanned
import android.view.View
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

fun withPageText(text: Spanned) = WithPageTextMatcher(text)

class WithPageTextMatcher(private val text: Spanned) : BaseMatcher<View>() {
    override fun describeTo(description: Description?) {
        description?.appendText("With text: ${text.toString().trim()}")
    }

    override fun matches(item: Any?): Boolean {
        return item is PageView &&
                item.text.toString().trim() == text.toString().trim()
    }
}