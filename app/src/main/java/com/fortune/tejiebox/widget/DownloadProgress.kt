package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil

/**
 * 自定义随进度移动数值的进度条
 */
class DownloadProgress(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    //进度条的宽
    private var mWidth = 0

    //进度条的高
    private var mHeight = 0

    //当前进度
    private var mProgress = 0

    //每一格进度,进度条的长度
    private var mSpace = 0f

    //文字和进度之间的间距
    private var mInterval = 0f

    private val progressPaint = Paint()

    /**
     * 初始化画笔
     */
    init {
        progressPaint.isAntiAlias = true
        progressPaint.color = Color.parseColor("#FF5F60FF")
        progressPaint.strokeWidth = 5f
        progressPaint.style = Paint.Style.FILL
        progressPaint.strokeCap = Paint.Cap.ROUND
    }

    /**
     * 俗称控件绘制完定型后获取控件大小
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        //进度数值直接设置为控件高度
        progressPaint.textSize = mHeight * 0.8f
        //间距..
        mSpace = mWidth.toFloat() / 100
        mInterval = mSpace
    }

    /**
     * 设置进度,唯一暴露出去的方法
     */
    fun setProgress(progress: Int) {
        this.mProgress = progress
        postInvalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val s = "$mProgress%"
        val textWidth = getTextWidth(progressPaint, s)
        //获取进度终点,也是文字开始绘制位置
        val lineEnd = if (mProgress * mSpace >= mWidth - textWidth) {
            //如果进度+文字绘制长度已经超过总长度的话,那就直接写死
            mWidth.toFloat() - textWidth
        } else {
            //没超过,则是进度*间距
            mProgress * mSpace
        }
        //这一步就是为了在进度条和数值之间有个间距
        val intervalLineEnd = if (lineEnd - mInterval <= 0f) {
            0f
        } else {
            lineEnd - mInterval
        }
        //绘制进度,之所以2/3是为了进度在文字中间,为什么不是1/2?
        //居上1/3,居下1/3,线宽1/3
        canvas?.drawLine(
            0f,
            mHeight.toFloat() / 3 * 2,
            intervalLineEnd,
            mHeight.toFloat() / 3 * 2,
            progressPaint
        )
        //绘制文字
        canvas?.drawText(s, lineEnd, mHeight * 0.9f, progressPaint)
    }

    /**
     * 获取文字的宽
     */
    private fun getTextWidth(paint: Paint, str: String): Int {
        var iRet = 0
        val length = str.length
        val widths = FloatArray(length)
        paint.getTextWidths(str, widths)
        for (i in 0 until length) {
            iRet += ceil(widths[i].toDouble()).toInt()
        }
        return iRet
    }
}
