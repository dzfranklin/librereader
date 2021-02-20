package org.danielzfranklin.librereader.model

import android.content.Context
import android.text.TextPaint
import android.widget.TextView
import androidx.core.view.setPadding
import kotlin.math.roundToInt

data class BookStyle(
    val color: PresetColor = PresetColor.Light,
    val typeface: BookTypeface = BookTypeface.DEFAULT,
    val textSizeInSp: Float = 10f,
    val paddingInDp: Int = 10
) {
    fun apply(view: TextView) {
        view.textSize = computeTextSizePixels(view.context)
        view.setTextColor(color.text)
        view.typeface = typeface.get(view.context)
        view.setPadding(computePaddingPixels(view.context))
        view.requestLayout()
    }

    fun toPaint(context: Context): TextPaint {
        val view = TextView(context)
        apply(view)
        return view.paint
    }

    fun computePaddingPixels(context: Context) =
        (getMetrics(context).density * paddingInDp.toFloat()).roundToInt()

    fun computeTextSizePixels(context: Context) =
        getMetrics(context).scaledDensity * textSizeInSp

    private fun getMetrics(context: Context) = context.resources.displayMetrics
}
