package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.bean.NewYear4NianShouBean
import com.google.gson.Gson

class BloodVolumeView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    private var mWidth = 0
    private var mHeight = 0
    private var scale = 0f
    private val mPaint = Paint()
    private var mData: NewYear4NianShouBean.Data? = null

    init {
        mPaint.isAntiAlias = true
        mPaint.color = Color.parseColor("#FFFFFF")
        mPaint.style = Paint.Style.FILL
        mPaint.strokeCap = Paint.Cap.BUTT
        mPaint.textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        scale = w.toFloat() / 360
        mPaint.strokeWidth = 12 * scale
        mHeight = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mData == null) {
            val bean = Gson().fromJson(
                "{\"code\":1,\"data\":{\"current_blood\":888888,\"current_energy\":16,\"end_time\":1703920223,\"start_time\":1672388635,\"total_blood\":888888,\"treasure_list\":[{\"blood\":1000,\"give_num\":20,\"receive_state\":0,\"treasure_id\":1},{\"blood\":50000,\"give_num\":100,\"receive_state\":0,\"treasure_id\":2},{\"blood\":120000,\"give_num\":500,\"receive_state\":0,\"treasure_id\":3},{\"blood\":250000,\"give_num\":1000,\"receive_state\":0,\"treasure_id\":4},{\"blood\":580000,\"give_num\":2000,\"receive_state\":0,\"treasure_id\":5},{\"blood\":888888,\"give_num\":5000,\"receive_state\":0,\"treasure_id\":6}]},\"msg\":\"success\"}",
                NewYear4NianShouBean::class.java
            )
            mData = bean.data
        }

        drawTotalBloodVolume(canvas)
        drawCurrentBloodVolume(canvas)
        drawPeriodOfBloodVolume(canvas)
        drawFont(canvas)
    }

    /**
     * 绘制总血量
     * 888888
     */
    private fun drawTotalBloodVolume(canvas: Canvas?) {
        mPaint.color = Color.parseColor("#FFFFFF")
        mPaint.strokeWidth = 12 * scale
        canvas?.drawLine(
            20f * scale, 50f * scale,
            mWidth.toFloat() - 20f * scale, 50f * scale,
            mPaint
        )
    }

    /**
     * 绘制当前血量
     */
    private fun drawCurrentBloodVolume(canvas: Canvas?) {
        val totalBlood = mData?.total_blood!!
        val currentBlood = mData?.current_blood!!

        mPaint.color = Color.parseColor("#FFC667")
        mPaint.strokeWidth = 12 * scale
        canvas?.drawLine(
            20f * scale,
            50f * scale,
            20f * scale + (currentBlood.toFloat() / totalBlood) * (mWidth - 40 * scale),
            50f * scale,
            mPaint
        )
    }

    /**
     * 绘制血量分段
     * 1000,50000,120000,250000,580000,8888888
     */
    private fun drawPeriodOfBloodVolume(canvas: Canvas?) {
        val totalBlood = mData?.total_blood!!
        val treasureList = mData?.treasure_list!!
        mPaint.strokeWidth = 2 * scale
        mPaint.color = Color.parseColor("#000000")
        //满血线
        drawPicState(canvas, 20f * scale, 5, treasureList[5].receive_state)
//        canvas?.drawLine(20f * scale, 44f * scale, 20f * scale, 56f * scale, mPaint)

        //其他血量线
        val allBlood = mWidth - 2 * 20f * scale
        val blood_58wan = (totalBlood.toFloat() - treasureList[4].blood) / totalBlood
        drawPicState(
            canvas,
            20f * scale + allBlood * blood_58wan,
            4,
            treasureList[4].receive_state
        )
        canvas?.drawLine(
            20f * scale + allBlood * blood_58wan,
            44f * scale,
            20f * scale + allBlood * blood_58wan,
            56f * scale,
            mPaint
        )

        val blood_25wan = (totalBlood.toFloat() - treasureList[3].blood) / totalBlood
        drawPicState(
            canvas,
            20f * scale + allBlood * blood_25wan,
            3,
            treasureList[3].receive_state
        )
        canvas?.drawLine(
            20f * scale + allBlood * blood_25wan,
            44f * scale,
            20f * scale + allBlood * blood_25wan,
            56f * scale,
            mPaint
        )

        val blood_12wan = (totalBlood.toFloat() - treasureList[2].blood) / totalBlood
        drawPicState(
            canvas,
            20f * scale + allBlood * blood_12wan,
            2,
            treasureList[2].receive_state
        )
        canvas?.drawLine(
            20f * scale + allBlood * blood_12wan,
            44f * scale,
            20f * scale + allBlood * blood_12wan,
            56f * scale,
            mPaint
        )

        val blood_5wan = (totalBlood.toFloat() - treasureList[1].blood) / totalBlood
        drawPicState(
            canvas,
            20f * scale + allBlood * blood_5wan,
            1,
            treasureList[1].receive_state
        )
        canvas?.drawLine(
            20f * scale + allBlood * blood_5wan,
            44f * scale,
            20f * scale + allBlood * blood_5wan,
            56f * scale,
            mPaint
        )

        val blood_1qian = (totalBlood.toFloat() - treasureList[0].blood) / totalBlood
        drawPicState(
            canvas,
            20f * scale + allBlood * blood_1qian,
            0,
            treasureList[0].receive_state
        )
        canvas?.drawLine(
            20f * scale + allBlood * blood_1qian - 2f * scale,
            44f * scale,
            20f * scale + allBlood * blood_1qian - 2f * scale,
            56f * scale,
            mPaint
        )
    }

    /**
     * 绘制文字
     */
    private fun drawFont(canvas: Canvas?) {
        val totalBlood = mData?.total_blood!!
        val currentBlood = mData?.current_blood!!
        mPaint.color = Color.parseColor("#ee1212")
        mPaint.textSize = 10f * scale
        mPaint.strokeWidth = 1 * scale
        val fontMetrics = mPaint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val measureText = mPaint.measureText("$currentBlood/$totalBlood".trim())
        val textRect = Rect()
        mPaint.getTextBounds(
            "$currentBlood/$totalBlood",
            0,
            "$currentBlood/$totalBlood".length,
            textRect
        )
        val rectF0 = RectF(
            20f * scale, 44f * scale,
            mWidth.toFloat() - 20f * scale, 56f * scale,
        )
        val baseLine0 = rectF0.centerY() + distance
        canvas?.drawText(
            "$currentBlood/$totalBlood",
            rectF0.centerX(),
            baseLine0,
            mPaint
        )
    }

    /**
     * 绘制宝箱图片
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawPicState(canvas: Canvas?, period: Float, index: Int, state: Int) {
        val opened = (resources.getDrawable(R.mipmap.new_year_box_open) as BitmapDrawable).bitmap
        val box1 = (resources.getDrawable(R.mipmap.box1) as BitmapDrawable).bitmap
        val box2 = (resources.getDrawable(R.mipmap.box2) as BitmapDrawable).bitmap
        val box3 = (resources.getDrawable(R.mipmap.box3) as BitmapDrawable).bitmap
        val box4 = (resources.getDrawable(R.mipmap.box4) as BitmapDrawable).bitmap
        val box5 = (resources.getDrawable(R.mipmap.box5) as BitmapDrawable).bitmap
        val box6 = (resources.getDrawable(R.mipmap.box6) as BitmapDrawable).bitmap
        val box1Open = (resources.getDrawable(R.mipmap.box1_open) as BitmapDrawable).bitmap
        val box2Open = (resources.getDrawable(R.mipmap.box2_open) as BitmapDrawable).bitmap
        val box3Open = (resources.getDrawable(R.mipmap.box3_open) as BitmapDrawable).bitmap
        val box4Open = (resources.getDrawable(R.mipmap.box4_open) as BitmapDrawable).bitmap
        val box5Open = (resources.getDrawable(R.mipmap.box5_open) as BitmapDrawable).bitmap
        val box6Open = (resources.getDrawable(R.mipmap.box6_open) as BitmapDrawable).bitmap
        val destRect = Rect(
            (period - 18f * scale).toInt(),
            if (index % 2 == 1) (4f * scale).toInt() else (60f * scale).toInt(),
            (period + 18f * scale).toInt(),
            if (index % 2 == 1) (40 * scale).toInt() else (96f * scale).toInt(),
        )
        when (index) {
            0 -> {
                val bitmap0 = if (state == 0) box1 else box1Open
                val srcRect0 = Rect(0, 0, bitmap0.width, bitmap0.height)
                canvas?.drawBitmap(bitmap0, srcRect0, destRect, mPaint)
                if (state == 2) {
                    val srcRect = Rect(0, 0, opened.width, opened.height)
                    canvas?.drawBitmap(opened, srcRect, destRect, mPaint)
                }
            }
            1 -> {
                val bitmap1 = if (state == 0) box2 else box2Open
                val srcRect1 = Rect(0, 0, bitmap1.width, bitmap1.height)
                canvas?.drawBitmap(bitmap1, srcRect1, destRect, mPaint)
                if (state == 2) {
                    val srcRect = Rect(0, 0, opened.width, opened.height)
                    canvas?.drawBitmap(opened, srcRect, destRect, mPaint)
                }
            }
            2 -> {
                val bitmap2 =if (state == 0) box3 else box3Open
                val srcRect2 = Rect(0, 0, bitmap2.width, bitmap2.height)
                canvas?.drawBitmap(bitmap2, srcRect2, destRect, mPaint)
                if (state == 2) {
                    val srcRect = Rect(0, 0, opened.width, opened.height)
                    canvas?.drawBitmap(opened, srcRect, destRect, mPaint)
                }
            }
            3 -> {
                val bitmap3 = if (state == 0) box4 else box4Open
                val srcRect3 = Rect(0, 0, bitmap3.width, bitmap3.height)
                canvas?.drawBitmap(bitmap3, srcRect3, destRect, mPaint)
                if (state == 2) {
                    val srcRect = Rect(0, 0, opened.width, opened.height)
                    canvas?.drawBitmap(opened, srcRect, destRect, mPaint)
                }
            }
            4 -> {
                val bitmap4 = if (state == 0) box5 else box5Open
                val srcRect4 = Rect(0, 0, bitmap4.width, bitmap4.height)
                canvas?.drawBitmap(bitmap4, srcRect4, destRect, mPaint)
                if (state == 2) {
                    val srcRect = Rect(0, 0, opened.width, opened.height)
                    canvas?.drawBitmap(opened, srcRect, destRect, mPaint)
                }
            }
            5 -> {
                val bitmap5 = if (state == 0) box6 else box6Open
                val srcRect5 = Rect(0, 0, bitmap5.width, bitmap5.height)
                canvas?.drawBitmap(bitmap5, srcRect5, destRect, mPaint)
                if (state == 2) {
                    val srcRect = Rect(0, 0, opened.width, opened.height)
                    canvas?.drawBitmap(opened, srcRect, destRect, mPaint)
                }
            }
        }
    }

    private var lastX = 0f
    private var lastY = 0f

    /**
     * 监听按键
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val moveX = Math.abs(lastX - event.x)
                val moveY = Math.abs(lastY - event.y)
                if (moveX < 20 * scale && moveY < 20 * scale) {
                    //认为是个合格的点击事件
                    getIndex(event.x, event.y)
                }
            }
        }
        return true
    }

    /**
     * 获取点击的是第几个宝箱
     */
    private fun getIndex(x: Float, y: Float) {
        val totalBlood = mData?.total_blood!!
        val treasureList = mData?.treasure_list!!
        val allBlood = mWidth - 2 * 20f * scale
        val blood_58wan = (totalBlood - treasureList[4].blood) / totalBlood.toFloat()
        val blood_25wan = (totalBlood - treasureList[3].blood) / totalBlood.toFloat()
        val blood_12wan = (totalBlood - treasureList[2].blood) / totalBlood.toFloat()
        val blood_5wan = (totalBlood - treasureList[1].blood) / totalBlood.toFloat()
        val blood_1qian = (totalBlood - treasureList[0].blood) / totalBlood.toFloat()
        if (x in 20f * scale - 18 * scale..20f * scale + 18 * scale &&
            y in 4f * scale..40f * scale
        ) {
            mOnCallBack?.click(5)
        } else if (
            x in 20f * scale + allBlood * blood_58wan - 18f * scale..20f * scale + allBlood * blood_58wan + 18f * scale &&
            y in 60f * scale..96f * scale
        ) {
            mOnCallBack?.click(4)
        } else if (
            x in 20f * scale + allBlood * blood_25wan - 18f * scale..20f * scale + allBlood * blood_25wan + 18f * scale &&
            y in 4f * scale..40f * scale
        ) {
            mOnCallBack?.click(3)
        } else if (
            x in 20f * scale + allBlood * blood_12wan - 18f * scale..20f * scale + allBlood * blood_12wan + 18f * scale &&
            y in 60f * scale..96f * scale
        ) {
            mOnCallBack?.click(2)
        } else if (
            x in 20f * scale + allBlood * blood_5wan - 18f * scale..20f * scale + allBlood * blood_5wan + 18f * scale &&
            y in 4f * scale..40f * scale
        ) {
            mOnCallBack?.click(1)
        } else if (
            x in 20f * scale + allBlood * blood_1qian - 18f * scale..20f * scale + allBlood * blood_1qian + 18f * scale &&
            y in 60f * scale..96f * scale
        ) {
            mOnCallBack?.click(0)
        }
    }

    /**
     * 设置数据
     */
    fun toChangeState(data: NewYear4NianShouBean.Data) {
        this.mData = data
        postInvalidate()
    }

    private var mOnCallBack: OnCallBack? = null

    interface OnCallBack {
        fun click(index: Int)
    }

    fun setOnCallBack(onCallBack: OnCallBack) {
        this.mOnCallBack = onCallBack
    }

}