package com.fortune.tejiebox.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import java.util.*


object ShareUtils {

    /**
     * 分享文字
     */
    fun shareText(context: Context, text: String) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "text/plain"

        val resolveInfo: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(shareIntent, 0)
        if (resolveInfo.isEmpty()) {
            ToastUtils.show("请先安装微信或QQ!")
            return
        }
        val targetIntents = arrayListOf<Intent>()
        for (info in resolveInfo) {
            val activityInfo = info.activityInfo
            LogUtils.d("===============${activityInfo.packageName}, ${activityInfo.name}")
            if (activityInfo.packageName == "com.tencent.mm" || activityInfo.packageName == "com.tencent.mobileqq") {
                val targetIntent = Intent()
                targetIntent.action = Intent.ACTION_SEND
                targetIntent.type = "text/plain"
                targetIntent.putExtra(Intent.EXTRA_TEXT, text)
                targetIntent.setPackage(activityInfo.packageName)
                targetIntent.setClassName(activityInfo.packageName, activityInfo.name)
                targetIntents.add(targetIntent)
            }
        }
        if (targetIntents.isEmpty()) {
            ToastUtils.show("请先安装微信或QQ!")
            return
        }
        val chooser = Intent.createChooser(targetIntents[0], "特戒盒子分享")
        chooser.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            targetIntents.toArray(arrayOf<Parcelable>())
        )
        context.startActivity(chooser)
    }

    /**
     * 分享图片
     */
    fun shareImage(context: Context, bitmap: Bitmap) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "image/*"
        val resolveInfo: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(shareIntent, 0)
        if (resolveInfo.isEmpty()) {
            ToastUtils.show("请先安装微信或QQ!")
            return
        }
        val targetIntents = arrayListOf<Intent>()
        for (info in resolveInfo) {
            val activityInfo = info.activityInfo
            LogUtils.d("===============${activityInfo.packageName}, ${activityInfo.name}")
            if (activityInfo.packageName == "com.tencent.mm" || activityInfo.packageName == "com.tencent.mobileqq") {
                val targetIntent = Intent()
                targetIntent.action = Intent.ACTION_SEND
                shareIntent.type = "image/*"
                val uri: Uri = Uri.parse(
                    MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        "IMG" + Calendar.getInstance().time,
                        null
                    )
                )
                targetIntent.putExtra(Intent.EXTRA_STREAM, uri)
                targetIntent.setPackage(activityInfo.packageName)
                targetIntent.setClassName(activityInfo.packageName, activityInfo.name)
                targetIntents.add(targetIntent)
            }
        }
        if (targetIntents.isEmpty()) {
            ToastUtils.show("请先安装微信或QQ!")
            return
        }
        val chooser = Intent.createChooser(targetIntents[0], "特戒盒子分享")
        chooser.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            targetIntents.toArray(arrayOf<Parcelable>())
        )
        context.startActivity(chooser)
    }
}