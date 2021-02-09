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
import org.danielzfranklin.librereader.repo.model.BookStyle
import org.danielzfranklin.librereader.ui.reader.pagesView.PagesView
import kotlin.math.round
import kotlin.math.roundToInt

class PageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    var manager: PagesView? = null

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

    fun displaySpan(span: Spanned?) {
        text = span?.trimEnd()
    }

    private var _style: BookStyle = BookStyle()
    var style: BookStyle
        get() = _style
        set(style) {
            this._style = style
            style.apply(this)
        }

    private var _percentTurned = 1f
    var percentTurned: Float
        get() = _percentTurned
        set(percent) {
            if (_percentTurned != percent) {
                _percentTurned = percent
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
            val edge = (width + edgeWidth + edgeShadowWidth).toFloat() * percentTurned
            drawText(canvas, edge)
            drawTurnShadow(canvas, edge)
        } else {
            drawText(canvas)
        }
    }

    @SuppressLint("WrongCall")
    private fun drawText(canvas: Canvas) {
        canvas.drawColor(style.bgColor)
        super.onDraw(canvas)
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

    private fun drawTurnShadow(canvas: Canvas, edge: Float) {
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