package com.fortune.tejiebox.widget.bloom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import java.util.*


class ExplosionField : View {
    private val mExplosions: MutableList<ExplosionAnimator> = ArrayList()
    private val mExpandInset = IntArray(2)

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        Arrays.fill(mExpandInset, Utils.dp2Px(32))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (explosion in mExplosions) {
            explosion.draw(canvas)
        }
    }

    fun expandExplosionBound(dx: Int, dy: Int) {
        mExpandInset[0] = dx
        mExpandInset[1] = dy
    }

    fun explode(bitmap: Bitmap?, bound: Rect?, startDelay: Long, duration: Long) {
        val explosion = ExplosionAnimator(
            this,
            bitmap!!, bound
        )
        explosion.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mExplosions.remove(animation)
            }
        })
        explosion.startDelay = startDelay
        explosion.duration = duration
        mExplosions.add(explosion)
        explosion.start()
    }

    fun explode(view: View) {
        val r = Rect()
        view.getGlobalVisibleRect(r)
        val location = IntArray(2)
        getLocationOnScreen(location)
        r.offset(-location[0], -location[1])
        r.inset(-mExpandInset[0], -mExpandInset[1])
        val startDelay = 100
        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(1500)
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            var random = Random()
            override fun onAnimationUpdate(animation: ValueAnimator) {
                view.translationX = (random.nextFloat() - 0.5f) * view.width * 0.05f
                view.translationY = (random.nextFloat() - 0.5f) * view.height * 0.05f
            }
        })
        animator.start()
        view.animate().setDuration(150).setStartDelay(startDelay.toLong()).scaleX(0f).scaleY(0f)
            .alpha(0f).start()
        explode(
            Utils.createBitmapFromView(view),
            r,
            startDelay.toLong(),
            ExplosionAnimator.DEFAULT_DURATION
        )
    }

    fun clear() {
        mExplosions.clear()
        invalidate()
    }

    companion object {
        fun attach2Window(activity: Activity): ExplosionField {
            val rootView = activity.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup
            val explosionField = ExplosionField(activity)
            rootView.addView(
                explosionField, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            return explosionField
        }
    }
}