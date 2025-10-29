package com.example.common_base.widget.more

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.example.common_base.util.dp

/**
 *  @author : zhang.wenqiang
 *  @date : 2021/1/28
 *  description :
 */
val RADIUS = 100f.dp()
val OPEN_ANGLE = 120f
val DASH_WIDTH = 2f.dp()
val DASH_LENGTH = 5f.dp()

class DashboardView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mPath: Path = Path()
    private var mDash: Path = Path()
    private lateinit var pathEffect: PathDashPathEffect

    init {
        mPaint.strokeWidth = 3f.dp()
        mPaint.style = Paint.Style.STROKE
        mDash.addRect(0f, 0f,
            DASH_WIDTH,
            DASH_LENGTH, Path.Direction.CCW)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mPath.reset()
        mPath.addArc(
            width / 2f - 150f.dp(), height / 2f - 150f.dp(), width / 2f + 150f.dp(),
            height / 2f + 150f.dp(), 90 + OPEN_ANGLE / 2f, 360 - OPEN_ANGLE
        )
        val pathMeasure = PathMeasure(mPath, false)
        pathEffect =
            PathDashPathEffect(mDash, (pathMeasure.length- DASH_LENGTH) / 20f, 0f, PathDashPathEffect.Style.ROTATE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(mPath, mPaint)

        mPaint.pathEffect = pathEffect
        canvas.drawPath(mPath, mPaint)
        mPaint.pathEffect = null

    }

}