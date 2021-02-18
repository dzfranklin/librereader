package org.danielzfranklin.librereader.ui.reader.stylePicker

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import org.danielzfranklin.librereader.R
import kotlin.math.roundToInt

class PageColorsButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleRes) {

    private val defaultTextColor = Color.BLACK
    private val defaultBgColor = Color.WHITE

    var textColor: Int = defaultTextColor
        set(value) {
            field = value
            invalidate()
        }

    var bgColor: Int = defaultBgColor
        set(value) {
            field = value
            invalidate()
        }

    init {
        val styles = context.obtainStyledAttributes(attrs, R.styleable.PageColorsButton)
        try {
            textColor = styles.getColor(R.styleable.PageColorsButton_textColor, defaultTextColor)
            bgColor = styles.getColor(R.styleable.PageColorsButton_bgColor, defaultBgColor)
        } finally {
            styles.recycle()
        }
    }

    private val paddingY = context.resources.getDimension(R.dimen.pageColorButtonPaddingY)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // NOTE: We violate some of the obligations of our contract for simplicity. This shouldn't
        // be an issue as long as parents always give us as much space as we want
        setMeasuredDimension(
            context.resources.getDimension(R.dimen.pageColorButtonWidth).roundToInt(),
            (context.resources.getDimension(R.dimen.pageColorButtonHeight) + paddingY).roundToInt()
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        isSelected = !isSelected
        return true
    }

    private val letter = context.resources.getString(R.string.sampleLetter)

    private val letterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface =
            Typeface.create(context.resources.getFont(R.font.crimson_pro), Typeface.BOLD_ITALIC)
        textSize = context.resources.getDimension(R.dimen.pageColorButtonTextSize)
    }

    private val letterWidth: Float
    private val letterHeight: Float

    init {
        val letterBounds = Rect()
        letterPaint.getTextBounds(letter, 0, 1, letterBounds)
        letterWidth = letterBounds.right.toFloat() - letterBounds.left.toFloat()
        letterHeight = letterBounds.bottom.toFloat() - letterBounds.top.toFloat()
    }

    private val borderRadius = context.resources.getDimension(R.dimen.pageColorButtonRadius)
    private val border = Path()
    private val borderThickness =
        context.resources.getDimension(R.dimen.pageColorButtonBorderThickness)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = borderThickness
        style = Paint.Style.STROKE
    }

    private val inset = borderThickness / 2f

    private val selectedBorderColor = context.getColor(R.color.colorAccent)

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return

        val fWidth = width.toFloat()
        val fHeight = height.toFloat() - paddingY

        border.apply {
            fillType = Path.FillType.WINDING

            // start top middle
            moveTo(fWidth / 2f, inset)

            // top right corner
            lineTo(fWidth - borderRadius - inset, inset)
            rCubicTo(borderRadius, 0f, borderRadius, 0f, borderRadius, borderRadius)

            // bottom right corner
            lineTo(fWidth - inset, fHeight - borderRadius - inset)
            rCubicTo(0f, borderRadius, 0f, borderRadius, -borderRadius, borderRadius)

            // bottom left corner
            lineTo(borderRadius + inset, fHeight - inset)
            rCubicTo(-borderRadius, 0f, -borderRadius, 0f, -borderRadius, -borderRadius)

            // top left corner
            lineTo(inset, borderRadius + inset)
            rCubicTo(0f, -borderRadius, 0f, -borderRadius, borderRadius, -borderRadius)

            // back to start
            lineTo(fWidth / 2f, inset)
        }

        canvas.withTranslation(0f, paddingY / 2f) {
            canvas.save()
            canvas.clipPath(border)
            canvas.drawColor(bgColor)
            canvas.restore()

            letterPaint.color = textColor
            canvas.drawText(
                letter,
                (fWidth - letterWidth) / 2,
                // 2.4 is a magic number that biases the letter upwards to account for visual density
                letterHeight + (fHeight - letterHeight) / 2.4f,
                letterPaint
            )

            if (isSelected) {
                borderPaint.color = selectedBorderColor
            } else {
                borderPaint.color = textColor
                borderPaint.alpha = (255f * 0.7f).roundToInt() // NOTE: modifies color
            }
            canvas.drawPath(border, borderPaint)
        }
    }
}