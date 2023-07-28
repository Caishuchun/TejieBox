package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
            if (isBind) {
                if (savePhone != phone) {
                    //如果不是之前输入的,直接重新发送
                    SPUtils.putValue(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
                    sendCode2(phone, null, null)
                } else {
                    val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
                    val currentTimeMillis = SystemClock.uptimeMillis()
                    if (oldTimeMillis == 0L) {
                        //历史时间没有的话,就要重新发验证码
                        sendCode2(phone, null, null)
                    } else {
                        when {
                            currentTimeMillis - oldTimeMillis > 60 * 1000 -> {
                                //当前时间超过历史时间1分钟,重新发送
                                sendCode2(phone, null, null)
                            }

                            currentTimeMillis < oldTimeMillis -> {
                                //当前时间小于历史时间,说明重新开机过,重新发送短信
                                sendCode2(phone, null, null)
                            }

                            else -> {
                                //直接跳转
                                if (tempOldPhone == null && tempOldCaptcha == null) {
                                    toNext(phone, tempOldPhone!!, tempOldCaptcha!!)
                                }
                            }
                        }
                    }
                }
            } else {
                val oldPhone = SPUtils.getString(SPArgument.PHONE_NUMBER)
                if (oldPhone == phone) {
                    ToastUtils.show("新手机号不能与旧手机号相同")
                    return
                }
                sendCode(phone)
            }
        }
    }

    /**
     * 先行弹框,再发送短信验证码
     */
    private fun sendCode(phone: String) {
        tempPhone = phone
        val intent = Intent(this, VerificationCodeActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable(
            VerificationCodeActivity.TYPE,
            VerificationCodeActivity.TITLE.CHANGE_PHONE
        )
        intent.putExtras(bundle)
        startActivityForResult(intent, 10104)
    }

    private var tempPhone: String? = null
    private var tempOldPhone: String? = null
    private var tempOldCaptcha: String? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //发送短信验证码成功后,跳转到下一步
        if (requestCode == 10104 && resultCode == Activity.RESULT_OK) {
            val code = data?.getIntExtra("code", -1)
            if (code == 4) {
                val old_phone = data.getStringExtra("old_phone")
                val old_captcha = data.getStringExtra("old_captcha")
                if (tempPhone != null) {
                    sendCode2(tempPhone!!, old_phone!!, old_captcha!!)
                    tempPhone = null
                    tempOldPhone = null
                    tempOldCaptcha = null
                }
            }
        }
    }

    /**
     * 发送短信验证码
     */
    private fun sendCode2(phone: String, old_phone: String?, old_captcha: String?) {
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
                            tempOldPhone = old_phone
                            tempOldCaptcha = old_captcha
                            toNext(phone, old_phone, old_captcha)
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
    private fun toNext(phone: String, old_phone: String?, old_captcha: String?) {
        val intent = Intent(this, ChangePhone2Activity::class.java)
        if (isBind) {
            intent.putExtra(ChangePhone2Activity.IS_BIND, true)
        }
        intent.putExtra(ChangePhone2Activity.PHONE, phone)
        intent.putExtra(ChangePhone2Activity.OLD_PHONE, old_phone)
        intent.putExtra(ChangePhone2Activity.OLD_CAPTCHA, old_captcha)
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