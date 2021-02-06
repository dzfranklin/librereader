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
import kotlin.math.round

class PageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    private var manager: ReaderPagesView? = null

    constructor(
        context: Context,
        manager: ReaderPagesView,
        text: Spanned,
        style: PageStyle,
        percentTurned: Float
    ) : this(context) {
        this.style = style
        this.text = text
        this.manager = manager

        // don't set this if already set to default to avoid redraw
        if (percentTurned != this.percentTurned) {
            this.percentTurned = percentTurned
        }
    }

    init {
        focusable = FOCUSABLE
        scrollIndicators = 0
        setTextIsSelectable(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        manager?.gestureDetector?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private var _style: PageStyle = PageStyle()
    var style: PageStyle
        get() = _style
        set(style) {
            this._style = style

            textSize = style.textSize
            setTextColor(style.textColor)
            typeface = style.typeface
            setPadding(style.padding, style.padding, style.padding, style.padding)

            requestLayout()
        }

    private var _percentTurned = 1f
    var percentTurned: Float
        get() = _percentTurned
        set(percent) {
            _percentTurned = percent
            invalidate()
            requestLayout()
        }

    private val edgeWidth = 4
    private val edgeShadowWidth: Int
        get() {
            // Goal: constant 60 until 70%, then linear from 60 to 0 from 70% to 100%
            return if (percentTurned < .7f) {
                60
            } else {
                // convert percent to percent between 70% and 100%
                val convertedPercent = (1f - percentTurned) / 0.3f
                round(60f * convertedPercent).toInt()
            }
        }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        val edge = (width + edgeWidth + edgeShadowWidth).toFloat() * percentTurned

        drawText(canvas, edge)
        drawEdge(canvas, edge)
    }

    // super.draw instead of super.onDraw causes infinite loop. TODO: Investigate?
    @SuppressLint("WrongCall")
    private fun drawText(canvas: Canvas, edge: Float) {
        // We draw the background ourselves because the default implementation would draw it past
        // the edge

        canvas.withClip(0f, 0f, edge, height.toFloat()) {
            canvas.drawColor(style.bgColor)
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

    private fun drawEdge(canvas: Canvas, edge: Float) {
        val lineX = round(edge + 1f)

        canvas.drawLine(
            lineX,
            0f,
            lineX,
            height.toFloat(),
            edgePaint
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val colors = intArrayOf(Color.LTGRAY, Color.TRANSPARENT)
            edgeShadow.setColors(colors, null)

            val startX = lineX.toInt() + (edgeWidth / 2)
            edgeShadow.setBounds(startX, 0, startX + edgeShadowWidth, height)

            edgeShadow.draw(canvas)
        }
    }
}