package com.fortune.tejiebox.utils

import android.view.Choreographer

/**
 * 实时FPS工具类
 */
object FPSUtils {
    private var mStartFrameTIme: Long = 0
    private var mFrameCount = 0
    private const val MONITOR_INTERVAL = 160L
    private const val MONITOR_INTERVAL_NANOS = MONITOR_INTERVAL * 1000 * 1000

    /**
     * 获取实时FPS
     */
    fun getFps() {
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (mStartFrameTIme == 0L) {
                    mStartFrameTIme = frameTimeNanos
                }
                val interval = (frameTimeNanos - mStartFrameTIme) / 1000000.0f
                if (interval > MONITOR_INTERVAL) {
                    val fps = mFrameCount.toDouble() * 1000L / interval
                    LogUtils.d("FPSUtils==>fps:$fps,mFrameCount:$mFrameCount,mStartFrameTIme:$mStartFrameTIme,frameTimeNanos:$frameTimeNanos,interval:$interval")
                    mFrameCount = 0
                    mStartFrameTIme = 0
                } else {
                    ++mFrameCount
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }
}