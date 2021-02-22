package org.danielzfranklin.librereader.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

fun Rect.offset() =
    Offset(left, top)

fun Rect.size() =
    Size(width, height)