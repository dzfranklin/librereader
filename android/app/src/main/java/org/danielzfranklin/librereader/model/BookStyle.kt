package org.danielzfranklin.librereader.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

data class BookStyle(
    val color: PresetColor = PresetColor.Light,
    val typeface: BookTypeface = BookTypeface.DEFAULT,
    val textSizeInSp: Float = 25f,
    val paddingInDp: Int = 10
) {
    fun toTextStyle(context: Context) = TextStyle(
        color = Color(color.text),
        background = Color(color.bg),
        fontFamily = FontFamily(typeface.get(context)),
        fontSize = textSizeInSp.sp,
    )
}
