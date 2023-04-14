package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.fortune.tejiebox.R

/**
 * 邀请进度条
 */
class InviteProgress constructor(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    private val mPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
    }

    private var mWidth = 0
    private var mHeight = 0
    private var mProgress = 0
    private val maxPart = 10
    private var bgType = 0

    private var mBitmap4Bg = BitmapFactory.decodeResource(resources, R.mipmap.bg_invite_red)
    private var mBitmap4BgRed = BitmapFactory.decodeResource(resources, R.mipmap.bg_invite_red)
    private var mBitmap4BgYellow =
        BitmapFactory.decodeResource(resources, R.mipmap.bg_invite_yellow)
    private var mBitmap4Progress =
        BitmapFactory.decodeResource(resources, R.mipmap.bg_invite_progress)

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.InviteProgress)
        for (index in 0 until typedArray.indexCount) {
            when (typedArray.getIndex(index)) {
                R.styleable.InviteProgress_bg_type -> {
                    bgType = typedArray.getInt(typedArray.getIndex(index), 0)
                }
            }
        }
        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mBitmap4Bg = if (bgType == 0) mBitmap4BgRed else mBitmap4BgYellow
        // 1.先画底图
        //图片大小, 原图
        val src4Bg = Rect(0, 0, mBitmap4Bg.width, mBitmap4Bg.height)
        //范围位置, 控件大小
        val dst4Bg = Rect(0, 0, mWidth, mHeight)
        canvas?.drawBitmap(mBitmap4Bg, src4Bg, dst4Bg, mPaint)

        // 2.再画进度条
        //图片大小, 原图
        val src4Progress = Rect(0, 0, getProgressWidth(1), mBitmap4Progress.height)
        //范围位置, 控件大小
        val dst4Progress = Rect(0, 0, getProgressWidth(0), mHeight)
        canvas?.drawBitmap(mBitmap4Progress, src4Progress, dst4Progress, mPaint)
    }

    /**
     * 设置进度
     */
    fun setProgress(progress: Int) {
        this.mProgress = progress
    }

    /**
     * 获取进度条长度
     */
    private fun getProgressWidth(type: Int) =
        when (mProgress) {
            0 -> 0
            1, 2, 3, 4 -> (if (type == 0) mWidth else mBitmap4Progress.width) / maxPart * (2 * mProgress - 1)
            else -> (if (type == 0) mWidth else mBitmap4Progress.width)
        }
}