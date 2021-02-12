package org.danielzfranklin.librereader.model

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

data class BookMeta(
    val id: BookID,
    val cover: Drawable,
    val title: String,
    val position: BookPosition,
    @ColorInt val coverBgColor: Int,
    @ColorInt val coverTextColor: Int
)