package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.SharePlatform
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import kotlinx.android.synthetic.main.layout_dialog_default.*
import java.io.File
import java.util.*

/**
 * 分享跳转
 */
object ShareJumpUtils {
    private var mDialog: BaseDialog? = null

    fun dismissDialog() {
        if (mDialog != null) {
            if (mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
            mDialog = null
        }
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
                "可将分享邀请图片在QQ和微信上分享给好用!"
            } else {
                "可将分享邀请链接在QQ和微信上分享给好用!"
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
                reportShare(type)
                context.startActivity(chooserIntent)
            }
            1 -> {
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
                reportShare(type)
                context.startActivity(chooserIntent)
            }
        }
    }

    /**
     * openInstall 分享统计
     */
    private fun reportShare(type: Int) {
        OpenInstall.reportShare(
            "10086",
            if (type == 0) SharePlatform.WechatSession else SharePlatform.QQ
        ) { ingnore, errror ->
            if (errror != null) {
                LogUtils.d("OpenInstall==========分享上报失败：$errror")
            } else {
                LogUtils.d("OpenInstall==========分享上报成功")
            }
        }
    }

    /**
     * 分享打开
     * @param type 0微信 1QQ
     */
    private fun share(context: Activity, type: Int) {
        when (type) {
            0 -> {
                if (!checkApkExist(context, "com.tencent.mm")) {
                    ToastUtils.show("请先安装最新版微信!")
                    return
                }
                val intent = Intent(Intent.ACTION_MAIN)
                val cmp = ComponentName(
                    "com.tencent.mm",
                    "com.tencent.mm.ui.LauncherUI"
                )
                intent.component = cmp
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            1 -> {
                if (!checkApkExist(context, "com.tencent.mobileqq")) {
                    ToastUtils.show("请先安装最新版QQ!")
                    return
                }
                val intent = Intent(Intent.ACTION_MAIN)
                val cmp = ComponentName(
                    "com.tencent.mobileqq",
                    "com.tencent.mobileqq.activity.SplashActivity"
                )
                intent.component = cmp
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
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