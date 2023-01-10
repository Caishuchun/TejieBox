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
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.WebActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.base.BaseDialog
import com.fortune.tejiebox.http.RetrofitUtils
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_beautiful.*
import kotlinx.android.synthetic.main.dialog_loading.*
import kotlinx.android.synthetic.main.layout_dialog_agreement.*
import kotlinx.android.synthetic.main.layout_dialog_default.*
import kotlinx.android.synthetic.main.layout_dialog_fireworks.*
import kotlinx.android.synthetic.main.layout_dialog_invite_tips.*
import kotlinx.android.synthetic.main.layout_dialog_star.*
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

        } finally {
            getStarObservable?.dispose()
            getStarObservable = null
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
     * 显示协议Dialog
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

        if (BaseAppUpdateSetting.isToAuditVersion) {
            mDialog?.tv_dialog_agreement_cancel?.visibility = View.VISIBLE
            mDialog?.tv_dialog_agreement_cancel?.let {
                RxView.clicks(it)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        exitProcess(0)
                    }
            }
        } else {
            mDialog?.tv_dialog_agreement_cancel?.visibility = View.GONE
        }

        val ssb =
            SpannableStringBuilder("欢迎使用特戒!\n我们非常重视您的个人信息和隐私协议保护。为了更好地保障您的个人权益, 在使用我们的服务前, 请务必打开链接并审慎阅读《用户协议》和《隐私协议》")
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

        }, 66, 72, 0)
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
        }, 73, ssb.length, 0)

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

    /**
     * 显示新年活动_邀请公示Dialog
     * @param num 奖励值
     * @param target 目标
     */
    @SuppressLint("SetTextI18n")
    fun showInviteTipsDialog(
        context: Context,
        num: Int,
        target: String,
        listener: OnDialogListener?
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_invite_tips)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)

        mDialog?.tv_dialog_newYear_invite_num?.let {
            it.text = "+${num / 10}元"
        }

        mDialog?.tv_dialog_newYear_invite_target?.let {
            it.text = "\"$target\""
        }

        mDialog?.tv_dialog_newYear_invite_sure?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismissLoading()
                }
        }

        mDialog?.iv_dialog_newYear_invite_cancel?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismissLoading()
                }
        }

        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    /**
     * 显示新年活动_展示礼花
     */
    @SuppressLint("SetTextI18n")
    fun showFireworksDialog(
        context: Context,
        index: Int,
        listener: OnDialogListener?
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_fireworks)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)
        val res = when (index) {
            1 -> {
                R.drawable.fireworks_1
            }
            2 -> {
                R.drawable.fireworks_2
            }
            3 -> {
                R.drawable.fireworks_3
            }
            else -> {
                R.drawable.fireworks_1
            }
        }

        val options = RequestOptions()
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        mDialog?.iv_dialog_fireworks?.let {
            Glide.with(context)
                .asGif()
                .load(res)
                .apply(options)
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        it.postDelayed(
                            Runnable {
                                dismissLoading()
                                listener?.next()
                            }, when (index) {
                                1 -> {
                                    1200
                                }
                                2 -> {
                                    1500
                                }
                                3 -> {
                                    2000
                                }
                                else -> {
                                    1500
                                }
                            }
                        )
                        return false
                    }
                })
                .into(it)
        }
        mDialog?.show()
    }

    /**
     * 显示新年活动_获取能量
     */
    @SuppressLint("SetTextI18n")
    fun showGetStarDialog(
        context: Activity,
        listener: OnDialogListener4Star?
    ) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.layout_dialog_star)
        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)

        val nianShouStarInfo = RetrofitUtils.builder().nianShouStarInfo()
        getStarObservable = nianShouStarInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            val data = it.data
                            mDialog?.tv_dialog_star_title1?.text = data[0].task_title
                            mDialog?.tv_dialog_star_title2?.text = data[1].task_title
                            mDialog?.tv_dialog_star_title3?.text = data[2].task_title

                            mDialog?.tv_dialog_star_msg2?.text = data[1].task_dis
                            mDialog?.tv_dialog_star_msg3?.text = data[2].task_dis

                            mDialog?.tv_dialog_star_star1?.text =
                                "${data[0].task_speed}/${data[0].task_total}"
                            mDialog?.tv_dialog_star_star2?.text = "${data[1].task_speed}"
                            mDialog?.tv_dialog_star_star3?.text =
                                "${data[2].task_speed}/${data[2].task_total}"
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(context)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                }, {
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(context, it))
                })

        mDialog?.iv_dialog_star_btn1?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismissLoading()
                    listener?.click(0)
                }
        }
        mDialog?.iv_dialog_star_btn2?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismissLoading()
                    listener?.click(1)
                }
        }
        mDialog?.iv_dialog_star_btn3?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismissLoading()
                    listener?.click(2)
                }
        }

        mDialog?.iv_dialog_star_cancel?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    dismissLoading()
                }
        }

        mDialog?.show()
    }

    interface OnDialogListener {
        fun next()
    }

    interface OnDialogListener4Star {
        fun click(index: Int)
    }
}