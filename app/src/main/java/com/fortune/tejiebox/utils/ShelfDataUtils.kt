package com.fortune.tejiebox.utils

import android.os.Environment
import com.fortune.tejiebox.bean.ShelfDataBean
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.TimeUnit

object ShelfDataUtils {

    interface OnGetShelfDataCallback {
        fun fail(msg: String)

        fun success(shelfDataBean: ShelfDataBean)
    }

    /**
     * 获取渠道信息从服务器
     */
    fun getShelfData4Service(callback: OnGetShelfDataCallback) {
        val shelfDataRequest = Request.Builder()
            .url("https://cdn.tjbox.lelehuyu.com/apk/setting/shelf_setting.json")
            .build()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
        val shelfDataCall = okHttpClient.newCall(shelfDataRequest)
        shelfDataCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogUtils.d("getShelfData=>onFailure(e:$e)")
                callback.fail("Network request failed.")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseStr = response.body()?.string()
                if (responseStr == null) {
                    LogUtils.d("getShelfData=>onResponse: responseStr is null")
                    callback.fail("Response body is null")
                }
                try {
                    val shelfDataBean = Gson().fromJson(responseStr, ShelfDataBean::class.java)
                    if (shelfDataBean != null) {
                        LogUtils.d("getShelfData=>onResponse: $shelfDataBean")
                        checkShelfDate4Local(shelfDataBean)
                        callback.success(shelfDataBean)
                    } else {
                        LogUtils.d("getShelfData=>onResponse: shelfDataBean is null")
                        callback.fail("ShelfDataBean is null")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback.fail("Format data error")
                }
            }
        })
    }

    /**
     * 检查本地的渠道上架信息
     */
    fun checkShelfDate4Local(shelfDataBean: ShelfDataBean) {
        val shelfData4Local = getShelfData4Local()
        if (shelfData4Local == null) {
            //本地没有, 直接存
            saveShelfData2Local(shelfDataBean)
        } else {
            if (shelfDataBean.update_time > shelfData4Local.update_time) {
                //服务器版本高于本地版本, 直接存
                saveShelfData2Local(shelfDataBean)
            }
        }
    }

    /**
     * 保存渠道上架信息到本地
     */
    private fun saveShelfData2Local(shelfDataBean: ShelfDataBean) {
        try {
            val path = "${Environment.getExternalStorageDirectory()}/.shelf_setting.json"
            LogUtils.d("+++++++++++++++++saveShelfData2Local=>path:$path")
            val saveShelfDataFile = File(path)
            if (!saveShelfDataFile.exists()) {
                saveShelfDataFile.createNewFile()
            }
            val bufferedWriter = BufferedWriter(FileWriter(path, false))
            bufferedWriter.write(Gson().toJson(shelfDataBean))
            bufferedWriter.close()
            LogUtils.d("+++++++++++++++++saveShelfData2Local=>over")
        } catch (e: Exception) {
            LogUtils.d("+++++++++++++++++saveShelfData2Local=>exception:${e.message}")
        }
    }

    /**
     * 获取渠道上架信息从本地
     */
    fun getShelfData4Local(): ShelfDataBean? {
        var bean: ShelfDataBean? = null
        try {
            val path = "${Environment.getExternalStorageDirectory()}/.shelf_setting.json"
            LogUtils.d("+++++++++++++++++getShelfData4Local=>path:$path")
            val saveShelfDataFile = File(path)
            if (saveShelfDataFile.exists()) {
                val stringBuilder = StringBuilder()
                val bufferedReader = BufferedReader(FileReader(path))
                var line = bufferedReader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = bufferedReader.readLine()
                }
                bufferedReader.close()
                LogUtils.d("+++++++++++++++++getShelfData4Local=>readOver:${stringBuilder.toString()}")
                Gson().fromJson(stringBuilder.toString(), ShelfDataBean::class.java)?.let {
                    bean = it
                }
            } else {
                LogUtils.d("+++++++++++++++++getShelfData4Local=>no file")
                bean = null
            }
        } catch (e: Exception) {
            LogUtils.d("+++++++++++++++++getShelfData4Local=>exception:${e.message}")
            bean = null
        }
        return bean
    }
}