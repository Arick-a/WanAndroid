package com.example.common_base.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.common_base.R
import kotlin.math.sqrt

/**
 * @describe :
 *
 * @author zwq 2020/8/23
 */
class LoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    var mPaint: Paint? = null
    var mRectf: RectF? = null
    var valueAnimator: ValueAnimator? = null

    var borderWidth = 0f
    val maxRotate = 720f
    var duration = 0
    var maxAngle = 0
    var borderColor = Color.BLACK

    //偏移角度
    var startAngle: Float = 0f

    //转动时角度
    var sweepAngle: Float = 0f

    init {
        mRectf = RectF()
        initProperties(context, attrs)
        createPaint()
    }

    private fun initProperties(context: Context, attrs: AttributeSet?) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.LoadingView)
        borderWidth = typedArray.getDimensionPixelOffset(
            R.styleable.LoadingView_borderWidth,
            context.resources.getDimensionPixelOffset(R.dimen.dp_3)
        ).toFloat()
        borderColor = typedArray.getColor(
            R.styleable.LoadingView_borderColor,
            Color.BLACK
        )
        maxAngle = typedArray.getInt(R.styleable.LoadingView_maxAngle, 100)
        duration = typedArray.getInt(R.styleable.LoadingView_duration, 2000)
        typedArray.recycle()
    }

    private fun createPaint() {
        mPaint = Paint()
        mPaint!!.apply {
            isAntiAlias = true
            isDither = true
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            strokeCap = Paint.Cap.ROUND
        }
    }

    private fun initValueAnimator() {
        if (valueAnimator != null) return

        valueAnimator = ValueAnimator.ofFloat(0f, maxRotate)
        valueAnimator!!.apply {
            duration = this@LoadingView.duration.toLong()
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                startAngle = -90 + value
                // 计算进度条的宽度变化
                sweepAngle = if (value <= maxRotate / 2) {
                    (maxAngle * (valueAnimator!!.animatedFraction * 2))
                } else {
                    (maxAngle * (2 - valueAnimator!!.animatedFraction * 2))
                }
                invalidate()
            }
            repeatCount = ValueAnimator.INFINITE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = measuredHeight

        val radius = if (width > height) height / 2 else width / 2

        mRectf!!.set(
            width / 2 - radius + borderWidth / 2.toFloat(),
            height / 2 - radius + borderWidth / 2.toFloat(),
            width / 2 + radius - borderWidth / 2.toFloat(),
            height / 2 + radius - borderWidth / 2.toFloat()
        )

        // 计算要设置的padding，防止图片的会压到圆上
        calculateCirclePadding((width / 2).coerceAtLeast(height / 2))
    }

    /**
     * 计算最小padding
     *
     * @param radius 最大的半径，因为不保证一定是正方形，所以用最大的半径来进行计算
     */
    private fun calculateCirclePadding(radius: Int) {
        // 得到边长院内正方形的边长的一半
        val rectLength = sqrt((radius * radius shr 1.toDouble().toInt()).toDouble()).toInt()
        // 用现在的padding和已经设置的padding进行比较
        // padding的最小值是圆的半径减去radius
        val minPadding = radius - rectLength
        setPadding(
            if (paddingLeft < minPadding) minPadding else paddingLeft,
            if (paddingTop < minPadding) minPadding else paddingTop,
            if (paddingRight < minPadding) minPadding else paddingRight,
            if (paddingBottom < minPadding) minPadding else paddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(mRectf!!, startAngle, sweepAngle, false, mPaint!!)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initValueAnimator()
        if (!valueAnimator!!.isStarted) {
            valueAnimator!!.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (valueAnimator!!.isStarted) {
            valueAnimator!!.cancel()
            valueAnimator = null
        }
    }
}