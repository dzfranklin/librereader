package org.danielzfranklin.librereader.model

import androidx.annotation.ColorInt

data class BookMeta(
    val id: BookID,
    val position: BookPosition,
    val style: BookStyle,
    val title: String,
    @ColorInt val coverBgColor: Int,
    @ColorInt val coverTextColor: Int
)