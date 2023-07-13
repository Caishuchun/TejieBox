package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.WebActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseDialog
import com.fortune.tejiebox.bean.AllAccountBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.snail.antifake.jni.EmulatorDetectUtil
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.dialog_beautiful.av_dialog
import kotlinx.android.synthetic.main.dialog_loading.tv_dialog_message
import kotlinx.android.synthetic.main.item_popup_account.view.tv_item_popup_account
import kotlinx.android.synthetic.main.layout_dialog_agreement.iv_dialog_agreement_cancel
import kotlinx.android.synthetic.main.layout_dialog_agreement.tv_dialog_agreement_cancel
import kotlinx.android.synthetic.main.layout_dialog_agreement.tv_dialog_agreement_content
import kotlinx.android.synthetic.main.layout_dialog_agreement.tv_dialog_agreement_next
import kotlinx.android.synthetic.main.layout_dialog_default.tv_dialog_default_cancel
import kotlinx.android.synthetic.main.layout_dialog_default.tv_dialog_default_message
import kotlinx.android.synthetic.main.layout_dialog_default.tv_dialog_default_sure
import kotlinx.android.synthetic.main.layout_dialog_default.tv_dialog_default_title
import kotlinx.android.synthetic.main.layout_dialog_default.view_dialog_default_line
import kotlinx.android.synthetic.main.layout_dialog_sms_code.et_dialog_smsCode_code
import kotlinx.android.synthetic.main.layout_dialog_sms_code.tv_dialog_smsCode_cancel
import kotlinx.android.synthetic.main.layout_dialog_sms_code.tv_dialog_smsCode_phone
import kotlinx.android.synthetic.main.layout_dialog_sms_code.tv_dialog_smsCode_sure
import kotlinx.android.synthetic.main.layout_dialog_start_game.et_dialog_startGame_account
import kotlinx.android.synthetic.main.layout_dialog_start_game.et_dialog_startGame_password
import kotlinx.android.synthetic.main.layout_dialog_start_game.iv_dialog_startGame_cancel
import kotlinx.android.synthetic.main.layout_dialog_start_game.iv_dialog_startGame_more
import kotlinx.android.synthetic.main.layout_dialog_start_game.ll_dialog_startGame_account
import kotlinx.android.synthetic.main.layout_dialog_start_game.ll_dialog_startGame_account_view
import kotlinx.android.synthetic.main.layout_dialog_start_game.tv_dialog_startGame_btn1
import kotlinx.android.synthetic.main.layout_dialog_start_game.tv_dialog_startGame_btn2
import kotlinx.android.synthetic.main.layout_dialog_start_game.tv_dialog_startGame_gameName
import kotlinx.android.synthetic.main.layout_dialog_start_game.tv_dialog_startGame_phone
import kotlinx.android.synthetic.main.layout_dialog_start_game.tv_dialog_startGame_tips
import kotlinx.android.synthetic.main.popupwindow_account.view.rv_popupWindow_account
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


object DialogUtils {
    private var mDialog: BaseDialog? = null
    private var getStarObservable: Disposable? = null

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
            getStarObservable?.dispose()
            getStarObservable = null
            mDialog = null
            mPopupWindow?.dismiss()
            mPopupWindow = null
            isShowPopupWindow = false
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
        if (mDialog != null && mDialog?.isShowing == true) mDialog?.tv_dialog_message?.text = msg
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
     * 显示短信验证码输入弹框
     */
    @SuppressLint("SetTextI18n")
    fun showSmsCodeDialog(context: Context, listener: OnDialogListener4ShowSmsCode) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_sms_code)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        SPUtils.getString(SPArgument.PHONE_NUMBER, "")?.let {
            mDialog?.tv_dialog_smsCode_phone?.text =
                "验证码已发送至${it.replaceRange(3, 7, "****")}"
        }
        mDialog?.tv_dialog_smsCode_cancel?.setOnClickListener {
            mDialog?.dismiss()
        }
        mDialog?.tv_dialog_smsCode_sure?.setOnClickListener {
            val code = mDialog?.et_dialog_smsCode_code?.text.toString().trim()
            if (code.isEmpty() || code.length != 6) {
                ToastUtils.show("请输入短信验证码")
                return@setOnClickListener
            }
            listener.onSure(code)
            mDialog?.dismiss()
        }
        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 显示仅有确认按钮的弹框
     */
    fun showOnlySureDialog(
        context: Context,
        title: String,
        msg: String,
        sure: String,
        isBackLastPage: Boolean,
        listener: OnDialogListener?
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_default)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        mDialog?.tv_dialog_default_title?.text = title
        mDialog?.tv_dialog_default_cancel?.visibility = View.GONE
        mDialog?.view_dialog_default_line?.visibility = View.GONE
        mDialog?.tv_dialog_default_sure?.text = sure
        mDialog?.tv_dialog_default_message?.text = msg
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            listener?.next()
            mDialog?.dismiss()
            if (isBackLastPage) {
                (context as Activity).finish()
            }
        }
        mDialog?.setOnCancelListener {
            listener?.next()
            mDialog?.dismiss()
            if (isBackLastPage) {
                (context as Activity).finish()
            }
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
        cancel: String?,
        sure: String,
        listener: OnDialogListener?
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
        if (cancel == null) {
            mDialog?.tv_dialog_default_cancel?.visibility = View.GONE
            mDialog?.view_dialog_default_line?.visibility = View.GONE
        } else {
            mDialog?.tv_dialog_default_cancel?.text = cancel
        }
        mDialog?.tv_dialog_default_sure?.text = sure
        mDialog?.tv_dialog_default_message?.text = msg
        if (msg.length > 24) {
            mDialog?.tv_dialog_default_message?.gravity = Gravity.START
        }
        mDialog?.tv_dialog_default_sure?.setOnClickListener {
            dismissLoading()
            listener?.next()
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
     * 显示协议Dialog
     * @param needExit 需要退出APP吗
     */
    fun showAgreementDialog(
        context: Context, needExit: Boolean = true, listener: OnDialogListener
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_agreement)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)

        mDialog?.tv_dialog_agreement_cancel?.visibility = View.VISIBLE
        mDialog?.tv_dialog_agreement_cancel?.text = if (needExit) "拒绝并退出应用" else "暂不同意"
        mDialog?.tv_dialog_agreement_cancel?.let {
            RxView.clicks(it).throttleFirst(200, TimeUnit.MILLISECONDS).subscribe {
                if (needExit) {
                    exitProcess(0)
                } else {
                    dismissLoading()
                }
            }
        }

        val ssb =
            SpannableStringBuilder("欢迎使用特戒盒子!\n我们非常重视您的个人信息和隐私协议保护。为了更好地保障您的个人权益, 在使用我们的服务前, 请务必打开链接并审慎阅读《用户协议》和《隐私协议》")
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

        }, 68, 74, 0)
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
        }, 75, ssb.length, 0)

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


    private var currentPage = 0
    private var mPopupWindow: PopupWindow? = null
    private var isShowPopupWindow = false

    /**
     * 显示进入未上架游戏Dialog
     */
    @SuppressLint("SetTextI18n", "CheckResult")
    fun showStartGameDialog(
        context: Activity,
        gameName: String,
        data: List<AllAccountBean.Data>?,
        listener: OnDialogListener4StartGame?,
        page: Int = 0
    ) {
        currentPage = page
        mPopupWindow?.dismiss()
        mPopupWindow = null
        isShowPopupWindow = false
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_start_game)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        val emulator = EmulatorDetectUtil.isEmulator(context)
        mDialog?.tv_dialog_startGame_tips?.let {
            it.visibility = if (emulator) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        mDialog?.tv_dialog_startGame_gameName?.text = gameName
        if (data == null || data.isEmpty()) {
            mDialog?.iv_dialog_startGame_more?.visibility = View.GONE
        } else {
            mDialog?.iv_dialog_startGame_more?.visibility = View.VISIBLE
            mDialog?.et_dialog_startGame_account?.let {
                it.setText(data[0].account)
                it.setSelection(data[0].account.length)
            }
            mDialog?.et_dialog_startGame_password?.let {
                it.setText(data[0].password)
                it.setSelection(data[0].password.length)
            }

            val popupWindow =
                LayoutInflater.from(context).inflate(R.layout.popupwindow_account, null)
            val width = PhoneInfoUtils.getWidth(context)
            mPopupWindow = PopupWindow(
                popupWindow,
                (248f / 360 * width).toInt(),
                (36f / 360 * width).toInt() * Math.min(data.size, 6)
            )
            mPopupWindow?.setOnDismissListener {
                isShowPopupWindow = false
            }
            mPopupWindow?.isOutsideTouchable = true
            mPopupWindow?.isFocusable = true

            val adapter = BaseAdapterWithPosition.Builder<AllAccountBean.Data>()
                .setLayoutId(R.layout.item_popup_account).setData(data)
                .addBindView { itemView, itemData, position ->
                    itemView.tv_item_popup_account.text = itemData.account
                    RxView.clicks(itemView).throttleFirst(200, TimeUnit.MILLISECONDS).subscribe {
                        mDialog?.et_dialog_startGame_account?.let {
                            it.setText(itemData.account)
                            it.setSelection(itemData.account.length)
                        }
                        mDialog?.et_dialog_startGame_password?.let {
                            it.setText(itemData.password)
                            it.setSelection(itemData.password.length)
                        }
                        mPopupWindow?.dismiss()
                    }
                }.create()
            popupWindow.rv_popupWindow_account.adapter = adapter
            popupWindow.rv_popupWindow_account.layoutManager = SafeLinearLayoutManager(context)
            adapter.notifyDataSetChanged()
        }

        changeView(context)
        mDialog?.tv_dialog_startGame_btn1?.let {
            RxView.clicks(it).throttleFirst(200, TimeUnit.MILLISECONDS).subscribe {
                currentPage = if (currentPage == 0) {
                    1
                } else {
                    0
                }
                changeView(context)
            }
        }
        mDialog?.tv_dialog_startGame_btn2?.let {
            RxView.clicks(it).throttleFirst(200, TimeUnit.MILLISECONDS).subscribe {
                if (currentPage == 0) {
                    //特戒账号登录
                    listener?.tejieStart()
                } else {
                    //账号密码登录
                    val account = mDialog?.et_dialog_startGame_account?.text?.toString()?.trim()
                    val password =
                        mDialog?.et_dialog_startGame_password?.text?.toString()?.trim()
                    if (account != null && account != "" && password != null && password != "") {
                        listener?.accountStart(account, password)
                    } else {
                        ToastUtils.show("请检查账号密码是否输入正确")
                    }
                }
            }
        }

        mDialog?.iv_dialog_startGame_cancel?.setOnClickListener {
            currentPage = 0
            mPopupWindow?.dismiss()
            mPopupWindow = null
            isShowPopupWindow = false
            dismissLoading()
        }
        mDialog?.setOnCancelListener {
            currentPage = 0
            mPopupWindow?.dismiss()
            mPopupWindow = null
            isShowPopupWindow = false
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 改UI
     */
    private fun changeView(context: Activity) {
        if (currentPage == 0) {
            mDialog?.tv_dialog_startGame_phone?.let {
                it.visibility = View.VISIBLE
                it.text = formatPhone()
            }

            mDialog?.ll_dialog_startGame_account?.let {
                it.visibility = View.GONE
            }

            mDialog?.tv_dialog_startGame_btn1?.let {
                it.text = "已有账号登录"
                it.setBackgroundResource(R.drawable.bg_green_btn)
            }
            mDialog?.tv_dialog_startGame_btn2?.let {
                it.text = "盒子账号登录"
                it.setBackgroundResource(R.drawable.bg_start_btn)
            }
        } else {
            mDialog?.tv_dialog_startGame_phone?.let {
                it.visibility = View.GONE
            }

            mDialog?.ll_dialog_startGame_account?.let {
                it.visibility = View.VISIBLE
            }

            mDialog?.et_dialog_startGame_account?.let {
                RxTextView.textChanges(it).skipInitialValue().subscribe {
                    mDialog?.et_dialog_startGame_password?.setText("")
                }
            }

            mDialog?.iv_dialog_startGame_more?.let {
                RxView.clicks(it).throttleFirst(200, TimeUnit.MILLISECONDS).subscribe {
                    OtherUtils.hindKeyboard(context, mDialog?.et_dialog_startGame_account!!)
                    if (isShowPopupWindow) {
                        mPopupWindow?.dismiss()
                    } else {
                        mPopupWindow?.showAsDropDown(mDialog?.ll_dialog_startGame_account_view!!)
                    }
                    isShowPopupWindow = !isShowPopupWindow
                }
            }

            mDialog?.tv_dialog_startGame_btn1?.let {
                it.text = "盒子账号登录"
                it.setBackgroundResource(R.drawable.bg_start_btn)
            }
            mDialog?.tv_dialog_startGame_btn2?.let {
                it.text = "登录"
                it.setBackgroundResource(R.drawable.bg_green_btn)
            }
        }
    }

    /**
     * 格式化手机号
     */
    private fun formatPhone(): String {
        val phone = SPUtils.getString(SPArgument.PHONE_NUMBER, null)!!
        return "${phone.substring(0, 3)}****${phone.substring(7)}"
    }

    interface OnDialogListener4ShowSmsCode {
        fun onSure(code: String)
    }

    /**
     * 全部游戏登录回调
     */
    interface OnDialogListener4StartGame {
        fun tejieStart()
        fun accountStart(account: String, password: String)
    }

    interface OnDialogListener {
        fun next()
    }
}