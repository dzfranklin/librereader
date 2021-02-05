package org.danielzfranklin.librereader.ui.reader

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.widget.TextView

data class PageStyle(
    val textSize: Float = 25f,
    val textColor: Int = Color.BLACK,
    val bgColor: Int = Color.WHITE,
    val typeface: Typeface = Typeface.DEFAULT,
    val padding: Int = 30
)
