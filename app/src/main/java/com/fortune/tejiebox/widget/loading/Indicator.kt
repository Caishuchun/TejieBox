package com.fortune.tejiebox.widget.loading

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.*

import android.graphics.drawable.Animatable

import android.graphics.drawable.Drawable


abstract class Indicator : Drawable(), Animatable {
    private val mUpdateListeners: HashMap<ValueAnimator, AnimatorUpdateListener> = HashMap()
    private var mAnimators: ArrayList<ValueAnimator>? = null
    private var alpha = 255
    private var drawBounds: Rect = ZERO_BOUNDS_RECT
    private var mHasAnimators = false
    private val mPaint: Paint = Paint()
    var color: Int
        get() = mPaint.color
        set(color) {
            mPaint.color = color
        }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha
    }

    override fun getAlpha(): Int {
        return alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun draw(canvas: Canvas) {
        draw(canvas, mPaint)
    }

    abstract fun draw(canvas: Canvas?, paint: Paint?)
    abstract fun onCreateAnimators(): ArrayList<ValueAnimator>?
    override fun start() {
        ensureAnimators()
        if (mAnimators == null) {
            return
        }

        // If the animators has not ended, do nothing.
        if (isStarted) {
            return
        }
        startAnimators()
        invalidateSelf()
    }

    private fun startAnimators() {
        for (i in 0 until mAnimators!!.size) {
            val animator = mAnimators!![i]

            //when the animator restart , add the updateListener again because they
            // was removed by animator stop .
            val updateListener = mUpdateListeners[animator]
            if (updateListener != null) {
                animator.addUpdateListener(updateListener)
            }
            animator.start()
        }
    }

    private fun stopAnimators() {
        if (mAnimators != null) {
            for (animator in mAnimators!!) {
                if (animator != null && animator.isStarted) {
                    animator.removeAllUpdateListeners()
                    animator.end()
                }
            }
        }
    }

    private fun ensureAnimators() {
        if (!mHasAnimators) {
            mAnimators = onCreateAnimators()
            mHasAnimators = true
        }
    }

    override fun stop() {
        stopAnimators()
    }

    private val isStarted: Boolean
        private get() {
            for (animator in mAnimators!!) {
                return animator.isStarted
            }
            return false
        }

    override fun isRunning(): Boolean {
        for (animator in mAnimators!!) {
            return animator.isRunning
        }
        return false
    }

    /**
     * Your should use this to add AnimatorUpdateListener when
     * create animator , otherwise , animator doesn't work when
     * the animation restart .
     * @param updateListener
     */
    fun addUpdateListener(animator: ValueAnimator, updateListener: AnimatorUpdateListener) {
        mUpdateListeners[animator] = updateListener
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        setDrawBounds(bounds)
    }

    @JvmName("setDrawBounds1")
    fun setDrawBounds(drawBounds: Rect) {
        setDrawBounds(drawBounds.left, drawBounds.top, drawBounds.right, drawBounds.bottom)
    }

    fun setDrawBounds(left: Int, top: Int, right: Int, bottom: Int) {
        drawBounds = Rect(left, top, right, bottom)
    }

    fun postInvalidate() {
        invalidateSelf()
    }

    @JvmName("getDrawBounds1")
    fun getDrawBounds(): Rect {
        return drawBounds
    }

    val width: Int
        get() = drawBounds.width()
    val height: Int
        get() = drawBounds.height()

    fun centerX(): Int {
        return drawBounds.centerX()
    }

    fun centerY(): Int {
        return drawBounds.centerY()
    }

    fun exactCenterX(): Float {
        return drawBounds.exactCenterX()
    }

    fun exactCenterY(): Float {
        return drawBounds.exactCenterY()
    }

    companion object {
        private val ZERO_BOUNDS_RECT: Rect = Rect()
    }

    init {
        mPaint.color = Color.WHITE
        mPaint.style = Paint.Style.FILL
        mPaint.isAntiAlias = true
    }
}