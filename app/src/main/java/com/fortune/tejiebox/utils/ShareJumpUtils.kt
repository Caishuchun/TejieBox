package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import com.fortune.tejiebox.bean.VersionBean
import com.tencent.connect.share.QQShare
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX.Req.WXSceneSession
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import kotlinx.android.synthetic.main.layout_dialog_default.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*


/**
 * 分享跳转
 */
object ShareJumpUtils {
    private var mDialog: BaseDialog? = null
    private var mTencent: Tencent? = null
    private var mWXApi: IWXAPI? = null

    fun dismissDialog() {
        if (mDialog != null) {
            if (mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
            mDialog = null
        }
        mTencent = null
        mWXApi = null
    }

    /**
     * 分享dialog
     */
    @SuppressLint("SetTextI18n")
    fun showDefaultDialog(
        context: Activity,
        message: String? = null,
        bitmapFilePath: String? = null
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_default)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.tv_dialog_default_title?.text = "特戒分享邀请"
        mDialog?.tv_dialog_default_message?.text =
            if (bitmapFilePath != null) {
                "可将分享邀请图片在QQ和微信上分享给好友!"
            } else {
                "可将分享邀请链接在QQ和微信上分享给好友!"
            }
        mDialog?.tv_dialog_default_cancel?.text = "微信"
        mDialog?.tv_dialog_default_sure?.text = "QQ"
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            dismissDialog()
            share(context, 1, message, bitmapFilePath)
        }
        mDialog?.tv_dialog_default_cancel?.setOnClickListener {
            dismissDialog()
            share(context, 0, message, bitmapFilePath)
        }
        mDialog?.setOnCancelListener {
            dismissDialog()
        }
        mDialog?.show()
    }

    /**
     * 分享打开
     * @param type 0微信 1QQ
     */
    private fun share(context: Activity, type: Int, message: String?, bitmapFilePath: String?) {
        when (type) {
            0 -> {
                if (VersionBean.getData()?.isCanUseShare == 1) {
                    if (mWXApi == null) {
                        mWXApi = WXAPIFactory.createWXAPI(context, "wx10a73dd0bd989acf", true)
                    }
                    val registerApp = mWXApi?.registerApp("wx10a73dd0bd989acf")
                    LogUtils.d("isRegisterApp1: $registerApp")
                    context.registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            mWXApi?.registerApp("wx10a73dd0bd989acf")
                            LogUtils.d("isRegisterApp2: $registerApp")
                        }
                    }, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))

                    val wxAppInstalled = mWXApi?.isWXAppInstalled
                    if (wxAppInstalled == null || wxAppInstalled == false) {
                        ToastUtils.show("请先安装最新版微信!")
                        return
                    }
                    toWxShare(context, message, bitmapFilePath)
                } else {
                    if (!checkApkExist(context, "com.tencent.mm")) {
                        ToastUtils.show("请先安装最新版微信!")
                        return
                    }
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    if (bitmapFilePath != null) {
                        shareIntent.type = "image/*"
                        val uri: Uri = Uri.parse(
                            MediaStore.Images.Media.insertImage(
                                context.contentResolver,
                                bitmapFilePath,
                                "IMG" + Calendar.getInstance().time,
                                null
                            )
                        )
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    } else {
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, message)
                    }
                    shareIntent.setPackage("com.tencent.mm")
                    shareIntent.setClassName(
                        "com.tencent.mm",
                        "com.tencent.mm.ui.tools.ShareImgUI"
                    )
                    val chooserIntent = Intent.createChooser(shareIntent, "特戒盒子分享") ?: return
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                }
            }

            1 -> {
                if (VersionBean.getData()?.isCanUseShare == 1) {
                    if (mTencent == null) {
                        mTencent = Tencent.createInstance("1112216471", context.applicationContext)
                    }
                    val qqInstalled = mTencent?.isQQInstalled(context)
                    if (qqInstalled == null || qqInstalled == false) {
                        ToastUtils.show("请先安装最新版QQ!")
                        return
                    }
                    toQQShare(context, message, bitmapFilePath)
                } else {
                    if (!checkApkExist(context, "com.tencent.mobileqq")) {
                        ToastUtils.show("请先安装最新版QQ!")
                        return
                    }
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    if (bitmapFilePath != null) {
                        shareIntent.type = "image/*"
                        val uri: Uri = FileProvider.getUriForFile(
                            context, context.packageName + ".provider", File(bitmapFilePath)
                        )
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    } else {
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, message)
                    }
                    shareIntent.setPackage("com.tencent.mobileqq")
                    shareIntent.setClassName(
                        "com.tencent.mobileqq",
                        "com.tencent.mobileqq.activity.JumpActivity"
                    )
                    val chooserIntent = Intent.createChooser(shareIntent, "特戒盒子分享") ?: return
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                }
            }
        }
    }

    /**
     * 调用微信分享SDK, 分享到微信
     */
    private fun toWxShare(context: Activity, message: String?, bitmapFilePath: String?) {
        val req = SendMessageToWX.Req()
        if (bitmapFilePath == null) {
            val url = message!!.split(": ")[1]
            val wxWebpageObject = WXWebpageObject()
            wxWebpageObject.webpageUrl = url

            val wxMediaMessage = WXMediaMessage(wxWebpageObject)
            wxMediaMessage.title = "特戒邀请"
            wxMediaMessage.description =
                "免费充值天天送，好玩的服处处有。立即点击下载${context.resources.getString(R.string.app_name)}"
            val thumbBmp = BitmapFactory.decodeResource(context.resources, R.drawable.icon)
            wxMediaMessage.thumbData = bmpToByteArray(thumbBmp, true)

            req.transaction = "webpage"
            req.message = wxMediaMessage
        } else {
            val bitmap = BitmapFactory.decodeFile(bitmapFilePath)
            val wxImageObject = WXImageObject(bitmap)
            val wxMediaMessage = WXMediaMessage()
            wxMediaMessage.mediaObject = wxImageObject

            //请勿问我为啥用 '耀' 这个字，这问题问微信 SDK 开发者去，他就是这么判断的
            val thumbBmp = Bitmap.createScaledBitmap(bitmap, '耀'.code, '耀'.code, true)
            wxMediaMessage.thumbData = bmpToByteArray(thumbBmp, true)

            req.transaction = "img"
            req.message = wxMediaMessage
        }
        req.scene = WXSceneSession
        mWXApi?.sendReq(req)
    }

    private fun bmpToByteArray(bmp: Bitmap, needRecycle: Boolean): ByteArray? {
        var data = bmpToByteArray(bmp, 100)
        LogUtils.d("zipBitmap: quality=100" + "   size=" + data.size)
        var i = 100
        while (data.size > '耀'.code) {
            //请勿问我为啥用 '耀' 这个字，这问题问微信 SDK 开发者去，他就是这么判断的
            i = if (i > 10) {
                i - 10
            } else {
                i - 1
            }
            if (i <= 0) {
                LogUtils.e("zipBitmap: 失败，很无奈清晰度已经降为0，但压缩的图像依然不符合微信的要求，最后size=" + data.size)
                break
            }
            data = bmpToByteArray(bmp, i)
            LogUtils.d("zipBitmap: quality=" + i + "   size=" + data.size)
        }
        if (needRecycle) bmp.recycle()
        return data
    }

    private fun bmpToByteArray(bmp: Bitmap, quality: Int): ByteArray {
        var i: Int
        var j: Int
        if (bmp.height > bmp.width) {
            i = bmp.width
            j = bmp.width
        } else {
            i = bmp.height
            j = bmp.height
        }
        val localBitmap = Bitmap.createBitmap(i, j, Bitmap.Config.RGB_565)
        val localCanvas = Canvas(localBitmap)
        while (true) {
            localCanvas.drawBitmap(bmp, Rect(0, 0, i, j), Rect(0, 0, i, j), null)
            val localByteArrayOutputStream = ByteArrayOutputStream()
            localBitmap.compress(
                Bitmap.CompressFormat.JPEG, quality,
                localByteArrayOutputStream
            )
            localBitmap.recycle()
            val arrayOfByte = localByteArrayOutputStream.toByteArray()
            try {
                localByteArrayOutputStream.close()
                return arrayOfByte
            } catch (e: Exception) {
                //F.out(e);
            }
            i = bmp.height
            j = bmp.height
        }
    }

    /**
     * 调用腾讯分享SDK, 分享到QQ
     */
    private fun toQQShare(context: Activity, message: String?, bitmapFilePath: String?) {
        val params = Bundle()
        if (bitmapFilePath == null) {
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT)
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, message!!.split(": ")[1])
            params.putString(
                QQShare.SHARE_TO_QQ_IMAGE_URL,
                "http://tejie-box.oss-cn-hangzhou.aliyuncs.com/apk/setting/icon_512.png"
            )
        } else {
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE)
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, bitmapFilePath)
        }
        params.putString(QQShare.SHARE_TO_QQ_TITLE, "特戒邀请")
        params.putString(
            QQShare.SHARE_TO_QQ_SUMMARY,
            "免费充值天天送，好玩的服处处有。立即点击下载${context.resources.getString(R.string.app_name)}"
        )
        params.putString(
            QQShare.SHARE_TO_QQ_APP_NAME,
            context.resources.getString(R.string.app_name)
        )
        mTencent?.shareToQQ(context, params, object : IUiListener {
            override fun onComplete(p0: Any?) {
                LogUtils.d("onComplete:$p0")
            }

            override fun onError(p0: UiError?) {
                LogUtils.d("onError:${p0?.errorMessage}")
            }

            override fun onCancel() {
                LogUtils.d("onCancel:")
            }

            override fun onWarning(p0: Int) {
                LogUtils.d("onWarning:$p0")
            }
        })
    }

    /**
     * 检查是否安装了APP
     */
    private fun checkApkExist(context: Activity, packageName: String): Boolean {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(
                packageName,
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}