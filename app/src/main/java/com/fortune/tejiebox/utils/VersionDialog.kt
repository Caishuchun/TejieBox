package com.fortune.tejiebox.utils

import android.content.Context
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import kotlinx.android.synthetic.main.layout_dialog_version.*

/**
 * 版本更新Dialog
 */

object VersionDialog {

    private var mDialog: BaseDialog? = null

    fun show(context: Context, msg: String, listener: OnUpdateAPP?) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_version)
        mDialog?.tv_dialog_version_cancel?.visibility = View.GONE
        mDialog?.view_dialog_version_line?.visibility = View.GONE
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.iv_dialog_version_back?.setOnClickListener {
            mDialog!!.dismiss()
        }
        mDialog?.tv_dialog_version_message?.text = msg
        mDialog?.tv_dialog_version_cancel?.setOnClickListener {
            mDialog!!.dismiss()
        }
        mDialog?.tv_dialog_version_sure?.setOnClickListener {
            mDialog?.dismiss()
            listener?.onUpdate()
        }
        mDialog?.setOnCancelListener {
            mDialog?.dismiss()
            listener?.onUpdate()
        }
        mDialog?.show()
    }

    fun dismiss() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
            mDialog = null
        }
    }

    interface OnUpdateAPP {
        fun onUpdate()
    }
}
