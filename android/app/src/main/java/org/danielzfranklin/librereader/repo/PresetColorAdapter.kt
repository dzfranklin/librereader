package org.danielzfranklin.librereader.repo

import com.squareup.sqldelight.ColumnAdapter
import org.danielzfranklin.librereader.model.PresetColor

class PresetColorAdapter: ColumnAdapter<PresetColor, String> {
    override fun decode(databaseValue: String) =
        when (databaseValue) {
            DARK -> PresetColor.Dark
            SEPIA -> PresetColor.Sepia
            LIGHT -> PresetColor.Light
            else -> throw IllegalArgumentException("Unrecognized color $databaseValue")
        }

    override fun encode(value: PresetColor) =
        when (value) {
            is PresetColor.Dark -> DARK
            is PresetColor.Sepia -> SEPIA
            is PresetColor.Light -> LIGHT
        }

    companion object {
        private const val DARK = "dark"
        private const val LIGHT = "light"
        private const val SEPIA = "sepia" }
}