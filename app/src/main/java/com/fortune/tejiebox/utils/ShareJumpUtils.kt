package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import kotlinx.android.synthetic.main.layout_dialog_default.*

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
        message: String? = null
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_default)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.tv_dialog_default_title?.text = "特戒分享"
        mDialog?.tv_dialog_default_message?.text =
            if (message != null) {
                "分享图片已保存至本地,\n可在QQ和微信上分享给他人!"
            } else {
                "分享链接已复制到剪贴板,\n可在QQ和微信上分享给他人!"
            }
        mDialog?.tv_dialog_default_cancel?.text = "微信"
        mDialog?.tv_dialog_default_sure?.text = "QQ"
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            dismissDialog()
            share(context, 1)
        }
        mDialog?.tv_dialog_default_cancel?.setOnClickListener {
            dismissDialog()
            share(context, 0)
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