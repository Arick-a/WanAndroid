package com.example.common_base.widget.more

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.example.common_base.util.dp

/**
 *  @author : zhang.wenqiang
 *  @date : 2021/1/28
 *  description :
 */
class TextViewDemo(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = Rect()
    private val fontMetrics = Paint.FontMetrics()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPaint.color = Color.BLACK
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.style = Paint.Style.FILL
        mPaint.textSize = 20f.dp()
        mPaint.strokeWidth = 1f.dp()

        canvas?.drawLine(0f, height / 2f, width.toFloat(), height / 2f, mPaint)

        mPaint.getFontMetrics(fontMetrics)
        mPaint.getTextBounds("ab国aaqq", 0, "ab国aaqq".length, bounds)
        mPaint.strokeWidth = 5f.dp()
//        canvas?.drawText("ab国aaqq", width / 2f, height / 2f, mPaint)
//        canvas?.drawText("ab国aaqq", width / 2f, height / 2f - (bounds.top - bounds.bottom) / 2, mPaint)
        canvas?.drawText("ab国aaqq", width / 2f, height / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2, mPaint)

        mPaint.textAlign = Paint.Align.LEFT
        mPaint.getFontMetrics(fontMetrics)
        canvas?.drawText("ab国aaqq", 0f, 0f, mPaint)
        canvas?.drawText("ab国aaqq", 0f, -fontMetrics.top, mPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = 100
        resolveSize(size,widthMeasureSpec)
    }

}