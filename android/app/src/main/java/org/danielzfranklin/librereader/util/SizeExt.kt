package org.danielzfranklin.librereader.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

fun Size.round() =
    IntSize(width.roundToInt(), height.roundToInt() )