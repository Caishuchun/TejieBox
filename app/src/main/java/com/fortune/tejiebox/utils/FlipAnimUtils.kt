package com.fortune.tejiebox.utils

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.view.View

/**
 * 实现游戏详情页的礼包领取翻转动画
 */
object FlipAnimUtils {
    private var objectAnimatorMap = mutableMapOf<View, ObjectAnimator>()

    /**
     * 动画抖动效果
     * @param view 抖动控件
     * @param scaleLarge 缩小倍数
     * @param scaleLarge 放大倍数
     * @param shakeDegrees 抖动角度
     * @param duration 动画时间
     */
    fun startShakeByPropertyAnim(
        view: View?,
        scaleSmall: Float,
        scaleLarge: Float,
        shakeDegrees: Float,
        duration: Long
    ) {
        if (view == null) {
            return
        }
        val scaleXValuesHolder = PropertyValuesHolder.ofKeyframe(
            View.SCALE_X,
            Keyframe.ofFloat(0f, 1.0f),
            Keyframe.ofFloat(0.25f, scaleSmall),
            Keyframe.ofFloat(0.5f, scaleLarge),
            Keyframe.ofFloat(0.75f, scaleLarge),
            Keyframe.ofFloat(1.0f, 1.0f)
        )
        val scaleYValuesHolder = PropertyValuesHolder.ofKeyframe(
            View.SCALE_Y,
            Keyframe.ofFloat(0f, 1.0f),
            Keyframe.ofFloat(0.25f, scaleSmall),
            Keyframe.ofFloat(0.5f, scaleLarge),
            Keyframe.ofFloat(0.75f, scaleLarge),
            Keyframe.ofFloat(1.0f, 1.0f)
        )
        val rotateValuesHolder = PropertyValuesHolder.ofKeyframe(
            View.ROTATION,
            Keyframe.ofFloat(0f, 0f),
            Keyframe.ofFloat(0.1f, -shakeDegrees),
            Keyframe.ofFloat(0.2f, shakeDegrees),
            Keyframe.ofFloat(0.3f, -shakeDegrees),
            Keyframe.ofFloat(0.4f, shakeDegrees),
            Keyframe.ofFloat(0.5f, -shakeDegrees),
            Keyframe.ofFloat(0.6f, shakeDegrees),
            Keyframe.ofFloat(0.7f, -shakeDegrees),
            Keyframe.ofFloat(0.8f, shakeDegrees),
            Keyframe.ofFloat(0.9f, -shakeDegrees),
            Keyframe.ofFloat(1.0f, 0f)
        )
        val objectAnimator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            scaleXValuesHolder,
            scaleYValuesHolder,
            rotateValuesHolder
        )
        objectAnimator.duration = duration
        objectAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                val message = Message()
                message.what = 101
                message.obj = view
                mHandler.sendMessageDelayed(message, 2000)
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        })
        objectAnimatorMap[view] = objectAnimator
        objectAnimator.start()
    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                101 -> {
                    val view = msg.obj as View
                    val objectAnimator = objectAnimatorMap[view]
                    objectAnimator?.start()
                }
                102 -> {
                    val view = msg.obj as View
                    val objectAnimator = objectAnimatorMap[view]
                    objectAnimator?.cancel()
                    view.animate().rotation(0f).setDuration(60).start()
                    objectAnimatorMap.remove(view)
                }
            }
        }
    }

    /**
     * 停止抖动动画
     */

    fun stopShakeByPropertyAnim(view: View) {
        val message = Message()
        message.what = 102
        message.obj = view
        mHandler.sendMessage(message)
    }

}