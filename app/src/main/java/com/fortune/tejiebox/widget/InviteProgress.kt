package com.fortune.tejiebox.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.fortune.tejiebox.R
import com.orhanobut.logger.Logger
import kotlin.math.tan

/**
 *
 * @author : YingYing Zhang
 * @e-mail : 540108843@qq.com
 * @time   : 2022-09-23
 * @desc   : 斑马波纹条 view 第三版, 第二版也不知道去哪了, 反正感觉就该是 v3
 *
 */
class InviteProgress constructor(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {
    private val paint: Paint = Paint().apply {
        color = resources.getColor(R.color.red_F03D3D)
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private var itemWidth = 40

    private var isRadius = true

    private var xPoint = 0
    private var valueAnimator: ValueAnimator? = null

    // 圆角 path
    private var roundRectPath: Path? = null

    // 斑马条纹 path
    private var stripPath: Path? = null


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val curHeight = measuredHeight
        if (curHeight == 0) {
            throw Exception("LiveLikeColorfulViewV3 onDraw height is 0!!!")
        }
        if (COLORFUL_VIEW_RADIUS > 90.0) {
            throw Exception("LiveLikeColorfulViewV3 onDraw radius must be in (0, 90]")
        }

        // 现在倾斜角度是 45度, 通过 tan45 = 1来计算坐标点值, delta 临边
        val delta = curHeight / tan(Math.toRadians(COLORFUL_VIEW_RADIUS))
        Logger.d("LiveLikeColorfulView onDraw delta = $delta, height = $curHeight")

        /**
         * 绘制斑马条纹
         * 以 xPoint 中心, 左右两边画斑波纹条, xPoint 根据属性动画动态变化
         */
        val stripPath = getStripPath()
        // 画 xPoint 左边线条
        var tempXPoint = xPoint
        while (tempXPoint >= -delta) {
            val x = tempXPoint.toFloat()
            val y = curHeight.toFloat()

            stripPath.moveTo(x, y)
            stripPath.lineTo(x, y)
            stripPath.lineTo((x + delta).toFloat(), 0F)
            stripPath.lineTo((x + delta - itemWidth).toFloat(), 0F)
            stripPath.lineTo(x - itemWidth, y)
            stripPath.lineTo(x, y)

            tempXPoint -= 2 * itemWidth
        }
        // 画 xPoint 右边线条
        tempXPoint = xPoint
        while (tempXPoint <= measuredWidth + delta) {
            val x = tempXPoint.toFloat()
            val y = curHeight.toFloat()

            stripPath.moveTo(x, y)
            stripPath.lineTo(x, y)
            stripPath.lineTo((x + delta).toFloat(), 0F)
            stripPath.lineTo((x + delta - itemWidth).toFloat(), 0F)
            stripPath.lineTo(x - itemWidth, y)
            stripPath.lineTo(x, y)

            tempXPoint += 2 * itemWidth
        }
        stripPath.close()

        paint.color = Color.parseColor("#80FB6767")
        canvas?.drawPath(getRoundRectPath(), paint)

        // clipxx 方法只对设置以后的 drawxx 起作用，已经画出来的图形, 是不会有作用的
        // 也就是说 clipxx需要先调用!!
        if (isRadius) {
            canvas?.clipPath(getRoundRectPath())
        }

        paint.color = Color.parseColor("#FB6767")
        canvas?.drawPath(stripPath, paint)
    }

    fun startAnimation() {
        if (isRunning()) return
        val distance = measuredWidth
        valueAnimator = ValueAnimator.ofInt(0, distance)
        val time = distance / COLORFUL_VIEW_X_DEFAULT_SPEED * 1_000
        valueAnimator?.duration = time
        Logger.d(
            "live colorful view v3 -> distance = $distance, " +
                    "time = $time, " +
                    "speed = $COLORFUL_VIEW_X_DEFAULT_SPEED"
        )
        valueAnimator?.repeatCount = ValueAnimator.INFINITE
        valueAnimator?.addUpdateListener {
            xPoint = (it?.animatedValue ?: 0) as Int
            invalidate()
        }
        valueAnimator?.interpolator = LinearInterpolator()
        valueAnimator?.start()
    }

    private fun getRoundRectPath(isForced: Boolean = false): Path {
        if (roundRectPath == null || isForced) {
            roundRectPath = Path()
            Logger.d("zyy only init once round rect path:height=$height")
            val rect = RectF(0F, 0F, width.toFloat(), height.toFloat())
            roundRectPath?.addRoundRect(
                rect,
                height.toFloat() / 2,
                height.toFloat() / 2,
                Path.Direction.CW
            )
        }
        return roundRectPath ?: Path()
    }

    private fun getStripPath(): Path {
        if (stripPath == null) {
            Logger.d("zyy only init once strip path")
            stripPath = Path()
        }
        stripPath?.reset()
        return stripPath ?: Path()
    }

    fun configChanged() {
        getRoundRectPath(true)
    }

    fun stopAnimation() {
        if (isRunning()) {
            valueAnimator?.end()
        }
    }

    fun isRunning() = valueAnimator?.isRunning == true

    fun setRoundRadius() {
        isRadius = true
        invalidate()
    }

    fun resetRadius() {
        isRadius = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        valueAnimator?.removeAllUpdateListeners()
    }

    companion object {
        // 斑马波纹内部移动速度, 标准速度: 每秒移动 400px
        private const val COLORFUL_VIEW_X_DEFAULT_SPEED = 250L

        // 斑马条纹倾斜角度
        var COLORFUL_VIEW_RADIUS = 45.0
    }
}