package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * 掉血提示View
 */
class LoseBloodView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    private var mWidth = 0f
    private var mHeight = 0f
    private var scale = 0f

    private var mPaint4Font = Paint()
    private var subscribe: Disposable? = null

    private var blood: Int? = null

    init {
        mPaint4Font.isAntiAlias = true
        mPaint4Font.color = Color.parseColor("#FF0000")
        mPaint4Font.style = Paint.Style.FILL_AND_STROKE
        mPaint4Font.strokeCap = Paint.Cap.ROUND
        mPaint4Font.textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        scale = w.toFloat() / 360
        mHeight = h.toFloat()
    }

    @SuppressLint("DrawAllocation", "CheckResult")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (blood == null) {
            return
        }
        //随机字号
        val random = (50..80).random()
        mPaint4Font.textSize = random * scale

        val fontMetrics = mPaint4Font.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom

        //随机中心点
        val centerX = ((random * scale).toInt()..(mWidth - random * scale).toInt()).random()
        val centerY = ((random * scale).toInt()..(mHeight - random * scale).toInt()).random()

        val lefX = (0..(random * scale).toInt()).random()
        val rightX = ((random * scale).toInt()..mWidth.toInt()).random()
        val topY = (0..(random * scale).toInt()).random()
        val bottomY = ((random * scale).toInt()..mHeight.toInt()).random()

        val rectF5 = RectF(
            centerX - 20 * scale,
            centerY - 20 * scale,
            centerX + 20 * scale,
            centerY + 20 * scale
        )
        val baseLine5 = rectF5.centerY() + distance
        canvas?.drawText(
//            "-$blood",
            "-10000",
            rectF5.centerX(),
            baseLine5,
            mPaint4Font
        )

        subscribe = Observable.timer(1000, TimeUnit.MILLISECONDS)
            .subscribe {
                mPaint4Font.color = Color.TRANSPARENT
                postInvalidate()
//                subscribe?.dispose()
//                subscribe = null
            }
    }

    /**
     * 设置掉血效果
     */
    fun setLoseBlood(blood: Int) {
        subscribe?.dispose()
        subscribe = null
        mPaint4Font.color = Color.parseColor("#FF0000")
        this.blood = blood
        postInvalidate()
    }
}