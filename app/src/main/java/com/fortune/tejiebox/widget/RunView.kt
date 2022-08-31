package com.fortune.tejiebox.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.fortune.tejiebox.utils.PhoneInfoUtils


class RunView : View {
    private var maxHeight = 0
    private var maxWidth = 0
    private var animatedValue = 0
    private var aimHeight = 0
    var width = 0f
    var height = 0f
    private var num = 0f
    private var startAngle = 0
    private var endAngle = 0
    private var paint: Paint? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    fun setHeight(height: Int) {
        aimHeight = height
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        maxWidth = getWidth()
        width = getWidth().toFloat()
        maxHeight = getHeight()
        height = getHeight().toFloat()
    }

    private fun init(context: Context) {
        val screenWidth = PhoneInfoUtils.getWidth(context as Activity)
        num = 9f / 360f * screenWidth

        val animator: ValueAnimator = ValueAnimator.ofInt(0, 360)
        animator.duration = 1000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = -1
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            animatedValue = animation.animatedValue as Int
            startAngle = animatedValue
            endAngle = animatedValue + 20
            invalidate()
        }
        animator.start()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (width >= num && height > num) {
            val path = Path()
            //四个圆角
            path.moveTo(num, 0f)
            path.lineTo(width - num, 0f)
            path.quadTo(width, 0f, width, num)
            path.lineTo(width, height - num)
            path.quadTo(width, height, width - num, height)
            path.lineTo(num, height)
            path.quadTo(0f, height, 0f, height - num)
            path.lineTo(0f, num)
            path.quadTo(0f, 0f, num, 0f)
            canvas.clipPath(path)
            canvas.drawFilter = PaintFlagsDrawFilter(
                0,
                Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
            )
        }
        paint = Paint()
//        val colors = intArrayOf(
//            Color.TRANSPARENT,
//            Color.argb(255, 255, 0, 0),
//            Color.argb(200, 255, 0, 0),
//            Color.argb(155, 255, 0, 0),
//            Color.TRANSPARENT,
//            Color.argb(255, 255, 255, 0),
//            Color.argb(200, 255, 255, 0),
//            Color.argb(155, 255, 255, 0),
//            Color.TRANSPARENT,
//            Color.argb(255, 0, 0, 255),
//            Color.argb(200, 0, 0, 255),
//            Color.argb(155, 0, 0, 255),
//        )

        val colors = intArrayOf(
            Color.argb(255, 255, 0, 0),
            Color.argb(255, 255, 0, 0),
            Color.argb(255, 255, 0, 0),
            Color.argb(255, 255, 0, 0),

            Color.argb(255, 255,165,0),
            Color.argb(255, 255,165,0),
            Color.argb(255, 255,165,0),
            Color.argb(255, 255,165,0),

            Color.argb(255, 255,255,0),
            Color.argb(255, 255,255,0),
            Color.argb(255, 255,255,0),
            Color.argb(255, 255,255,0),

            Color.argb(255, 0,255,0),
            Color.argb(255, 0,255,0),
            Color.argb(255, 0,255,0),
            Color.argb(255, 0,255,0),

            Color.argb(255, 0,127,255),
            Color.argb(255, 0,127,255),
            Color.argb(255, 0,127,255),
            Color.argb(255, 0,127,255),

            Color.argb(255, 0,0,255),
            Color.argb(255, 0,0,255),
            Color.argb(255, 0,0,255),
            Color.argb(255, 0,0,255),

            Color.argb(255, 139,0,255),
            Color.argb(255, 139,0,255),
            Color.argb(255, 139,0,255),
            Color.argb(255, 139,0,255)
        )

        val backGradient = SweepGradient(maxWidth / 2f, maxHeight / 2f, colors, null)
        paint?.shader = backGradient
        paint?.shader = backGradient
        //计算圆的圆心
        val cx = paddingLeft + (getWidth() - paddingLeft - paddingRight) / 2f
        val cy = paddingTop + (getHeight() - paddingTop - paddingBottom) / 2f
        canvas.rotate(-90f + startAngle, cx, cy)
        val radius = maxWidth
        canvas.drawCircle(cx, cy, radius.toFloat(), paint!!)
        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}

