package com.fortune.tejiebox.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import com.fortune.tejiebox.R
import com.fortune.tejiebox.utils.LogUtils

class FloatView(context: Context, attributeSet: AttributeSet) :
    RelativeLayout(context, attributeSet) {

    private val MORE_SPACE = 10.0 //移动距离像素,超过才算移动
    private var mView: View? = null

    init {
        mView = LayoutInflater.from(context).inflate(R.layout.layout_float_view, this, true)
    }

    //控件的宽高
    private var mWidth = 0
    private var mHeight = 0
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
    }

    //屏幕宽高以及半屏和上下边距
    private var mScreenWith = 0
    private var mScreenHeight = 0
    private var halfWith = 0
    private var mTop = 0
    private var mBottom = 0

    /**
     * 设置父布局的大小
     */
    fun setParentSize(width: Int, height: Int, top: Int, bottom: Int) {
        mScreenWith = width
        mScreenHeight = height
        halfWith = width / 2

        mTop = top
        mBottom = bottom
    }

    private var lastX = 0f //按下x
    private var lastY = 0f //按下y
    private var isMove = false //是拖动?
    private var isPress = false //是点击?
    private var offsetX = 0f  //偏移量x,移动的时候不加偏移量,按钮会发生位置错误,手指下会是按钮的左上角,而不是最初按下位置
    private var offsetY = 0f //偏移量y
    private var isChangeBg = false //吸边的时候可以更改变化按钮的左右两边圆角,后面美术改了个方图,不用了,但思路留在这

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.rawX
        val y = ev.rawY
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isMove = false
                isPress = true
                lastX = x
                lastY = y
                offsetX = getX() - x
                offsetY = getY() - y
            }
            MotionEvent.ACTION_MOVE -> {
                LogUtils.d("+++++++++x:$x,y:$y")
                isMove = isMove(ev)
                isPress = if (isMove) {
                    if (!isChangeBg) {
                        isChangeBg = true
                        //思路:这里换背景图
                    }
                    val xandY = getXandY(x + offsetX, y + offsetY)
                    setX(xandY[0].toFloat())
                    setY(xandY[1].toFloat())
                    false
                } else {
                    true
                }
            }
            MotionEvent.ACTION_UP -> {
                isChangeBg = false
                if (isPress) {
                    callback?.click()
                    isPress = true
                }
                if (isMove) {
                    isMove = false
                }
                if (x != 0f || x != (mScreenWith - mWidth).toFloat()) {
                    if (x <= halfWith) {
                        //在左半边,吸附左边
//                        if (x > lastX) {
                        animate().setInterpolator(DecelerateInterpolator())
                            .setDuration(500)
//                                .xBy(-x - offsetX)
                            .xBy(-getX())
                            .start()
                        //思路:这里换背景图
//                        }
                    } else {
                        //在右半边,吸附右边
//                        if (x < lastX) {
                        animate().setInterpolator(DecelerateInterpolator())
                            .setDuration(500)
//                                .xBy(mScreenWith - mWidth - x - offsetX)
                            .xBy(mScreenWith - getX() - mWidth)
                            .start()
                        //思路:这里换背景图
//                        }
                    }
                }
            }
        }
        return true
    }

    /**
     * 获取移动到的x和y
     */
    private fun getXandY(x: Float, y: Float): IntArray {
        val resultX = when {
            x < 0 -> {
                0
                //思路:这里换背景图
            }
            x > mScreenWith - mWidth -> {
                mScreenWith - mWidth
                //思路:这里换背景图
            }
            else -> {
                x.toInt()
            }
        }
        val resultY: Int = when {
            y < mTop -> {
                mTop
            }
            y > mScreenHeight - mBottom - mWidth -> {
                mScreenHeight - mBottom - mWidth
            }
            else -> {
                y.toInt()
            }
        }
        return intArrayOf(resultX, resultY)
    }


    private var callback: OnClickCallback? = null

    fun setOnClickCallback(callback: OnClickCallback) {
        this.callback = callback
    }

    interface OnClickCallback {
        fun click()
    }

    private fun isMove(ev: MotionEvent): Boolean {
        val x = ev.rawX
        val y = ev.rawY
        val moreSpace =
            Math.sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY)).toDouble())
        return moreSpace > MORE_SPACE
    }
}