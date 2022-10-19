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

    //百度推广链接
    private const val promoteUrl4Baidu = "http://tjhz.jinangedesm.com.cn/api/baidu/api.php?type=1"
    private const val promoteUrl4Baidu2 = "http://tjhz.jinangedesm.com.cn/api/baidu2/api.php?type=1"

    //51统计链接
    private const val promoteUrl451 = "https://www.51cr.com/d.php?id=21511|2&f="

    /**
     * 推广
     */
    fun promote(context: Activity) {
        promote4Ali()
        promote4Baidu()
        promote4Baidu2()
        promote451()
    }

    /**
     * 51数据统计
     */
    private fun promote451() {
        Thread {
            val url = URL(promoteUrl451)
            try {
                val httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connectTimeout = 10000
                httpURLConnection.readTimeout = 10000
                httpURLConnection.requestMethod = "GET"
                val responseCode = httpURLConnection.responseCode
                LogUtils.d("51统计---$responseCode,${httpURLConnection.responseMessage}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * 阿里推广
     */
    private fun promote4Ali() {
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
    private fun promote4Baidu() {
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

    /**
     * 百度推广1
     */
    private fun promote4Baidu2() {
        Thread {
            val url = URL(promoteUrl4Baidu2)
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