package org.danielzfranklin.librereader.util

import kotlin.math.max
import kotlin.math.min

/** Clamp to min <= this <= max */
fun Float.clamp(min: Float, max: Float) =
    max(min, min(max, this))