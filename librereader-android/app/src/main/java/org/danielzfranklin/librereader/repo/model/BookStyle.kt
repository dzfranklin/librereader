package org.danielzfranklin.librereader.repo.model

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.widget.TextView

data class BookStyle(
    val textSize: Float = 25f,
    val textColor: Int = Color.BLACK,
    val bgColor: Int = Color.WHITE,
    val typeface: Typeface = Typeface.DEFAULT,
    val padding: Int = 30
) {
    fun apply(view: TextView) {
        view.textSize = textSize
        view.setTextColor(textColor)
        view.typeface = typeface
        view.setPadding(padding, padding, padding, padding)
        view.requestLayout()
    }

    fun toPaint(context: Context): TextPaint {
        val view = TextView(context)
        apply(view)
        return view.paint
    }
}
