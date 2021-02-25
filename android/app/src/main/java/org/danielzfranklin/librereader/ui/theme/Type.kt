package org.danielzfranklin.librereader.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.unit.sp

private val defaultTypography = Typography()
val typography = Typography(
    body1 = defaultTypography.body1.copy(fontSize = 15.sp)
)