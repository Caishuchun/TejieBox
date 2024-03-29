package com.fortune.tejiebox.utils

import android.app.Activity
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 推广工具类
 */
object PromoteUtils {

    //阿里推广链接
//    private const val promoteUrl4Ali = "http://tjhz.jinangedesm.com.cn/api/tj.php"
    private const val promoteUrl4Ali = "http://tjhz.52ww.com/api/tj.php"

    //百度推广链接
//    private const val promoteUrl4Baidu = "http://tjhz.jinangedesm.com.cn/api/baidu/api.php?type=1"
//    private const val promoteUrl4Baidu2 = "http://tjhz.jinangedesm.com.cn/api/baidu2/api.php?type=1"
    private const val promoteUrl4Baidu = "http://tjhz.52ww.com/api/baidu/api.php?type=1"
    private const val promoteUrl4Baidu2 = "http://tjhz.52ww.com/api/baidu2/api.php?type=1"

    //    百度新推广_激活
    private const val promoteUrl4BaiduActivate = "http://ocpc.tjbox.lelehuyu.com/api.php?a=activate"

    //    百度新推广_注册
    private const val promoteUrl4BaiduRegister = "http://ocpc.tjbox.lelehuyu.com/api.php?a=register"

    //51统计链接
    private const val promoteUrl451 = "https://www.51cr.com/d.php?id=21511|2&f="

    /**
     * 推广_注册
     */
    fun promote(context: Activity) {
        promote4Ali()
        promote4Baidu()
        promote4Baidu2()
        promote4BaiduRegister()
        promote451()
    }

    /**
     * 激活
     */
    fun activate(context: Activity) {
        promote4BaiduActivate()
    }

    /**
     * 百度推广激活
     */
    private fun promote4BaiduActivate() {
        Thread {
            val url = URL(promoteUrl4BaiduActivate)
            try {
                val httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connectTimeout = 10000
                httpURLConnection.readTimeout = 10000
                httpURLConnection.requestMethod = "GET"
                val responseCode = httpURLConnection.responseCode
                LogUtils.d("推广请求_百度激活---$responseCode,${httpURLConnection.responseMessage}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
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

    /**
     * 百度推广3
     */
    private fun promote4BaiduRegister() {
        Thread {
            val url = URL(promoteUrl4BaiduRegister)
            try {
                val httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.connectTimeout = 10000
                httpURLConnection.readTimeout = 10000
                httpURLConnection.requestMethod = "GET"
                val responseCode = httpURLConnection.responseCode
                LogUtils.d("推广请求_百度注册---$responseCode,${httpURLConnection.responseMessage}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
}