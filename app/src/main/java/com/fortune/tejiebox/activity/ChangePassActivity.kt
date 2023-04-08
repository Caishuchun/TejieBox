package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_change_pass.*
import java.util.concurrent.TimeUnit

class ChangePassActivity : BaseActivity() {

    private var currentPage = 1
    private var signPassIsShow = false
    private var reSignPassIsShow = false
    private var oldPassIsShow = false
    private var isSend = false
    private var time = 60

    private var changePassUseOldPassObservable: Disposable? = null
    private var changePassSendCodeObservable: Disposable? = null
    private var changePassUseCodeObservable: Disposable? = null
    private var timeObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ChangePassActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_change_pass

    override fun onBackPressed() {
        toBack()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)
        ll_changePass_select.visibility = View.VISIBLE
        ll_changePass_root.visibility = View.GONE
        val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)!!

        RxView.clicks(iv_changePass_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toBack()
            }

        ll_changePass_root.visibility = View.VISIBLE
        ll_changePass_select.visibility = View.GONE
//        currentPage = 2
        ll_changePass_phonePart.visibility = View.VISIBLE
        tv_changePass_phone.text =
            "+86 ${phone.substring(0, 3)}****${phone.substring(7)}"
        ll_changePass_passPart.visibility = View.GONE

        RxView.clicks(ll_changePass_usePhone)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (phone == null) {
                    ToastUtils.show("请先绑定手机号")
                    return@subscribe
                }
                ll_changePass_root.visibility = View.VISIBLE
                ll_changePass_select.visibility = View.GONE
                currentPage = 2
                ll_changePass_phonePart.visibility = View.VISIBLE
                tv_changePass_phone.text =
                    "+86 ${phone.substring(0, 3)}****${phone.substring(7)}"
                ll_changePass_passPart.visibility = View.GONE
            }
        RxView.clicks(ll_changePass_usePass)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                ll_changePass_root.visibility = View.VISIBLE
                ll_changePass_select.visibility = View.GONE
                currentPage = 3
                ll_changePass_phonePart.visibility = View.GONE
                ll_changePass_passPart.visibility = View.VISIBLE
            }

        et_changePass_pass.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    when {
                        it.length in 8..16 -> {
                            tv_changePass_pass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 密码可用"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                        it.length > 16 -> {
                            tv_changePass_pass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 密码不得超过16位字符"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                        else -> {
                            tv_changePass_pass_tips.visibility = View.INVISIBLE
                        }
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        when {
                            et.text.length < 8 -> {
                                tv_changePass_pass_tips.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 密码不得少于8位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            et.text.length > 16 -> {
                                tv_changePass_pass_tips.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 密码不得超过16位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            else -> {
                                tv_changePass_pass_tips.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "√ 密码可用"
                                    tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                                }
                            }
                        }
                    }
                }
            }
        }

        et_changePass_rePass.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    when {
                        it.length < 8 -> {
                            tv_changePass_rePass_tips.visibility = View.INVISIBLE
                        }
                        it.toString() == et_changePass_pass.text.toString() -> {
                            tv_changePass_rePass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 两次输入密码一致"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                        it.toString() != et_changePass_pass.text.toString() -> {
                            tv_changePass_rePass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 两次输入的密码不一致"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                        it.length > 16 -> {
                            tv_changePass_rePass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 密码不得超过16位字符"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        tv_changePass_rePass_tips.let { tv ->
                            if (et.text.toString() != et_changePass_pass.text.toString()) {
                                tv.visibility = View.VISIBLE
                                tv.text = "* 两次输入的密码不一致"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            } else {
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 两次输入密码一致"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                    }
                }
            }
        }

        et_changePass_oldPass.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    when {
                        it.length in 8..16 -> {
                            tv_changePass_oldPass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 密码可用"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                        it.length > 16 -> {
                            tv_changePass_oldPass_tips.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 密码不得超过16位字符"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                        else -> {
                            tv_changePass_oldPass_tips.visibility = View.INVISIBLE
                        }
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        when {
                            et.text.length < 8 -> {
                                tv_changePass_oldPass_tips.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 密码不得少于8位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            et.text.length > 16 -> {
                                tv_changePass_oldPass_tips.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 密码不得超过16位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            else -> {
                                tv_changePass_oldPass_tips.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "√ 密码可用"
                                    tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                                }
                            }
                        }
                    }
                }
            }
        }

        iv_changePass_pass.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    signPassIsShow = !signPassIsShow
                    et_changePass_pass.let {
                        it.transformationMethod = if (signPassIsShow) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        it.setSelection(it.length())
                    }
                    iv.setImageResource(if (!signPassIsShow) R.mipmap.pass_show else R.mipmap.pass_unshow)
                }
        }

        iv_changePass_rePass.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    reSignPassIsShow = !reSignPassIsShow
                    et_changePass_rePass.let {
                        it.transformationMethod = if (reSignPassIsShow) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        it.setSelection(it.length())
                    }
                    iv.setImageResource(if (!reSignPassIsShow) R.mipmap.pass_show else R.mipmap.pass_unshow)
                }
        }

        iv_changePass_oldPass.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    oldPassIsShow = !oldPassIsShow
                    et_changePass_oldPass.let {
                        it.transformationMethod = if (oldPassIsShow) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        it.setSelection(it.length())
                    }
                    iv.setImageResource(if (!oldPassIsShow) R.mipmap.pass_show else R.mipmap.pass_unshow)
                }
        }

        RxView.clicks(tv_changePass_sendCode)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //发送短信验证码
                toSendCode()
            }

        RxView.clicks(tv_changePass_sure)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
//                if (currentPage == 2) {
                    //验证码校验
                    toCheckUsePhoneCode()
//                } else if (currentPage == 3) {
//                    //旧密码校验
//                    toCheckUseOldPass()
//                }
            }
    }

    /**
     * 返回跳转
     */
    private fun toBack() {
        if (currentPage == 1) {
            finish()
        } else {
            currentPage = 1
            ll_changePass_select.visibility = View.VISIBLE
            ll_changePass_root.visibility = View.GONE
            et_changePass_code.setText("")
            et_changePass_oldPass.setText("")
            et_changePass_pass.setText("")
            et_changePass_rePass.setText("")
        }
    }

    /**
     * 修改密码_发送短信验证码
     */
    @SuppressLint("CheckResult", "SetTextI18n")
    private fun toSendCode() {
        DialogUtils.showBeautifulDialog(this)
        val changePassSendCode = RetrofitUtils.builder().changePassSendCode()
        changePassSendCodeObservable = changePassSendCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                when (it.code) {
                    1 -> {
                        ToastUtils.show("发送短信验证码成功")
                        isSend = true
                        time = 60
                        timeObservable?.dispose()
                        timeObservable = null
                        timeObservable = Observable.interval(0, 1, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                if (time > 0) {
                                    time--
                                    tv_changePass_sendCode.text = "${time}s后重发"
//                                    tv_changePass_sendCode.setBackgroundResource(R.drawable.bg_gray_10)
                                    tv_changePass_sendCode.isEnabled = false
                                } else {
                                    tv_changePass_sendCode.text = "重发"
//                                    tv_changePass_sendCode.setBackgroundResource(R.drawable.bg_start_btn_big)
                                    tv_changePass_sendCode.isEnabled = true
                                    time = 60
                                    timeObservable?.dispose()
                                    timeObservable = null
                                }
                            }
                    }
                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(this)
                    }
                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 校验手机验证码是否发送正常
     */
    private fun toCheckUsePhoneCode() {
        val isPhoneCodeOk = et_changePass_code.text.isNotEmpty() && isSend
        if (isPhoneCodeOk) {
            val isNewPassOk =
                tv_changePass_pass_tips.text.startsWith("√") && tv_changePass_pass_tips.visibility == View.VISIBLE
            val isRePassOk =
                tv_changePass_rePass_tips.text.startsWith("√") && tv_changePass_rePass_tips.visibility == View.VISIBLE
            if (isNewPassOk && isRePassOk) {
                toChangePassUseCode(
                    et_changePass_pass.text.toString().trim(),
                    et_changePass_code.text.toString().trim()
                )
            } else {
                ToastUtils.show("请检查账号密码是否合规后再进行!")
            }
        } else {
            ToastUtils.show("请检查短信验证码是否正确!")
        }
    }

    /**
     * 修改密码_通过验证码校验
     */
    private fun toChangePassUseCode(newPass: String, code: String) {
        DialogUtils.showBeautifulDialog(this)
        val changePassUseCode = RetrofitUtils.builder().changePassUseCode(newPass, code)
        changePassUseCodeObservable = changePassUseCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, newPass)
                        ToastUtils.show("修改密码成功")
                        finish()
                    }
                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(this)
                    }
                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 校验旧密码修改是否正常
     */
    private fun toCheckUseOldPass() {
        val isOldPassOk =
            tv_changePass_oldPass_tips.text.startsWith("√") && tv_changePass_oldPass_tips.visibility == View.VISIBLE
        val isNewPassOk =
            tv_changePass_pass_tips.text.startsWith("√") && tv_changePass_pass_tips.visibility == View.VISIBLE
        val isRePassOk =
            tv_changePass_rePass_tips.text.startsWith("√") && tv_changePass_rePass_tips.visibility == View.VISIBLE
        if (isOldPassOk && isNewPassOk && isRePassOk) {
            toChangePassUseOldPass(
                et_changePass_oldPass.text.toString().trim(),
                et_changePass_pass.text.toString().trim()
            )
        } else {
            ToastUtils.show("请检查账号密码是否合规后再进行!")
        }
    }

    /**
     * 修改密码_通过旧密码校验
     */
    private fun toChangePassUseOldPass(oldPass: String, newPass: String) {
        DialogUtils.showBeautifulDialog(this)
        val changePassUseOldPass = RetrofitUtils.builder().changePassUseOldPass(oldPass, newPass)
        changePassUseOldPassObservable = changePassUseOldPass.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, newPass)
                        ToastUtils.show("修改密码成功")
                        finish()
                    }
                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(this)
                    }
                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun destroy() {
        changePassUseOldPassObservable?.dispose()
        changePassUseOldPassObservable = null

        changePassSendCodeObservable?.dispose()
        changePassSendCodeObservable = null

        changePassUseCodeObservable?.dispose()
        changePassUseCodeObservable = null

        timeObservable?.dispose()
        timeObservable = null
    }
}