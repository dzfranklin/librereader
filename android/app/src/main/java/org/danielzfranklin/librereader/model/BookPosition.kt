package org.danielzfranklin.librereader.model

import kotlin.math.round

data class BookPosition(
    val id: BookID,
    val percent: Float,
    val sectionIndex: Int,
    val charIndex: Int
) {
    override fun toString(): String {
        return "BookPosition $sectionIndex/$charIndex $id (${round(percent * 100 * 100f) / 100f}%)"
    }
}