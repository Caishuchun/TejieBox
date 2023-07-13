package com.fortune.tejiebox.utils

import android.content.Context
import com.fortune.tejiebox.R

/**
 * 网络异常提示信息
 */
object HttpExceptionUtils {

    /**
     * 网络异常提示
     */
    fun getExceptionMsg(context: Context, throwable: Throwable?): String {
        if (throwable?.message != null &&
            (throwable.message!!.contains("No address associated with hostname") ||
                    throwable.message!!.contains("No address associated with hostname") ||
                    throwable.message!!.contains("failed to connect") ||
                    throwable.message!!.contains("Failed to connect") ||
                    throwable.message!!.contains("reset"))
        ) {
            return "网络异常，请检查网络设置"
//            return context.getString(R.string.network_fail_to_connect)
        } else if (throwable?.message != null &&
            (throwable.message!!.contains("time out") ||
                    throwable.message!!.contains("timed out") ||
                    throwable.message!!.contains("timeout") ||
                    throwable.message!!.contains("timedout"))
        ) {
            return "网络连接超时，请稍后重试"
//            return context.getString(R.string.network_fail_to_connect)
        } else {
            return context.getString(R.string.network_fail_to_request)
        }
    }

}