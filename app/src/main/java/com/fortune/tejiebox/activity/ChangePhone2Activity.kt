package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.os.SystemClock
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginPhoneChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_change_phone2.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class ChangePhone2Activity : BaseActivity() {

    private var timer: Disposable? = null

    private var sendCodeObservable: Disposable? = null
    private var changePhoneObservable: Disposable? = null
    private var bindPhoneObservable: Disposable? = null
    private var lastTime = 59
    private var currentPhone = ""

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ChangePhone2Activity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val PHONE = "phone"

        const val IS_BIND = "isBind"

        const val OLD_PHONE = "oldPhone"
        const val OLD_CAPTCHA = "oldCaptcha"
    }

    private var isBind = false

    private var oldPhone: String? = null
    private var oldCaptcha: String? = null
    override fun getLayoutId() = R.layout.activity_change_phone2

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        currentPhone = intent.getStringExtra(PHONE)!!
        isBind = intent.getBooleanExtra(IS_BIND, false)
        if (isBind) {
            tv_changePhone2_title.text = "绑定手机号"
        }
        oldPhone = intent.getStringExtra(OLD_PHONE)
        oldCaptcha = intent.getStringExtra(OLD_CAPTCHA)
        initView()
        val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
        val currentTimeMillis = SystemClock.uptimeMillis()
        if (oldTimeMillis == 0L) {
            //历史时间没有的话,就要重新倒计时
        } else {
            when {
                currentTimeMillis - oldTimeMillis > 60 * 1000 -> {
                    //当前时间超过历史时间1分钟,重新倒计时
                }

                currentTimeMillis < oldTimeMillis -> {
                    //当前时间小于历史时间,说明重新开机过,重新倒计时
                }

                else -> {
                    //直接获取剩余时间,倒计时
                    lastTime -= ((currentTimeMillis - oldTimeMillis) / 1000).toInt()
                }
            }
        }
        toShowTime()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        tv_changePhone2_currentPhone.text = "${getString(R.string.send_to)} +86 $currentPhone"

        RxView.clicks(iv_changePhone2_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        RxView.clicks(tv_changePhone2_reSend)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { sendCode() }

        RxTextView.textChanges(et_changePhone2_code)
            .skipInitialValue()
            .subscribe {
                changeCodeBg(it.toString())
                if (it.length == 6) {
                    if (isBind) toBindPhone(it.toString())
                    else toChangePhone(it.toString())
                }
            }
    }

    /**
     * 绑定手机号
     */
    private fun toBindPhone(code: String) {
        DialogUtils.showBeautifulDialog(this)
        val bingPhone =
            RetrofitUtils.builder().bingPhone(currentPhone, code, GetDeviceId.getDeviceId(this))
        bindPhoneObservable = bingPhone.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            ToastUtils.show("绑定手机号成功!")
                            SPUtils.putValue(SPArgument.PHONE_NUMBER, currentPhone)
                            SPUtils.putValue(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
                            ChangePhone1Activity.getInstance()?.finish()
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
     * 开始修改手机号
     */
    private fun toChangePhone(code: String) {
        DialogUtils.showBeautifulDialog(this)
        val changePhone = RetrofitUtils.builder()
            .changePhone(currentPhone, code.toInt(), oldPhone!!, oldCaptcha!!)
        changePhoneObservable = changePhone.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            ToastUtils.show(getString(R.string.change_phone_success))
                            SPUtils.putValue(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
                            SPUtils.putValue(SPArgument.PHONE_NUMBER, currentPhone)
                            EventBus.getDefault().postSticky(LoginPhoneChange(currentPhone))
                            ChangePhone1Activity.getInstance()?.finish()
                            finish()
//                            toSplash()
                        }

                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }

                        else -> {
                            DialogUtils.showOnlySureDialog(
                                this, "绑定手机号", it.msg, "好的", false, null
                            )
                        }
//                        else -> {
//                            ToastUtils.show(it.msg)
//                        }
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
     * 跳转到起始页面进行登录
     */
    private fun toSplash() {
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
        SPUtils.putValue(SPArgument.USER_ID, null)
        SPUtils.putValue(SPArgument.USER_ID_NEW, null)
        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
        SPUtils.putValue(SPArgument.ID_NAME, null)
        SPUtils.putValue(SPArgument.ID_NUM, null)
        ActivityManager.toSplashActivity(this)
    }

    /**
     * 实时修改验证码界面
     */
    private fun changeCodeBg(code: String) {
        tv_code_1.setBackgroundResource(R.drawable.bg_code_unenter)
        tv_code_2.setBackgroundResource(R.drawable.bg_code_unenter)
        tv_code_3.setBackgroundResource(R.drawable.bg_code_unenter)
        tv_code_4.setBackgroundResource(R.drawable.bg_code_unenter)
        tv_code_5.setBackgroundResource(R.drawable.bg_code_unenter)
        tv_code_6.setBackgroundResource(R.drawable.bg_code_unenter)
        tv_code_1.text = ""
        tv_code_2.text = ""
        tv_code_3.text = ""
        tv_code_4.text = ""
        tv_code_5.text = ""
        tv_code_6.text = ""
        when (code.length) {
            0 -> {
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entering)
            }

            1 -> {
                tv_code_2.setBackgroundResource(R.drawable.bg_code_entering)
                tv_code_1.text = code[0].toString()
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
            }

            2 -> {
                tv_code_3.setBackgroundResource(R.drawable.bg_code_entering)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
            }

            3 -> {
                tv_code_4.setBackgroundResource(R.drawable.bg_code_entering)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
            }

            4 -> {
                tv_code_5.setBackgroundResource(R.drawable.bg_code_entering)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_4.text = code[3].toString()
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_4.setBackgroundResource(R.drawable.bg_code_entered)
            }

            5 -> {
                tv_code_6.setBackgroundResource(R.drawable.bg_code_entering)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_4.text = code[3].toString()
                tv_code_5.text = code[4].toString()
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_4.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_5.setBackgroundResource(R.drawable.bg_code_entered)
            }

            6 -> {
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_4.text = code[3].toString()
                tv_code_5.text = code[4].toString()
                tv_code_6.text = code[5].toString()
                tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_4.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_5.setBackgroundResource(R.drawable.bg_code_entered)
                tv_code_6.setBackgroundResource(R.drawable.bg_code_entered)
            }
        }
    }

    /**
     * 发送短信验证码
     */
    private fun sendCode() {
        DialogUtils.showBeautifulDialog(this)
        val sendCode4changePhone =
            if (isBind) RetrofitUtils.builder().sendCode(currentPhone, 1)
            else RetrofitUtils.builder().sendCode4changePhone(currentPhone)
        sendCodeObservable = sendCode4changePhone
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            toShowTime()
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }


    /**
     * 倒计时
     */
    private fun toShowTime() {
        timer?.dispose()
        timer = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (lastTime > 0) {
                    tv_changePhone2_reSend.isEnabled = false
                    tv_changePhone2_reSend.text =
                        "${MyApp.getInstance().getString(R.string.resend)}(${lastTime}s)"
                    lastTime--
                } else {
                    lastTime = 59
                    timer?.dispose()
                    tv_changePhone2_reSend.isEnabled = true
                    tv_changePhone2_reSend.text = MyApp.getInstance().getString(R.string.resend)
                }
            }
    }

    override fun destroy() {
        timer?.dispose()
        changePhoneObservable?.dispose()
        sendCodeObservable?.dispose()

        timer = null
        changePhoneObservable = null
        sendCodeObservable = null

        bindPhoneObservable?.dispose()
        bindPhoneObservable = null
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