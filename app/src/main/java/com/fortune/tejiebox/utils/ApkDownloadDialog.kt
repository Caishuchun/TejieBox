package com.fortune.tejiebox.utils

import android.content.Context
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import kotlinx.android.synthetic.main.dialog_apk_download.*

/**
 *apk下载更新进度框
 */

object ApkDownloadDialog {
    private var mDialog: BaseDialog? = null

    /**
     * 取消加载框
     */
    fun dismissLoading() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
            mDialog = null
        }
    }

    /**
     * 是否正在显示
     */
    fun isShowing() = mDialog?.isShowing == true

    /**
     * 更新进度条
     */
    fun setProgress(progress: Int) {
        mDialog?.number_progress_bar?.setProgress(progress)
    }

    /**
     * 显示Dialog
     */
    fun showDialog(context: Context) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_apk_download)
        mDialog?.setCancelable(false)
        mDialog?.show()
    }
}
