package com.fortune.tejiebox.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.WebActivity
import com.fortune.tejiebox.base.BaseDialog
import kotlinx.android.synthetic.main.dialog_beautiful.*
import kotlinx.android.synthetic.main.dialog_loading.*
import kotlinx.android.synthetic.main.layout_dialog_agreement.*
import kotlinx.android.synthetic.main.layout_dialog_default.*

object DialogUtils {
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
     * 显示带有loading的Dialog
     */
    fun showDialogWithLoading(context: Context, msg: String) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.BeautifulDialog)
        mDialog?.setContentView(R.layout.dialog_loading)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        mDialog?.tv_dialog_message?.text = msg
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 设置提示信息
     */
    fun setDialogMsg(msg: String) {
        if (mDialog != null && mDialog?.isShowing == true)
            mDialog?.tv_dialog_message?.text = msg
    }

    /**
     * 花里胡哨加载条
     */
    fun showBeautifulDialog(context: Context) {
        (context as Activity).window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.BeautifulDialog)
        mDialog?.setContentView(R.layout.dialog_beautiful)
        mDialog?.setCancelable(false)
        mDialog?.av_dialog?.show()
        mDialog?.setOnCancelListener {
            mDialog?.av_dialog?.hide()
        }
        mDialog?.show()
    }

    /**
     * 显示普通Dialog
     */
    fun showDefaultDialog(
        context: Context,
        title: String,
        msg: String,
        cancel: String,
        sure: String,
        listener: OnDialogListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_default)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.tv_dialog_default_title?.text = title
        mDialog?.tv_dialog_default_cancel?.text = cancel
        mDialog?.tv_dialog_default_sure?.text = sure
        mDialog?.tv_dialog_default_message?.text = msg
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            dismissLoading()
            listener.next()
        }
        mDialog?.tv_dialog_default_cancel?.setOnClickListener {
            dismissLoading()
        }
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 显示普通Dialog
     */
    fun showAgreementDialog(
        context: Context,
        listener: OnDialogListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_agreement)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)

        val ssb = SpannableStringBuilder("我已阅读并同意《用户协议》和《隐私协议》")
        ssb.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(context, WebActivity::class.java)
                intent.putExtra(WebActivity.TYPE, WebActivity.USER_AGREEMENT)
                context.startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#5F60FF")
                ds.isUnderlineText = false
            }

        }, 7, 13, 0)
        ssb.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(context, WebActivity::class.java)
                intent.putExtra(WebActivity.TYPE, WebActivity.PRIVACY_AGREEMENT)
                context.startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#5F60FF")
                ds.isUnderlineText = false
            }
        }, 14, ssb.length, 0)

        mDialog?.tv_dialog_agreement_content?.let {
            it.movementMethod = LinkMovementMethod.getInstance()
            it.setText(ssb, TextView.BufferType.SPANNABLE)
            it.highlightColor = Color.TRANSPARENT
        }

        mDialog?.tv_dialog_agreement_next?.setOnClickListener {
            dismissLoading()
            listener.next()
        }
        mDialog?.iv_dialog_agreement_cancel?.setOnClickListener {
            dismissLoading()
        }
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    interface OnDialogListener {
        fun next()
    }
}