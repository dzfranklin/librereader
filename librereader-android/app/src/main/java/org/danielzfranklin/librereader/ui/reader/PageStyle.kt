package org.danielzfranklin.librereader.ui.reader

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.widget.TextView

data class PageStyle(
    val textSize: Float = 20f,
    val textColor: Int = Color.LTGRAY,
    val bgColor: Int = Color.BLACK,
    val typeface: Typeface = Typeface.MONOSPACE
) {
    fun toPaint(): TextPaint {
        val paint = TextPaint()

        paint.textSize = textSize * 4
        paint.color = textColor
        paint.bgColor = bgColor
        paint.typeface = typeface

        return paint
    }
}
