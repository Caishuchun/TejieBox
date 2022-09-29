package com.fortune.tejiebox.utils

import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.orhanobut.logger.Logger

/**
 * 日志工具类
 */

object LogUtils {
    const val isDebug = BaseAppUpdateSetting.isDebug

    /**
     * debug 日志
     */
    fun d(log: Any) {
        if (isDebug)
            Logger.d(log)
    }

    /**
     * error 日志
     */
    fun e(log: Any) {
        if (isDebug)
            Logger.e(log.toString())
    }

    /**
     * info 日志
     */
    fun i(log: Any) {
        if (isDebug)
            Logger.i(log.toString())
    }

}