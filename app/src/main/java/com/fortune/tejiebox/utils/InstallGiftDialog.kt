package com.fortune.tejiebox.utils

import android.content.Context
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import kotlinx.android.synthetic.main.dialog_install_gift.tv_install_gift_money
import kotlinx.android.synthetic.main.dialog_install_gift.tv_install_gift_sure
import kotlinx.android.synthetic.main.dialog_install_gift.tv_install_gift_tips

object InstallGiftDialog {
    private var mDialog: BaseDialog? = null

    /**
     * 下载礼包
     */
    fun showInstallGiftDialog(
        context: Context,
        listener: OnDialogListener?,
        tips: String? = "",
        money: Int? = 200,
        days: Int? = 7
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_install_gift)

        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)

        mDialog?.tv_install_gift_sure?.let {
            it.text = tips
        }

        mDialog?.tv_install_gift_money?.let {
            it.text = money.toString()
        }
        mDialog?.tv_install_gift_tips?.let {
            it.text = it.text.toString().replace("*", days.toString())
        }
        mDialog?.tv_install_gift_sure?.let {
            it.setOnClickListener {
                listener?.next()
                if (tips?.contains("领取") == true) {
                    mDialog?.dismiss()
                }
            }
        }
        mDialog?.show()
    }

    /**
     * 取消加载框
     */
    fun dismissLoading() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mDialog = null
        }
    }

    interface OnDialogListener {
        fun next()
    }
}