package org.danielzfranklin.librereader.ui.screen.reader

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.util.lruCache

class Renderer(
    outerWidth: Dp,
    outerHeight: Dp,
    padding: Dp,
    baseStyle: TextStyle,
    density: Density,
    fontLoader: Font.ResourceLoader,
    cacheSize: Int = 2,
    val maxSection: Int,
    makeSection: (Int) -> AnnotatedString,
) {
    private val cache: LruCache<Int, SectionRenderer> = lruCache(cacheSize, create = { index ->
        if (index < 0 || index > maxSection) return@lruCache null

        val renderer = SectionRenderer(
            outerWidth,
            outerHeight,
            padding,
            makeSection(index),
            baseStyle,
            density,
            fontLoader
        )

        renderer
    })

    operator fun get(index: Int): SectionRenderer? = cache[index]
}

@Composable
fun rememberRenderer(
    outerWidth: Dp,
    outerHeight: Dp,
    padding: Dp,
    baseStyle: TextStyle,
    density: Density,
    fontLoader: Font.ResourceLoader,
    cacheSize: Int = 2,
    maxSection: Int,
    makeSection: (Int) -> AnnotatedString,
): Renderer = remember(
    outerWidth,
    outerHeight,
    padding,
    baseStyle,
    density,
    fontLoader,
    maxSection,
    makeSection
) {
    Renderer(
        outerWidth,
        outerHeight,
        padding,
        baseStyle,
        density,
        fontLoader,
        cacheSize,
        maxSection,
        makeSection
    )
}