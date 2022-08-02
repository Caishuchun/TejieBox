package com.fortune.tejiebox.base

import android.app.Dialog
import android.content.Context
import com.fortune.tejiebox.utils.LogUtils

class BaseDialog(
    context: Context, themeResId: Int
) : Dialog(context, themeResId) {
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && window != null) {
            val decorView = window?.decorView
            if (decorView?.height == 0 || decorView?.width == 0) {
                LogUtils.d("Dialog 布局异常,重新布局")
                decorView.requestLayout()
            }
        }
    }

}