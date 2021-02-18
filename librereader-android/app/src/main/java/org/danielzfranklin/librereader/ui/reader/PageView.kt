package org.danielzfranklin.librereader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.Spanned
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.model.BookStyle
import kotlin.math.round
import kotlin.math.roundToInt

class PageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        focusable = FOCUSABLE
        scrollIndicators = 0
        post {
            // NOTE: This involves measuring the text layout, and as such is expensive
            // so we defer it to after the first render
            setTextIsSelectable(true)
        }
    }

    var propagateTouchEventsTo: ((event: MotionEvent?) -> Boolean)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val superResult = super.onTouchEvent(event)
        val result = propagateTouchEventsTo?.let { it(event) } ?: false
        return superResult || result
    }

    fun displaySpan(span: Spanned?) {
        setText(span, BufferType.SPANNABLE)
    }

    var style: BookStyle = BookStyle()
        set(style) {
            if (field != style) {
                style.apply(this)
                field = style
            }
        }

    init {
        style.apply(this)
    }

    var percentTurned = 1f
        set(percent) {
            if (field != percent) {
                field = percent
                invalidate()
                requestLayout()
            }
        }

    private val edgeWidth = 4
    private val maxShadowWidth = context.resources.getDimension(R.dimen.maxPageTurnShadow)
    private val edgeShadowWidth: Int
        get() {
            // Goal: constant 60 until 70%, then linear from maxShadowWidth to 0 from 70% to 100%
            return if (percentTurned < .7f) {
                maxShadowWidth.roundToInt()
            } else {
                // convert percent to percent between 70% and 100%
                val convertedPercent = (1f - percentTurned) / 0.3f
                (maxShadowWidth * convertedPercent).roundToInt()
            }
        }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        if (percentTurned > 0f) {
            val totalEdge = (edgeWidth + edgeShadowWidth).toFloat()
            val edge = (width.toFloat() + totalEdge) * percentTurned - totalEdge
            drawText(canvas, edge)
            drawTurnShadow(canvas, edge)
        } else {
            drawText(canvas)
        }
    }

    @SuppressLint("WrongCall")
    private fun drawText(canvas: Canvas) {
        canvas.drawColor(style.color.bg)
        super.onDraw(canvas)
    }

    // super.draw instead of super.onDraw causes infinite loop. TODO: Investigate?
    @SuppressLint("WrongCall")
    private fun drawText(canvas: Canvas, edge: Float) {
        // We draw the background ourselves because the default implementation would draw it past
        // the edge

        canvas.withClip(0f, 0f, edge, height.toFloat()) {
            canvas.drawColor(style.color.bg)
        }

        canvas.withTranslation(-(width - edge), 0f) {
            super.onDraw(canvas)
        }
    }

    private val edgePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = edgeWidth.toFloat()
    }
    private val edgeShadow = GradientDrawable().apply {
        orientation = GradientDrawable.Orientation.LEFT_RIGHT
        mutate()
    }

    private fun drawTurnShadow(canvas: Canvas, edge: Float) {
        val lineX = round(edge + 1f)

        canvas.drawLine(
            lineX,
            0f,
            lineX,
            height.toFloat(),
            edgePaint
        )

        edgeShadow.colors = intArrayOf(Color.LTGRAY, Color.TRANSPARENT)

        val startX = lineX.toInt() + (edgeWidth / 2)
        edgeShadow.setBounds(startX, 0, startX + edgeShadowWidth, height)

        edgeShadow.draw(canvas)
    }

    init {
        if (isInEditMode) {
            text = context.getText(R.string.preview_book_text)
            percentTurned = 0.7f
        }
    }
}