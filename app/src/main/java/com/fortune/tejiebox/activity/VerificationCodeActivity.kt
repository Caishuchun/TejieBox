package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.bean.GameInfo4ClipboardBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.ActivityManager
import com.fortune.tejiebox.utils.DialogUtils
import com.fortune.tejiebox.utils.GetDeviceId
import com.fortune.tejiebox.utils.HttpExceptionUtils
import com.fortune.tejiebox.utils.IPMacAndLocationUtils
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.PromoteUtils
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_verification_code.*
import kotlinx.android.synthetic.main.fragment_login_second.et_login_second_code
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class VerificationCodeActivity : BaseActivity() {

    private var countDownTimer: Disposable? = null
    private var isSendVerificationCode = false
    private var type: TITLE = TITLE.FIRST_SIGN_IN
    private var account: String? = null
    private var phoneEnd: String? = null
    private var tempPhone: String? = null

    private var sendVerificationCodeObservable: Disposable? = null
    private var verificationCodeObservable: Disposable? = null
    private var checkAccountAndPhoneObservable: Disposable? = null
    private var loginObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: VerificationCodeActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val TYPE = "type"
        const val ACCOUNT = "account"
        const val PHONE_END = "phone_end"
    }

    enum class TITLE {
        FIRST_SIGN_IN, FIRST_WHITE_PIAO, FIRST_USE_BALANCE, CHANGE_PHONE, CHANGE_DEVICE
    }

    override fun getLayoutId() = R.layout.activity_verification_code

    override fun doSomething() {
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {

        type = (intent.getSerializableExtra(TYPE) as TITLE)
        when (type) {
            TITLE.FIRST_SIGN_IN -> {
                tv_verification_code_title.text = "首次签到"
            }

            TITLE.FIRST_WHITE_PIAO -> {
                tv_verification_code_title.text = "首次白嫖"
            }

            TITLE.FIRST_USE_BALANCE -> {
                tv_verification_code_title.text = "首次使用余额"
            }

            TITLE.CHANGE_PHONE -> {
                tv_verification_code_title.text = "更换手机号"
                tv_verification_code_sure.text = "下一步"
            }

            TITLE.CHANGE_DEVICE -> {
                tv_verification_code_phone.visibility = View.GONE
                ll_verification_code_phone.visibility = View.VISIBLE
                tv_verification_code_title.text = "登录"
                tv_verification_code_sure.text = "登录"

                account = intent.getStringExtra(ACCOUNT)
                phoneEnd = intent.getStringExtra(PHONE_END)
                tv_verification_code_phoneEnd.text = phoneEnd
            }
        }

        RxView.clicks(iv_verification_code_close)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)
        if (phone?.isEmpty() == true) {
            ToastUtils.show("暂未绑定手机号")
            finish()
        }
        tv_verification_code_phone.text = phone?.replaceRange(3, 7, "****")

        val verificationCodeTime = SPUtils.getLong(SPArgument.VERIFICATION_CODE_TIME)
        val currentTimeMillis = System.currentTimeMillis()
        val interval = ((verificationCodeTime - currentTimeMillis) / 1000).toInt()
        if (interval < 0) {
            //时间间隔超过60秒需要重新发送短信验证码
            tv_verification_code_send.text = "发送"
            tv_verification_code_send.isEnabled = true
        } else {
            //直接展示倒计时
            isSendVerificationCode = true
            tv_verification_code_send.isEnabled = false
            showCountDown(interval)
        }

        RxView.clicks(tv_verification_code_send)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (account == null) {
                    //首次签到、首次白嫖、首次使用余额、更换手机号
                    toSendVerificationCode(phone!!)
                } else {
                    //更换设备
                    val phoneStart = et_verification_code_phoneStart.text.toString()
                    if (phoneStart.length < 7) {
                        ToastUtils.show("请输入正确的手机号")
                        return@subscribe
                    } else {
                        toCheckAccountAndPhone(account!!, phoneStart + phoneEnd!!)
                    }
                }
            }

        RxView.clicks(tv_verification_code_sure)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (!isSendVerificationCode) {
                    ToastUtils.show("请先发送验证码")
                    return@subscribe
                }
                if (et_verification_code_code.text.length == 6) {
                    val code = et_verification_code_code.text.toString()
                    if (type == TITLE.CHANGE_PHONE) {
                        //更换手机号，直接传值到下一个界面
                        toChangePhone(phone!!, code)
                    } else if (type == TITLE.CHANGE_DEVICE) {
                        //更换设备,直接登录
                        val allPhone = et_verification_code_phoneStart.text.toString() + phoneEnd!!
                        if (allPhone == tempPhone) {
                            toLogin(tempPhone!!, code)
                        } else {
                            ToastUtils.show("请勿修改已补全手机号")
                        }
                    } else {
                        //首次签到、首次白嫖、首次使用余额，需要传值到下一个界面
                        toCheckCode(phone!!, code)
                    }
                } else {
                    ToastUtils.show("请输入正确的验证码")
                }
            }
    }

    /**
     * 登录
     */
    private fun toLogin(phone: String, code: String) {
        val data = GameInfo4ClipboardBean.getData()
        val gameChannel = data?.channelId
        val gameVersion = data?.version
        var gameId: Int? = SPUtils.getInt(SPArgument.NEED_JUMP_GAME_ID_UPDATE, -1)
        if (gameId == -1) {
            gameId = null
        }
        val inviteInfo = SPUtils.getString(SPArgument.OPEN_INSTALL_INFO)
        DialogUtils.showBeautifulDialog(this)
        val login = RetrofitUtils.builder().login(
            phone = phone,
            captcha = code.toInt(),
            device_id = GetDeviceId.getDeviceId(this),
            game_channel = gameChannel,
            game_id = gameId,
            game_version = gameVersion,
            i = inviteInfo,
            latitude = IPMacAndLocationUtils.getLatitude(this),
            longitude = IPMacAndLocationUtils.getLongitude(this)
        )
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, null)
        SPUtils.putValue(SPArgument.USER_ID, null)
        SPUtils.putValue(SPArgument.USER_ID_NEW, null)
        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
        SPUtils.putValue(SPArgument.ID_NAME, null)
        SPUtils.putValue(SPArgument.ID_NUM, null)
        loginObservable = login.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success==>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_UPDATE, -1)
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            tempPhone = null
                            SPUtils.putValue(SPArgument.OPEN_INSTALL_USED, true)
                            SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT, true)
                            SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                            SPUtils.putValue(SPArgument.PHONE_NUMBER, it.data?.phone)
                            SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, it.data?.account)
                            SPUtils.putValue(SPArgument.IS_HAVE_ID, it.data?.id_card)
                            SPUtils.putValue(SPArgument.USER_ID, it.data?.user_id)
                            SPUtils.putValue(SPArgument.USER_ID_NEW, it.data?.user_id_raw)
                            if (it.data?.id_card == 1) {
                                SPUtils.putValue(SPArgument.ID_NAME, it.data?.card_name)
                                SPUtils.putValue(SPArgument.ID_NUM, it.data?.car_num)
                            }

                            // 是否有奖励积分可以弹框
                            var isHaveRewardInteger = false
                            if (it.data?.first_login == 1) {
                                // 首次注册的推广统计
//                                if (BaseAppUpdateSetting.isToPromoteVersion) {
                                PromoteUtils.promote(this)
//                                }
                                // 打电话推广, 首次注册且有奖励积分的
//                                if (it.data?.integral != null && it.data?.integral!! > 0) {
//                                    isHaveRewardInteger = true
//                                    DialogActivity.showGetIntegral(
//                                        this,
//                                        it.data?.integral!!,
//                                        true,
//                                        null
//                                    )
//                                }
                                //openInstall 注册统计
//                                OpenInstall.reportRegister()
                            }
                            EventBus.getDefault().postSticky(
                                LoginStatusChange(
                                    true,
                                    it.data?.phone,
                                    it.data?.account,
                                    isHaveRewardInteger,
                                    it.data?.first_login == 1
                                )
                            )
                            finish()
                            LoginActivity.getInstance()?.finish()
                            Login4AccountActivity.getInstance()?.finish()
                        }

                        else -> {
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                            et_login_second_code.setText("")
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
     * 检查账号和手机号是否匹配
     */
    private fun toCheckAccountAndPhone(account: String, phone: String) {
        val checkAccountAndPhone = RetrofitUtils.builder().checkAccountAndPhone(phone, account)
        checkAccountAndPhoneObservable = checkAccountAndPhone
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            tempPhone = phone
                            toSendVerificationCode(phone, true)
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
     * 验证验证码是否是合规的
     */
    private fun toCheckCode(phone: String, code: String) {
        val verificationType =
            if (type == TITLE.FIRST_SIGN_IN || type == TITLE.FIRST_WHITE_PIAO) 1 else 2
        val verificationCode =
            RetrofitUtils.builder().verificationCode(phone, code, verificationType)
        verificationCodeObservable = verificationCode
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            val intent = Intent()
                            val codes =
                                if (type == TITLE.FIRST_SIGN_IN) 1 else if (type == TITLE.FIRST_WHITE_PIAO) 2 else 3
                            intent.putExtra("code", codes)
                            setResult(RESULT_OK, intent)
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
     * 跳转去更换手机号界面
     */
    private fun toChangePhone(phone: String, code: String) {
        val intent = Intent()
        intent.putExtra("code", 4)
        intent.putExtra("old_phone", phone)
        intent.putExtra("old_captcha", code)
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * 发送短信验证码
     */
    private fun toSendVerificationCode(phone: String, isLogin: Boolean = false) {
        DialogUtils.showBeautifulDialog(this)
        val type =
            if (type == TITLE.FIRST_SIGN_IN || type == TITLE.FIRST_WHITE_PIAO) 1 else if (type == TITLE.FIRST_USE_BALANCE) 2 else null
        val sendVerificationCode =
            if (isLogin) RetrofitUtils.builder().sendCode(phone)
            else RetrofitUtils.builder().sendCode4changePhone(phone, type)
        sendVerificationCodeObservable = sendVerificationCode
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            isSendVerificationCode = true
                            SPUtils.putValue(
                                SPArgument.VERIFICATION_CODE_TIME,
                                System.currentTimeMillis() + 60 * 1000
                            )
                            showCountDown(60)
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
     * 展示倒计时
     */
    @SuppressLint("SetTextI18n")
    private fun showCountDown(seconds: Int) {
        var allTime = seconds
        countDownTimer?.dispose()
        countDownTimer = null
        countDownTimer = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (allTime == 0) {
                    tv_verification_code_send.text = "重发"
                    tv_verification_code_send.isEnabled = true
                    countDownTimer?.dispose()
                    countDownTimer = null
                } else {
                    allTime--
                    tv_verification_code_send.text = "${allTime}s"
                }
            }
    }

    override fun destroy() {
        countDownTimer?.dispose()
        countDownTimer = null

        sendVerificationCodeObservable?.dispose()
        sendVerificationCodeObservable = null

        verificationCodeObservable?.dispose()
        verificationCodeObservable = null

        checkAccountAndPhoneObservable?.dispose()
        checkAccountAndPhoneObservable = null

        loginObservable?.dispose()
        loginObservable = null
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