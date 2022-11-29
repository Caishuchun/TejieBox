package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class UploadPicProgress(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    private var mWidth = 0
    private var mHeight = 0
    private var mBaseNum = 0f
    private var mProgress = 0

    private var mPaint = Paint()

    init {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //第一步先画遮罩层
        mPaint.color = Color.parseColor("#50000000")
        canvas?.drawRoundRect(
            RectF(0f, 0f, mWidth.toFloat(), mHeight.toFloat()),
            10 * mBaseNum, 10 * mBaseNum, mPaint
        )
        //第二步,画进度条底色
        mPaint.color = Color.parseColor("#50FFFFFF")
        canvas?.drawCircle(mWidth / 2f, mHeight / 2f, 20 * mBaseNum, mPaint)
        //第三步,画扇面即进度条
        mPaint.color = Color.parseColor("#50000000")
        canvas?.drawArc(
            RectF(
                mWidth / 2f - 20 * mBaseNum, mHeight / 2f - 20 * mBaseNum,
                mWidth / 2f + 20 * mBaseNum, mHeight / 2f + 20 * mBaseNum
            ),
            -90f, mProgress.toFloat() / 100 * 360, true, mPaint
        )
        //第四步,画进度数据
        mPaint.color = Color.parseColor("#FFFFFF")
        mPaint.textSize = 12 * mBaseNum
        val textWidth = mPaint.measureText("$mProgress%")
        val baseLine = Math.abs(mPaint.ascent() + mPaint.descent()) / 2
        canvas?.drawText(
            "$mProgress%",
            mWidth / 2f - textWidth / 2,
            mHeight / 2f + baseLine,
            mPaint
        )
    }

    /**
     * 设置基准比例大小,即当前手机屏幕尺寸对应360的基数
     */
    fun setBaseSizeNum(baseNum: Float) {
        mBaseNum = baseNum
    }

    /**
     * 设置进度
     */
    fun setProgress(progress: Int) {
        mProgress = if (mProgress > 100) {
            100
        } else {
            progress
        }
        postInvalidate()
    }
}