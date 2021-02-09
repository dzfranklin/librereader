package org.danielzfranklin.librereader.repo.model

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.widget.TextView
import androidx.core.view.setPadding
import kotlin.math.roundToInt

/**
 * @param textSize Unit is dp
 * @param padding Unit is dp
 */
data class BookStyle(
    val textColor: Int = Color.BLACK,
    val bgColor: Int = Color.WHITE,
    val typeface: Typeface = Typeface.DEFAULT,
    // Private because users need pixels, which must be computed based on DPI
    private val textSize: Float = 10f,
    private val padding: Int = 10
) {
    fun apply(view: TextView) {
        view.textSize = computeTextSizePixels(view.context)
        view.setTextColor(textColor)
        view.typeface = typeface
        view.setPadding(computePaddingPixels(view.context))
        view.requestLayout()
    }

    fun toPaint(context: Context): TextPaint {
        val view = TextView(context)
        apply(view)
        return view.paint
    }

    fun computePaddingPixels(context: Context) =
        (getMetrics(context).density * padding.toFloat()).roundToInt()

    fun computeTextSizePixels(context: Context) =
        getMetrics(context).scaledDensity * textSize

    private fun getMetrics(context: Context) = context.resources.displayMetrics
}
