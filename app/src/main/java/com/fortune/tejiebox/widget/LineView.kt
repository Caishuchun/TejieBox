package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.bean.NewYear4InviteBean
import com.google.gson.Gson

class LineView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    private var mWidth = 0
    private var mHeight = 0
    private var scale = 0f
    private val mPaint4Line = Paint()
    private val mPaint4Circle = Paint()
    private val mPaint4Font = Paint()
    private var mTreasureList: List<NewYear4InviteBean.Data.Treasure>? = null

    init {
        mPaint4Line.isAntiAlias = true
        mPaint4Line.color = Color.parseColor("#c5c5c5")
//        mPaint4Line.color = Color.parseColor("#f51818")
        mPaint4Line.style = Paint.Style.STROKE
        mPaint4Line.strokeCap = Paint.Cap.ROUND

        mPaint4Circle.isAntiAlias = true
        mPaint4Circle.color = Color.parseColor("#c5c5c5")
//        mPaint4Circle.color = Color.parseColor("#f51818")
        mPaint4Circle.style = Paint.Style.FILL
        mPaint4Circle.strokeCap = Paint.Cap.ROUND

        mPaint4Font.isAntiAlias = true
        mPaint4Font.color = Color.parseColor("#FFFFFF")
        mPaint4Font.style = Paint.Style.FILL_AND_STROKE
        mPaint4Font.strokeCap = Paint.Cap.ROUND
        mPaint4Font.textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        scale = w.toFloat() / 360
        mPaint4Line.strokeWidth = 5 * scale
        mPaint4Font.strokeWidth = scale
        mPaint4Font.textSize = 15 * scale
        mHeight = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawParts(canvas)
    }

    /**
     * 逐一绘制单元部分
     * 16*19
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawParts(canvas: Canvas?) {
        if (mTreasureList == null) {
            val bean = Gson().fromJson(
                "{\"code\":1,\"data\":{\"end_time\":1703920223,\"share_link\":\"http://47.94.252.136:10001?new_year_channel\\u003da94\",\"start_time\":1672388635,\"total_time\":30,\"treasure_list\":[{\"give_num\":50,\"invitation_num\":1,\"receive_state\":0,\"treasure_id\":1},{\"give_num\":100,\"invitation_num\":2,\"receive_state\":0,\"treasure_id\":2},{\"give_num\":100,\"invitation_num\":3,\"receive_state\":0,\"treasure_id\":3},{\"give_num\":200,\"invitation_num\":5,\"receive_state\":0,\"treasure_id\":4},{\"give_num\":200,\"invitation_num\":6,\"receive_state\":0,\"treasure_id\":5},{\"give_num\":200,\"invitation_num\":8,\"receive_state\":0,\"treasure_id\":6},{\"give_num\":500,\"invitation_num\":20,\"receive_state\":0,\"treasure_id\":7},{\"give_num\":1000,\"invitation_num\":30,\"receive_state\":0,\"treasure_id\":8},{\"give_num\":5000,\"invitation_num\":50,\"receive_state\":0,\"treasure_id\":8}]},\"msg\":\"success\"}",
                NewYear4InviteBean::class.java
            )
            mTreasureList = bean.data.treasure_list
        }
        val data = mTreasureList!!

        //第一部分
        if (data[0].receive_state == 0) {
            mPaint4Line.color = Color.parseColor("#c5c5c5")
            canvas?.drawLine(
                mWidth / 16f, mHeight / 19f * 5,
                mWidth / 16f * 4, mHeight / 19f * 5,
                mPaint4Line
            )
            mPaint4Line.color = Color.parseColor("#f51818")
            canvas?.drawLine(
                mWidth / 16f, mHeight / 19f * 5,
                mWidth / 16f * 2, mHeight / 19f * 5,
                mPaint4Line
            )
        } else {
            mPaint4Line.color = Color.parseColor("#f51818")
            canvas?.drawLine(
                mWidth / 16f, mHeight / 19f * 5,
                mWidth / 16f * 4, mHeight / 19f * 5,
                mPaint4Line
            )
        }
        val destRect0 = Rect(
            (mWidth / 16f * 4 - 30 * scale).toInt(),
            (mHeight / 19f * 5 - 82 * scale).toInt(),
            (mWidth / 16f * 4 + 30 * scale).toInt(),
            (mHeight / 19f * 5 - 22 * scale).toInt()
        )
        val bitmap0 = getBitmap(data[0])
        val srcRect0 = Rect(0, 0, bitmap0.width, bitmap0.height)
        canvas?.drawBitmap(bitmap0, srcRect0, destRect0, mPaint4Line)

        //第二部分
        mPaint4Line.color =
            if (data[1].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawLine(
            mWidth / 16f * 4, mHeight / 19f * 5,
            mWidth / 16f * 8, mHeight / 19f * 5,
            mPaint4Line
        )
        val destRect1 = Rect(
            (mWidth / 16f * 8 - 30 * scale).toInt(),
            (mHeight / 19f * 5 - 82 * scale).toInt(),
            (mWidth / 16f * 8 + 30 * scale).toInt(),
            (mHeight / 19f * 5 - 22 * scale).toInt()
        )
        val bitmap1 = getBitmap(data[1])
        val srcRect1 = Rect(0, 0, bitmap1.width, bitmap1.height)
        canvas?.drawBitmap(bitmap1, srcRect1, destRect1, mPaint4Line)

        //第三部分
        mPaint4Line.color =
            if (data[2].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawLine(
            mWidth / 16f * 8, mHeight / 19f * 5,
            mWidth / 16f * 12, mHeight / 19f * 5,
            mPaint4Line
        )
        val destRect2 = Rect(
            (mWidth / 16f * 12 - 30 * scale).toInt(),
            (mHeight / 19f * 5 - 82 * scale).toInt(),
            (mWidth / 16f * 12 + 30 * scale).toInt(),
            (mHeight / 19f * 5 - 22 * scale).toInt()
        )
        val bitmap2 = getBitmap(data[2])
        val srcRect2 = Rect(0, 0, bitmap2.width, bitmap2.height)
        canvas?.drawBitmap(bitmap2, srcRect2, destRect2, mPaint4Line)

        //第四部分
        mPaint4Line.color =
            if (data[3].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        val half = (mHeight / 19f * 11 - mHeight / 19f * 5) / 2
        canvas?.drawArc(
            mWidth / 16f * 12 - half, mHeight / 19f * 5,
            mWidth / 16f * 12 + half, mHeight / 19f * 11,
            -90f, 180f, false, mPaint4Line
        )
        val destRect3 = Rect(
            (mWidth / 16f * 12 - 30 * scale).toInt(),
            (mHeight / 19f * 11 - 82 * scale).toInt(),
            (mWidth / 16f * 12 + 30 * scale).toInt(),
            (mHeight / 19f * 11 - 22 * scale).toInt()
        )
        val bitmap3 = getBitmap(data[3])
        val srcRect3 = Rect(0, 0, bitmap3.width, bitmap3.height)
        canvas?.drawBitmap(bitmap3, srcRect3, destRect3, mPaint4Line)

        //第五部分
        mPaint4Line.color =
            if (data[4].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawLine(
            mWidth / 16f * 12, mHeight / 19f * 11,
            mWidth / 16f * 8, mHeight / 19f * 11,
            mPaint4Line
        )
        val destRect4 = Rect(
            (mWidth / 16f * 8 - 30 * scale).toInt(),
            (mHeight / 19f * 11 - 82 * scale).toInt(),
            (mWidth / 16f * 8 + 30 * scale).toInt(),
            (mHeight / 19f * 11 - 22 * scale).toInt()
        )
        val bitmap4 = getBitmap(data[4])
        val srcRect4 = Rect(0, 0, bitmap4.width, bitmap4.height)
        canvas?.drawBitmap(bitmap4, srcRect4, destRect4, mPaint4Line)

        //第六部分
        mPaint4Line.color =
            if (data[5].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawLine(
            mWidth / 16f * 8, mHeight / 19f * 11,
            mWidth / 16f * 4, mHeight / 19f * 11,
            mPaint4Line
        )
        val destRect5 = Rect(
            (mWidth / 16f * 4 - 30 * scale).toInt(),
            (mHeight / 19f * 11 - 82 * scale).toInt(),
            (mWidth / 16f * 4 + 30 * scale).toInt(),
            (mHeight / 19f * 11 - 22 * scale).toInt()
        )
        val bitmap5 = getBitmap(data[5])
        val srcRect5 = Rect(0, 0, bitmap5.width, bitmap5.height)
        canvas?.drawBitmap(bitmap5, srcRect5, destRect5, mPaint4Line)

        //第七部分
        mPaint4Line.color =
            if (data[6].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawArc(
            mWidth / 16f * 4 - half, mHeight / 19f * 11,
            mWidth / 16f * 4 + half, mHeight / 19f * 17,
            90f, 180f, false, mPaint4Line
        )
        val destRect6 = Rect(
            (mWidth / 16f * 4 - 30 * scale).toInt(),
            (mHeight / 19f * 17 - 82 * scale).toInt(),
            (mWidth / 16f * 4 + 30 * scale).toInt(),
            (mHeight / 19f * 17 - 22 * scale).toInt()
        )
        val bitmap6 = getBitmap(data[6])
        val srcRect6 = Rect(0, 0, bitmap6.width, bitmap6.height)
        canvas?.drawBitmap(bitmap6, srcRect6, destRect6, mPaint4Line)

        //第八部分
        mPaint4Line.color =
            if (data[7].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawLine(
            mWidth / 16f * 4, mHeight / 19f * 17,
            mWidth / 16f * 8, mHeight / 19f * 17,
            mPaint4Line
        )
        val destRect7 = Rect(
            (mWidth / 16f * 8 - 30 * scale).toInt(),
            (mHeight / 19f * 17 - 82 * scale).toInt(),
            (mWidth / 16f * 8 + 30 * scale).toInt(),
            (mHeight / 19f * 17 - 22 * scale).toInt()
        )
        val bitmap7 = getBitmap(data[7])
        val srcRect7 = Rect(0, 0, bitmap7.width, bitmap7.height)
        canvas?.drawBitmap(bitmap7, srcRect7, destRect7, mPaint4Line)

        //第九部分
        mPaint4Line.color =
            if (data[8].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawLine(
            mWidth / 16f * 8, mHeight / 19f * 17,
            mWidth / 16f * 12, mHeight / 19f * 17,
            mPaint4Line
        )
        val destRect8 = Rect(
            (mWidth / 16f * 12 - 30 * scale).toInt(),
            (mHeight / 19f * 17 - 82 * scale).toInt(),
            (mWidth / 16f * 12 + 30 * scale).toInt(),
            (mHeight / 19f * 17 - 22 * scale).toInt()
        )
        val bitmap8 = getBitmap(data[8])
        val srcRect8 = Rect(0, 0, bitmap8.width, bitmap8.height)
        canvas?.drawBitmap(bitmap8, srcRect8, destRect8, mPaint4Line)

        //结尾
        mPaint4Line.color =
            if (data[8].receive_state == 2) Color.parseColor("#f51818")
            else Color.parseColor("#c5c5c5")
        canvas?.drawLine(
            mWidth / 16f * 12, mHeight / 19f * 17,
            mWidth / 16f * 15, mHeight / 19f * 17,
            mPaint4Line
        )

        drawFont(canvas, data)
    }

    /**
     * 获取恰当的图片
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getBitmap(data: NewYear4InviteBean.Data.Treasure): Bitmap {
        val bitmap4Wait = (resources.getDrawable(R.mipmap.invite_wait) as BitmapDrawable).bitmap
        val bitmap4Get = (resources.getDrawable(R.mipmap.invite_get) as BitmapDrawable).bitmap
        val bitmap4Got = (resources.getDrawable(R.mipmap.invite_got) as BitmapDrawable).bitmap
        return when (data.receive_state) {
            1 -> bitmap4Get
            2 -> bitmap4Got
            else -> bitmap4Wait
        }
    }

    /**
     * 绘制文字
     */
    private fun drawFont(canvas: Canvas?, data: List<NewYear4InviteBean.Data.Treasure>) {
        val fontMetrics = mPaint4Font.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        //1
        mPaint4Circle.color =
            if (data[0].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 4, mHeight / 19f * 5,
            15 * scale, mPaint4Circle
        )
        val rectF0 = RectF(
            mWidth / 16f * 4 - 15 * scale,
            mHeight / 19f * 5 - 15 * scale,
            mWidth / 16f * 4 + 15 * scale,
            mHeight / 19f * 5 + 15 * scale
        )
        val baseLine0 = rectF0.centerY() + distance
        canvas?.drawText(
            data[0].invitation_num.toString(),
            rectF0.centerX(),
            baseLine0,
            mPaint4Font
        )

        //2
        mPaint4Circle.color =
            if (data[1].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 8, mHeight / 19f * 5,
            15 * scale, mPaint4Circle
        )
        val rectF1 = RectF(
            mWidth / 16f * 8 - 15 * scale,
            mHeight / 19f * 5 - 15 * scale,
            mWidth / 16f * 8 + 15 * scale,
            mHeight / 19f * 5 + 15 * scale
        )
        val baseLine1 = rectF1.centerY() + distance
        canvas?.drawText(
            data[1].invitation_num.toString(),
            rectF1.centerX(),
            baseLine1,
            mPaint4Font
        )

        //3
        mPaint4Circle.color =
            if (data[2].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 12, mHeight / 19f * 5,
            15 * scale, mPaint4Circle
        )
        val rectF2 = RectF(
            mWidth / 16f * 12 - 15 * scale,
            mHeight / 19f * 5 - 15 * scale,
            mWidth / 16f * 12 + 15 * scale,
            mHeight / 19f * 5 + 15 * scale
        )
        val baseLine2 = rectF2.centerY() + distance
        canvas?.drawText(
            data[2].invitation_num.toString(),
            rectF2.centerX(),
            baseLine2,
            mPaint4Font
        )

        //4
        mPaint4Circle.color =
            if (data[3].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 12, mHeight / 19f * 11,
            15 * scale, mPaint4Circle
        )
        val rectF3 = RectF(
            mWidth / 16f * 12 - 15 * scale,
            mHeight / 19f * 11 - 15 * scale,
            mWidth / 16f * 12 + 15 * scale,
            mHeight / 19f * 11 + 15 * scale
        )
        val baseLine3 = rectF3.centerY() + distance
        canvas?.drawText(
            data[3].invitation_num.toString(),
            rectF3.centerX(),
            baseLine3,
            mPaint4Font
        )

        //5
        mPaint4Circle.color =
            if (data[4].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 8, mHeight / 19f * 11,
            15 * scale, mPaint4Circle
        )
        val rectF4 = RectF(
            mWidth / 16f * 8 - 15 * scale,
            mHeight / 19f * 11 - 15 * scale,
            mWidth / 16f * 8 + 15 * scale,
            mHeight / 19f * 11 + 15 * scale
        )
        val baseLine4 = rectF4.centerY() + distance
        canvas?.drawText(
            data[4].invitation_num.toString(),
            rectF4.centerX(),
            baseLine4,
            mPaint4Font
        )

        //6
        mPaint4Circle.color =
            if (data[5].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 4, mHeight / 19f * 11,
            15 * scale, mPaint4Circle
        )
        val rectF5 = RectF(
            mWidth / 16f * 4 - 15 * scale,
            mHeight / 19f * 11 - 15 * scale,
            mWidth / 16f * 4 + 15 * scale,
            mHeight / 19f * 11 + 15 * scale
        )
        val baseLine5 = rectF5.centerY() + distance
        canvas?.drawText(
            data[5].invitation_num.toString(),
            rectF5.centerX(),
            baseLine5,
            mPaint4Font
        )

        //7
        mPaint4Circle.color =
            if (data[6].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 4, mHeight / 19f * 17,
            15 * scale, mPaint4Circle
        )
        val rectF6 = RectF(
            mWidth / 16f * 4 - 15 * scale,
            mHeight / 19f * 17 - 15 * scale,
            mWidth / 16f * 4 + 15 * scale,
            mHeight / 19f * 17 + 15 * scale
        )
        val baseLine6 = rectF6.centerY() + distance
        canvas?.drawText(
            data[6].invitation_num.toString(),
            rectF6.centerX(),
            baseLine6,
            mPaint4Font
        )

        //8
        mPaint4Circle.color =
            if (data[7].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 8, mHeight / 19f * 17,
            15 * scale, mPaint4Circle
        )
        val rectF7 = RectF(
            mWidth / 16f * 8 - 15 * scale,
            mHeight / 19f * 17 - 15 * scale,
            mWidth / 16f * 8 + 15 * scale,
            mHeight / 19f * 17 + 15 * scale
        )
        val baseLine7 = rectF7.centerY() + distance
        canvas?.drawText(
            data[7].invitation_num.toString(),
            rectF7.centerX(),
            baseLine7,
            mPaint4Font
        )

        //9
        mPaint4Circle.color =
            if (data[8].receive_state == 0) Color.parseColor("#c5c5c5")
            else Color.parseColor("#f51818")
        canvas?.drawCircle(
            mWidth / 16f * 12, mHeight / 19f * 17,
            15 * scale, mPaint4Circle
        )
        val rectF8 = RectF(
            mWidth / 16f * 12 - 15 * scale,
            mHeight / 19f * 17 - 15 * scale,
            mWidth / 16f * 12 + 15 * scale,
            mHeight / 19f * 17 + 15 * scale
        )
        val baseLine8 = rectF8.centerY() + distance
        canvas?.drawText(
            data[8].invitation_num.toString(),
            rectF8.centerX(),
            baseLine8,
            mPaint4Font
        )
    }


    private var lastX = 0f
    private var lastY = 0f

    /**
     * 监听点击事件
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
     * 获取点击的index
     */
    private fun getIndex(x: Float, y: Float) {
        if (x in mWidth / 16f * 4 - 30 * scale..mWidth / 16f * 4 + 30 * scale
            && y in mHeight / 19f * 5 - 82 * scale..mHeight / 19f * 5 - 22 * scale
        ) {
            mOnCallBack?.click(0)
        } else if (x in mWidth / 16f * 8 - 30 * scale..mWidth / 16f * 8 + 30 * scale
            && y in mHeight / 19f * 5 - 82 * scale..mHeight / 19f * 5 - 22 * scale
        ) {
            mOnCallBack?.click(1)
        } else if (x in mWidth / 16f * 12 - 30 * scale..mWidth / 16f * 12 + 30 * scale
            && y in mHeight / 19f * 5 - 82 * scale..mHeight / 19f * 5 - 22 * scale
        ) {
            mOnCallBack?.click(2)
        } else if (x in mWidth / 16f * 12 - 30 * scale..mWidth / 16f * 12 + 30 * scale
            && y in mHeight / 19f * 11 - 82 * scale..mHeight / 19f * 11 - 22 * scale
        ) {
            mOnCallBack?.click(3)
        } else if (x in mWidth / 16f * 8 - 30 * scale..mWidth / 16f * 8 + 30 * scale
            && y in mHeight / 19f * 11 - 82 * scale..mHeight / 19f * 11 - 22 * scale
        ) {
            mOnCallBack?.click(4)
        } else if (x in mWidth / 16f * 4 - 30 * scale..mWidth / 16f * 4 + 30 * scale
            && y in mHeight / 19f * 11 - 82 * scale..mHeight / 19f * 11 - 22 * scale
        ) {
            mOnCallBack?.click(5)
        } else if (x in mWidth / 16f * 4 - 30 * scale..mWidth / 16f * 4 + 30 * scale
            && y in mHeight / 19f * 17 - 82 * scale..mHeight / 19f * 17 - 22 * scale
        ) {
            mOnCallBack?.click(6)
        } else if (x in mWidth / 16f * 8 - 30 * scale..mWidth / 16f * 8 + 30 * scale
            && y in mHeight / 19f * 17 - 82 * scale..mHeight / 19f * 17 - 22 * scale
        ) {
            mOnCallBack?.click(7)
        } else if (x in mWidth / 16f * 12 - 30 * scale..mWidth / 16f * 12 + 30 * scale
            && y in mHeight / 19f * 17 - 82 * scale..mHeight / 19f * 17 - 22 * scale
        ) {
            mOnCallBack?.click(8)
        }
    }

    private var mOnCallBack: OnCallBack? = null

    interface OnCallBack {
        fun click(index: Int)
    }

    fun setOnCallBack(onCallBack: OnCallBack) {
        this.mOnCallBack = onCallBack
    }

    /**
     * 更新状态
     */
    fun changeState(treasureList: List<NewYear4InviteBean.Data.Treasure>) {
        this.mTreasureList = treasureList
        postInvalidate()
    }

}