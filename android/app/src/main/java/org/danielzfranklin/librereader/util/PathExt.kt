package org.danielzfranklin.librereader.util

import android.graphics.RectF
import android.graphics.Region
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.toRegion

fun Path.contains(offset: IntOffset): Boolean {
    // See <https://stackoverflow.com/a/10586689>
    val path = asAndroidPath()
    val bounds = RectF()
    path.computeBounds(bounds, true)
    val region = Region().apply {
        setPath(path, bounds.toRegion())
    }

    return region.contains(offset.x, offset.y)
}