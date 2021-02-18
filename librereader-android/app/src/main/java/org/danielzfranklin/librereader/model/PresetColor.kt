package org.danielzfranklin.librereader.model

import android.graphics.Color

sealed class PresetColor(text: String, bg: String) {
    val text = Color.parseColor(text)
    val bg = Color.parseColor(bg)

    object Light : PresetColor("#000000", "#FFFFFF")
    object Sepia : PresetColor("#29220F", "#817046")
    object Dark : PresetColor("#818181", "#121212")
}