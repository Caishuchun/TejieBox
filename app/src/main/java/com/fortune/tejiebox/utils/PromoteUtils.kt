package com.fortune.tejiebox.utils

import android.app.Activity
import com.fortune.tejiebox.constants.SPArgument
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 推广工具类
 */
object PromoteUtils {

    //阿里推广链接
    private const val promoteUrl4Ali = "http://tjhz.jinangedesm.com.cn/api/tj.php"
    private const val promoteUrl4Baidu = "http://tjhz.jinangedesm.com.cn/api/baidu/api.php?type=1"

    /**
     * 推广
     */
    fun promote(context: Activity) {
        val isLogined = SPUtils.getBoolean(SPArgument.IS_LOGIN_ED, false)
        if (isLogined) {
            return
        }
        promote4Ali(context)
        promote4Baidu(context)
    }

    /**
     * 阿里推广
     */
    private fun promote4Ali(context: Activity) {
        Thread {
            val url = URL(promoteUrl4Ali)
            try {
                val httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connectTimeout = 10000
                httpURLConnection.readTimeout = 10000
                httpURLConnection.requestMethod = "GET"
                val responseCode = httpURLConnection.responseCode
                LogUtils.d("推广请求_阿里---$responseCode,${httpURLConnection.responseMessage}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * 百度推广
     */
    private fun promote4Baidu(context: Activity) {
        Thread {
            val url = URL(promoteUrl4Baidu)
            try {
                val httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connectTimeout = 10000
                httpURLConnection.readTimeout = 10000
                httpURLConnection.requestMethod = "GET"
                val responseCode = httpURLConnection.responseCode
                LogUtils.d("推广请求_百度---$responseCode,${httpURLConnection.responseMessage}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
}