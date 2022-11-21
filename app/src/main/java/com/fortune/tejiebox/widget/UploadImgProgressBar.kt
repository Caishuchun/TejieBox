package com.fortune.tejiebox.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.fortune.tejiebox.utils.LogUtils

/**
 * 图片加载进度
 */
class UploadImgProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mWidth = 0
    private var mHeight = 0
    private var mPaint = Paint()
    private var mRadius = 50f
    private var mProgress = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    init {
        mPaint.color = Color.parseColor("#50000000")
        mPaint.style = Paint.Style.FILL
//        mPaint.shader = ComposeShader(Shader(), Shader(), PorterDuff.Mode.DST_OUT)
//            ComposeShader(PorterDuff.Mode.DST_OUT)
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        canvas?.drawRoundRect(
            RectF(0f, 0f, mWidth.toFloat(), mHeight.toFloat()),
            mRadius, mRadius,
            mPaint
        )

        val halfAngle = getHalfAngle()
        val realAngle = getRealAngle()
        LogUtils.d("${javaClass.simpleName}====halfAngle:$halfAngle,realAngle:$realAngle")
        setDrawPath(halfAngle, realAngle, canvas!!)
    }

    /**
     * 设置多边形画线
     */
    private fun setDrawPath(halfAngle: Float, realAngle: Float, canvas: Canvas) {
        val path = Path()
        path.moveTo(mWidth / 2f, mHeight / 2f)
        if (realAngle <= 90) {
            if (realAngle <= halfAngle) {
                path.lineTo(mWidth / 2f, 0f)
                val size = Math.tan(Math.toRadians(realAngle.toDouble())) * (mHeight.toFloat() / 2)
                path.lineTo(mWidth / 2f + size.toFloat(), 0f)
            } else {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                val size =
                    Math.tan(Math.toRadians(90 - realAngle.toDouble())) * (mWidth.toFloat() / 2)
                path.lineTo(mWidth.toFloat(), (mHeight / 2f) - size.toFloat())
            }
        } else if (realAngle <= 180 && realAngle > 90) {
            if (realAngle <= 180 - halfAngle) {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                val size =
                    Math.tan(Math.toRadians(realAngle.toDouble() - 90)) * (mWidth.toFloat() / 2)
                path.lineTo(mWidth.toFloat(), (mHeight / 2f) + size.toFloat())
            } else {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                path.lineTo(mWidth.toFloat(), mHeight.toFloat())
                val size =
                    Math.tan(Math.toRadians(180 - realAngle.toDouble())) * (mHeight.toFloat() / 2)
                path.lineTo(mWidth / 2f + size.toFloat(), mHeight.toFloat())
            }
        } else if (realAngle <= 270 && realAngle > 180) {
            if (realAngle <= 180 + halfAngle) {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                path.lineTo(mWidth.toFloat(), mHeight.toFloat())
                val size =
                    Math.tan(Math.toRadians(realAngle.toDouble() - 180)) * (mHeight.toFloat() / 2)
                path.lineTo(mWidth / 2f - size.toFloat(), mHeight.toFloat())
            } else {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                path.lineTo(mWidth.toFloat(), mHeight.toFloat())
                path.lineTo(0f, mHeight.toFloat())
                val size =
                    Math.tan(Math.toRadians(270 - realAngle.toDouble())) * (mWidth.toFloat() / 2)
                path.lineTo(0f, mHeight / 2f + size.toFloat())
            }
        } else {
            if (realAngle <= 360 - halfAngle) {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                path.lineTo(mWidth.toFloat(), mHeight.toFloat())
                path.lineTo(0f, mHeight.toFloat())
                val size =
                    Math.tan(Math.toRadians(realAngle.toDouble() - 270)) * (mWidth.toFloat() / 2)
                path.lineTo(0f, mHeight / 2f - size.toFloat())
            } else {
                path.lineTo(mWidth / 2f, 0f)
                path.lineTo(mWidth.toFloat(), 0f)
                path.lineTo(mWidth.toFloat(), mHeight.toFloat())
                path.lineTo(0f, mHeight.toFloat())
                path.lineTo(0f, 0f)
                val size =
                    Math.tan(Math.toRadians(360 - realAngle.toDouble())) * (mHeight.toFloat() / 2)
                path.lineTo(mWidth / 2f - size.toFloat(), 0f)
            }
        }
        path.close()
        mPaint.color = Color.TRANSPARENT
        canvas.drawPath(path, mPaint)
    }

    /**
     * 获取进度对应的角度
     */
    private fun getRealAngle() = mProgress * 360

    /**
     * 获取中心到右上角连线后的角度
     */
    private fun getHalfAngle(): Float {
        val halfWidth = mWidth.toFloat() / 2
        val halfHeight = mHeight.toFloat() / 2
        val line = Math.sqrt(halfWidth.toDouble() * halfWidth + halfHeight * halfHeight)
        LogUtils.d("${javaClass.simpleName}====halfWidth:$halfWidth,halfHeight:$halfHeight,line:$line")
        val asin = Math.asin(halfWidth / line)
        return Math.toDegrees(asin).toFloat()
    }


    //---------------以下为外部提供方法--------------------

    /**
     * 设置圆角
     */
    fun setRadius(radius: Float) {
        mRadius = radius
    }

    /**
     * 设置进度
     */
    fun setProgress(progress: Float) {
        mProgress = progress
        postInvalidate()
    }
}