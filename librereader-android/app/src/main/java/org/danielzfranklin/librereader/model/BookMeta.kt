package org.danielzfranklin.librereader.model

import android.graphics.Bitmap
import androidx.annotation.ColorInt

data class BookMeta(
    val id: BookID,
    val position: BookPosition,
    val style: BookStyle,
    val cover: Bitmap,
    val title: String,
    @ColorInt val coverBgColor: Int,
    @ColorInt val coverTextColor: Int
)