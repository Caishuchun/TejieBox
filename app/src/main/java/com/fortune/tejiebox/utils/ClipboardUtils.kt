package com.fortune.tejiebox.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.fortune.tejiebox.R

/**
 * 剪切板工具类
 */
object ClipboardUtils {

    private var manager: ClipboardManager? = null

    /**
     * 获取剪切板数据
     */
    fun getClipboardContent(activity: Activity): String {
        try {
            if (manager == null) {
                manager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            }
            val primaryClip = manager?.primaryClip
            if (primaryClip == null || primaryClip.itemCount <= 0) {
                return ""
            }
            val item0Content = primaryClip.getItemAt(0)
            //清空剪切板
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //api28以上
                manager?.clearPrimaryClip()
            } else {
                manager?.setPrimaryClip(ClipData(null))
            }
            return if (item0Content == null || item0Content.text == null) {
                ""
            } else {
                item0Content.text.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 填充数据到剪切板
     */
    fun setClipboardContent(activity: Activity, content: String) {
        try {
            if (manager == null) {
                manager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            }
            val mClipData =
                ClipData.newPlainText(activity.resources.getString(R.string.app_name), content)
            manager?.setPrimaryClip(mClipData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}