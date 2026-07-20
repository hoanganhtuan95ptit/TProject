package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ReplacementSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import kotlin.math.ceil
import kotlin.math.floor

data class BigRoundedOutline(
    val textSize: Float,
    val paddingHorizontal: Float = 0f,
    val paddingVertical: Float = 0f,
    val marginHorizontal: Float = 0f,
    val marginVertical: Float = 0f,
    val textColor: Int? = null,
    val strokeColor: Int,
    val strokeWidth: Float = 1f,
    val cornerRadius: Float = 0f,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f
) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigRoundedOutlineConvert : BigImageSpanConvert {

    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return (bigSpan as? BigRoundedOutline)?.let(::RoundedOutlineAndroidSpan)
    }
}

private class RoundedOutlineAndroidSpan(
    private val span: BigRoundedOutline
) : ReplacementSpan() {

    private val rect = RectF()

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {

        val measurePaint = createTextPaint(paint)
        val width = measureText(
            paint = measurePaint,
            text,
            start,
            end
        ) + getHorizontalInsets()

        updateFontMetrics(measurePaint, fm)

        return ceil(width.toDouble()).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {

        val textPaint = createTextPaint(paint)
        val textWidth = measureText(
            paint = textPaint,
            text = text,
            start = start,
            end = end
        )

        val rect = buildOutlineRect(
            textPaint = textPaint,
            x = x,
            baseline = y,
            textWidth = textWidth
        )

        configureOutlinePaint()
        canvas.drawRoundRect(
            rect,
            span.cornerRadius,
            span.cornerRadius,
            outlinePaint
        )

        if (text == null) return

        canvas.drawText(
            text,
            start,
            end,
            rect.left + span.paddingHorizontal,
            y.toFloat(),
            textPaint
        )
    }

    private fun createTextPaint(source: Paint) = TextPaint(source).apply {

        textSize = span.textSize
        span.textColor?.let { color = it }
    }

    private fun measureText(
        paint: TextPaint,
        text: CharSequence?,
        start: Int,
        end: Int
    ): Float {

        return text?.let { paint.measureText(it, start, end) } ?: 0f
    }

    private fun getHorizontalInsets(): Float {

        return ((span.paddingHorizontal + span.marginHorizontal) * 2f) + getStrokeWidth()
    }

    private fun updateFontMetrics(
        paint: TextPaint,
        fm: Paint.FontMetricsInt?
    ) {

        fm ?: return

        val fontMetrics = paint.fontMetrics
        val strokeInset = getStrokeInset()

        fm.ascent = floor(
            fontMetrics.ascent - span.paddingVertical - span.marginVertical - strokeInset
        ).toInt()
        fm.descent = ceil(
            fontMetrics.descent + span.paddingVertical + span.marginVertical + strokeInset
        ).toInt()
        fm.top = fm.ascent
        fm.bottom = fm.descent
    }

    private fun buildOutlineRect(
        textPaint: TextPaint,
        x: Float,
        baseline: Int,
        textWidth: Float
    ): RectF {

        val fontMetrics = textPaint.fontMetrics
        val strokeInset = getStrokeInset()

        val rectLeft = x + span.marginHorizontal + strokeInset
        val rectRight = rectLeft + textWidth + (span.paddingHorizontal * 2f)
        val rectTop = baseline + fontMetrics.ascent - span.paddingVertical
        val rectBottom = baseline + fontMetrics.descent + span.paddingVertical

        rect.set(
            rectLeft,
            rectTop,
            rectRight,
            rectBottom
        )

        return rect
    }

    private fun getStrokeWidth(): Float {

        return span.strokeWidth.coerceAtLeast(0f)
    }

    private fun getStrokeInset(): Float {

        return getStrokeWidth() / 2f
    }

    private fun configureOutlinePaint() {

        outlinePaint.apply {

            style = Paint.Style.STROKE

            color = span.strokeColor

            strokeWidth = getStrokeWidth()

            pathEffect = if (
                span.dashWidth > 0f &&
                span.dashGap > 0f
            ) {
                DashPathEffect(
                    floatArrayOf(span.dashWidth, span.dashGap),
                    0f
                )
            } else {
                null
            }
        }
    }
}
