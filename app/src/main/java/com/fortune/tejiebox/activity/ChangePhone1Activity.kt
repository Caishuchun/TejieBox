package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.SystemClock
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_change_phone1.*
import java.util.concurrent.TimeUnit

class ChangePhone1Activity : BaseActivity() {
    private var sendCodeObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ChangePhone1Activity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val IS_BIND = "isBind"
    }

    private var isBind = false
    private var savePhone = ""

    override fun getLayoutId() = R.layout.activity_change_phone1

    @SuppressLint("SetTextI18n", "CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        isBind = intent.getBooleanExtra(IS_BIND, false)

        if (isBind) {
            tv_changePhone1_title.text = "绑定手机号"
            tv_changePhone1_tips.text = "与注册账号进行绑定,下次可使用该手机号进行登录"
            ll_changePhone1_currentPhone.visibility = View.INVISIBLE
            et_changePhone1_phone.hint = "输入需要绑定的手机号"
        } else {
            val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)!!
            tv_changePhone1_currentPhone.text =
                "+86 ${phone.substring(0, 3)}****${phone.substring(7)}"
        }

        RxView.clicks(iv_changePhone1_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        RxView.clicks(tv_changePhone1_next)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toCheckPhone()
            }
    }

    /**
     * 检查手机号
     */
    private fun toCheckPhone() {
        val phone = et_changePhone1_phone.text.toString().trim()
        if (phone.isEmpty() || !OtherUtils.isPhone(phone)) {
            ToastUtils.show(getString(R.string.please_enter_right_phone))
        } else {
            if (savePhone != phone) {
                //如果不是之前输入的,直接重新发送
                SPUtils.putValue(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
                sendCode(phone)
            } else {
                val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
                val currentTimeMillis = SystemClock.uptimeMillis()
                if (oldTimeMillis == 0L) {
                    //历史时间没有的话,就要重新发验证码
                    sendCode(phone)
                } else {
                    when {
                        currentTimeMillis - oldTimeMillis > 60 * 1000 -> {
                            //当前时间超过历史时间1分钟,重新发送
                            sendCode(phone)
                        }
                        currentTimeMillis < oldTimeMillis -> {
                            //当前时间小于历史时间,说明重新开机过,重新发送短信
                            sendCode(phone)
                        }
                        else -> {
                            //直接跳转
                            toNext(phone)
                        }
                    }
                }
            }
        }
    }

    /**
     * 发送短信验证码
     */
    private fun sendCode(phone: String) {
        DialogUtils.showBeautifulDialog(this)
        val sendCode4changePhone =
            if (isBind) RetrofitUtils.builder().sendCode(phone, 1)
            else RetrofitUtils.builder().sendCode4changePhone(phone)
        sendCodeObservable = sendCode4changePhone
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            savePhone = phone
                            SPUtils.putValue(
                                SPArgument.CODE_TIME_4_CHANGE_PHONE,
                                SystemClock.uptimeMillis()
                            )
                            toNext(phone)
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 跳转到下一个界面
     */
    private fun toNext(phone: String) {
        val intent = Intent(this, ChangePhone2Activity::class.java)
        if (isBind) {
            intent.putExtra(ChangePhone2Activity.IS_BIND, true)
        }
        intent.putExtra(ChangePhone2Activity.PHONE, phone)
        startActivity(intent)
    }

    override fun destroy() {
        sendCodeObservable?.dispose()
        sendCodeObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}