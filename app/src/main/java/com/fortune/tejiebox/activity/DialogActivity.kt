package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Html
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.bloom.ExplosionField
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_dialog.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class DialogActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: DialogActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        private var type = TYPE.GET_INTEGRAL
        private var integral = 0
        private var canCancel = false
        private var callback: OnCallback? = null

        /**
         * 展示领取积分界面
         */
        fun showGetIntegral(
            context: Activity,
            integral: Int,
            canCancel: Boolean,
            callback: OnCallback?
        ) {
            context.startActivity(Intent(context, DialogActivity::class.java))
            this.type = TYPE.GET_INTEGRAL
            this.integral = integral
            this.canCancel = canCancel
            this.callback = callback
        }

        private var success = false
        private var msg = ""

        /**
         * 展示积分兑换结果
         */
        fun showRechargeResult(
            context: Activity,
            success: Boolean,
            msg: String
        ) {
            context.startActivity(Intent(context, DialogActivity::class.java))
            this.type = TYPE.RECHARGE_RESULT
            this.success = success
            this.msg = msg
            this.canCancel = true
            this.callback = null
        }

        private var giftNum = "888888"

        /**
         * 展示免费礼包
         */
        fun showGiftCode(
            context: Activity,
            giftNum: String
        ) {
            context.startActivity(Intent(context, DialogActivity::class.java))
            this.type = TYPE.GIFT_CODE
            this.giftNum = giftNum
            this.canCancel = true
            this.callback = null
        }
    }

    enum class TYPE {
        GET_INTEGRAL,
        RECHARGE_RESULT,
        GIFT_CODE
    }

    private var time = 0L
    private var timer: Disposable? = null
    private var getGiftCodeObservable: Disposable? = null
    private var isLogin = false

    override fun getLayoutId() = R.layout.activity_dialog

    @SuppressLint("CheckResult")
    override fun doSomething() {
        EventBus.getDefault().register(this)
        if (canCancel) {
            RxView.clicks(rl_dialog_root)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe { finish() }
        }

        when (type) {
            TYPE.GET_INTEGRAL -> {
                showGetIntegral()
            }
            TYPE.RECHARGE_RESULT -> {
                showRechargeResult()
            }
            TYPE.GIFT_CODE -> {
                showGiftCode()
            }
        }
    }

    /**
     * 展示免费礼包
     */
    @SuppressLint("CheckResult", "SetTextI18n")
    private fun showGiftCode() {
        ll_dialog_giftCode.visibility = View.VISIBLE
        ll_dialog_rechargeResult.visibility = View.GONE
        ll_dialog_getIntegral.visibility = View.GONE

        RxView.clicks(ll_dialog_giftCode)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { }

        RxView.clicks(iv_dialog_giftCode_cancel)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        if (!MyApp.getInstance().isHaveToken()) {
            //没有登录
            tv_dialog_giftCode_login.visibility = View.VISIBLE
            ll_dialog_giftCode_show.visibility = View.GONE

            tv_dialog_giftCode_login.text = Html.fromHtml("<u>未登录,请先点击登录</u>")
            RxView.clicks(tv_dialog_giftCode_login)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    isLogin = true
                    LoginUtils.toQuickLogin(this)
                }

        } else {
            toGetGiftCode()
        }
    }

    /**
     * 请求获取游戏礼包码
     */
    @SuppressLint("SetTextI18n")
    private fun toGetGiftCode() {
        val getGiftCode = RetrofitUtils.builder().getGiftCode(giftNum)
        getGiftCodeObservable = getGiftCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            ll_dialog_giftCode_show.visibility = View.VISIBLE
                            tv_dialog_giftCode_login.visibility = View.GONE
                            tv_dialog_giftCode_code.text = it.data.code
                            time = it.data.ttl

                            timer?.dispose()
                            timer = Observable.interval(0L, 1L, TimeUnit.SECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    tv_dialog_giftCode_time.text = "${timeFormat()} 有效"
                                    if (time <= 0L) {
                                        //重新请求接口获取数据
                                        timer?.dispose()
                                        toGetGiftCode()
                                    }
                                }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                            finish()
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                    finish()
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                finish()
            })
    }

    /**
     * 时间格式化
     */
    private fun timeFormat(): String {
        time--
        return if (time < 60) {
            "00:${timeFormat2bit(time)}"
        } else {
            val minutes = time / 60L
            val second = time % 60L
            "${timeFormat2bit(minutes)}:${timeFormat2bit(second)}"
        }
    }

    /**
     * 时间格式化为两位
     */
    private fun timeFormat2bit(time: Long): String {
        return if (time < 10) {
            "0$time"
        } else {
            time.toString()
        }
    }

    /**
     * 展示积分兑换结果
     */
    @SuppressLint("CheckResult")
    private fun showRechargeResult() {
        ll_dialog_rechargeResult.visibility = View.VISIBLE
        ll_dialog_getIntegral.visibility = View.GONE
        ll_dialog_giftCode.visibility = View.GONE
        when (success) {
            true -> {
                iv_dialog_recharge.setImageResource(R.mipmap.recharge_success)
                tv_dialog_recharge_title.text = "充值成功"
                tv_dialog_recharge_msg.text = msg
                tv_dialog_recharge_sure.setBackgroundResource(R.drawable.bg_recharge_success)
            }
            false -> {
                iv_dialog_recharge.setImageResource(R.mipmap.recharge_fail)
                tv_dialog_recharge_title.text = "充值失败"
                tv_dialog_recharge_msg.text = msg
                tv_dialog_recharge_sure.setBackgroundResource(R.drawable.bg_recharge_fail)
            }
        }
        RxView.clicks(tv_dialog_recharge_sure)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        RxView.clicks(ll_dialog_rechargeResult)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { }
    }

    /**
     * 展示领取积分
     */
    @SuppressLint("SetTextI18n", "CheckResult")
    private fun showGetIntegral() {
        ll_dialog_getIntegral.visibility = View.VISIBLE
        ll_dialog_rechargeResult.visibility = View.GONE
        ll_dialog_giftCode.visibility = View.GONE

        RxView.clicks(ll_dialog_getIntegral)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { }

        tv_integral_integral.text =
            if (BaseAppUpdateSetting.isToAuditVersion) "积分 +$integral"
            else "余额 +${integral / 10}元"
        iv_dialog_color.postDelayed({
            val explosionField = ExplosionField.attach2Window(this)
            explosionField.explode(iv_dialog_color)
        }, 100)

        RxView.clicks(tv_integral_sure)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun whenLogin(loginStatusChange: LoginStatusChange) {
        if (loginStatusChange == null) {
            return
        }
        if (loginStatusChange.isLogin && isLogin) {
            isLogin = false
            toGetGiftCode()
        }
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)
        if (type == TYPE.GET_INTEGRAL) {
            callback?.cancel()
        }
        timer?.dispose()
        timer = null

        getGiftCodeObservable?.dispose()
        getGiftCodeObservable = null
    }

    interface OnCallback {
        fun cancel()
    }
}