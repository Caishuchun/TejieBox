package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 圆形进度条
 */
class CircleProgress constructor(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    private val mPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#00ff66")
    }

    private var mWidth = 0
    private var mHeight = 0
    private var mProgress = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //先画一个圆环
        mPaint.strokeWidth = 2f
        canvas?.drawCircle(
            mWidth / 2f, mHeight / 2f, mWidth / 2f - 2f, mPaint
        )

        //画进度条
        mPaint.strokeWidth = 20f
        canvas?.drawArc(
            RectF(11f, 11f, mWidth.toFloat() - 11f, mHeight.toFloat() - 11f),
            -90f,
            mProgress.toFloat() / 100 * 360,
            false,
            mPaint
        )
    }

    /**
     * 设置进度条
     */
    fun setProgress(progress: Int) {
        this.mProgress = progress
        postInvalidate()
    }
}