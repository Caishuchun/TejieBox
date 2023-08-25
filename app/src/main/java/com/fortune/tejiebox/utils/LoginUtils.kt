package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.activity.LoginActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.GameInfo4ClipboardBean
import com.fortune.tejiebox.constants.FilesArgument
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.http.HttpUrls
import com.fortune.tejiebox.http.RetrofitUtils
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.mobile.auth.gatewayauth.*
import com.mobile.auth.gatewayauth.model.TokenRet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_quick_login_body.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

object LoginUtils {

    private var quickLogin4AliObservable: Disposable? = null
    private var helper: PhoneNumberAuthHelper? = null
    private var isFirstCheck = true
    private var isInit = true

    /**
     * 初始化
     */
    @SuppressLint("CheckResult")
    fun init(activity: Activity) {
        val isAgree = SPUtils.getBoolean(SPArgument.IS_CHECK_AGREEMENT, false)
        isInit = true
        helper?.clearPreInfo()
        val mTokenResultListener = object : TokenResultListener {
            override fun onTokenSuccess(result: String?) {
                LogUtils.d("Ali=>onTokenSuccess:${Gson().toJson(result)}")
                if (result != null) {
                    val tokenRet = Gson().fromJson<TokenRet>(
                        result,
                        TokenRet::class.java
                    )
                    toDealAliListener(activity, tokenRet)
                }
            }

            override fun onTokenFailed(result: String?) {
                LogUtils.d("Ali=>onTokenFailed:${Gson().toJson(result)}")
                val tokenRet = Gson().fromJson<TokenRet>(
                    result,
                    TokenRet::class.java
                )
                toDealAliListener(activity, tokenRet)
            }
        }

        helper = PhoneNumberAuthHelper.getInstance(activity, mTokenResultListener)
        helper?.setAuthSDKInfo("m73eA2DbbCaEcgCqkoMaEof9/IYQmkAefyUXbQr+Sl4gY3m4yaE705KHsPl4szssTyKujGp9ctWiGd1np58TH/afciOftT+e4satI7U/qOr1FJPDSXpYDtaMyINWmsN0xA9vVgZ9TmFYVYnWcaBtzZWMs8ua95g5YWW37geyAAOYeu26w/xhIw4vQqJmUT6fEvrDFVTae99djKqXeIAq6div6hof9c0qvOtvJsmRavWAinKkPw9AIy1C/cR8HRc/q8cH+J+yCXIP+G/ED9oyl+Cfrsom0L9sXLl2iXsAUAT10/tbnMMQeQ==")
        helper?.setAuthUIConfig(
            AuthUIConfig.Builder()
                //背景
                .setPageBackgroundPath("bg_login")
                //状态栏
                .setStatusBarColor(Color.parseColor("#FFFFFF"))
                .setLightColor(true)
                .setWebViewStatusBarColor(Color.parseColor("#FFFFFF"))
                .setWebNavColor(Color.parseColor("#FFFFFF"))
                .setWebNavTextColor(Color.parseColor("#0A0422"))
                .setWebSupportedJavascript(true)
                .setNavReturnImgDrawable(activity.getDrawable(R.drawable.back_black))
                .setNavReturnImgWidth(px2dp(activity, 24f))
                .setNavReturnImgHeight(px2dp(activity, 24f))
                //标题
                .setNavHidden(true)
                //服务商
                .setSloganTextSizeDp(px2dp(activity, 14f))
                .setSloganText(" ")
                .setSloganTextColor(Color.parseColor("#0A0422"))
                //掩码
                .setNumberSizeDp(px2dp(activity, 16f))
                .setNumberLayoutGravity(Gravity.CENTER)
                .setNumberColor(Color.parseColor("#0A0422"))
                //一键登录按钮
                .setLogBtnWidth(px2dp(activity, 296f))
                .setLogBtnHeight(px2dp(activity, 48f))
                .setLogBtnBackgroundPath("bg_btn")
                .setLogBtnText(activity.getString(R.string.login_quick))
                .setLogBtnTextColor(Color.parseColor("#FFFFFF"))
                .setLogBtnTextSizeDp(px2dp(activity, 16f))
                //切换登录方式
                .setSwitchAccHidden(true)
                .setSwitchAccText(activity.getString(R.string.login_other_phone))
                .setSwitchAccTextColor(Color.parseColor("#0A0422"))
                .setSwitchAccTextSizeDp(px2dp(activity, 14f))
                //协议
                .setAppPrivacyOne(
                    activity.getString(R.string.user_agreement),
                    (if (BaseAppUpdateSetting.appType) HttpUrls.REAL_URL else HttpUrls.TEST_URL) + FilesArgument.PROTOCOL_SERVICE
                )
                .setAppPrivacyTwo(
                    activity.getString(R.string.privacy_agreement),
                    (if (BaseAppUpdateSetting.appType) HttpUrls.REAL_URL else HttpUrls.TEST_URL) + FilesArgument.PROTOCOL_PRIVACY
                )
                .setPrivacyBefore(activity.getString(R.string.login_tips))
                .setCheckboxHidden(false)
                .setCheckBoxWidth(px2dp(activity, 24f))
                .setCheckBoxHeight(px2dp(activity, 24f))
                .setPrivacyState(
                    isAgree
//                        || !BaseAppUpdateSetting.isToPromoteVersion
                )
                .setCheckedImgDrawable(activity.getDrawable(R.drawable.checked))
                .setUncheckedImgDrawable(activity.getDrawable(R.drawable.uncheck))
                .setVendorPrivacyPrefix("《")
                .setVendorPrivacySuffix("》")
                .setAppPrivacyColor(Color.parseColor("#990A0422"), Color.parseColor("#5F60FF"))
                //协议弹框
                .setPrivacyAlertIsNeedShow(true)
                .setPrivacyAlertWidth(px2dp(activity, 300f))
                .setPrivacyAlertHeight(px2dp(activity, 150f))
                .setPrivacyAlertBackgroundColor(Color.parseColor("#FFFFFF"))
                .setPrivacyAlertCornerRadiusArray(
                    intArrayOf(
                        px2dp(activity, 10f),
                        px2dp(activity, 10f),
                        px2dp(activity, 10f),
                        px2dp(activity, 10f)
                    )
                )
                .setPrivacyAlertTitleTextSize(px2dp(activity, 16f))
                .setPrivacyAlertContentTextSize(px2dp(activity, 12f))
                .setPrivacyAlertBtnTextSize(px2dp(activity, 14f))
                .setPrivacyAlertBtnBackgroundImgPath("bg_btn")
                .setPrivacyAlertBtnWidth(px2dp(activity, 100f))
                .setPrivacyAlertBtnHeigth(px2dp(activity, 30f))
                .setPrivacyAlertContentAlignment(Gravity.CENTER)
                .setPrivacyAlertContentHorizontalMargin(px2dp(activity, 10f))
                .setPrivacyAlertContentVerticalMargin(px2dp(activity, 15f))
                .setPrivacyAlertBtnTextColor(Color.parseColor("#FFFFFF"))
                .setPrivacyAlertCloseImgWidth(px2dp(activity, 15f))
                .setPrivacyAlertCloseImgHeight(px2dp(activity, 15f))
                .create()
        )

        //适配全面屏
        helper?.keepAuthPageLandscapeFullSreen(true)

        val numView = LayoutInflater.from(activity).inflate(R.layout.layout_quick_login_num, null)
        helper?.addAuthRegistViewConfig(
            "num", AuthRegisterViewConfig.Builder()
                .setView(numView)
                .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_NUMBER)
                .build()
        )
        val bodyView = LayoutInflater.from(activity).inflate(R.layout.layout_quick_login_body, null)
        bodyView.iv_login4ali_title.setImageResource(
            if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.app_title2
            else R.mipmap.app_title
        )
        RxView.clicks(bodyView.iv_login4ali_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                hideLoadingAndQuitLoginPage()
            }
        RxView.clicks(bodyView.tv_login4ali_sms)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }
        RxView.clicks(bodyView.tv_login4ali_account)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                hideLoadingAndQuitLoginPage()
                activity.startActivity(Intent(activity, Login4AccountActivity::class.java))
            }
        helper?.addAuthRegistViewConfig(
            "body", AuthRegisterViewConfig.Builder()
                .setView(bodyView)
                .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_BODY)
                .build()
        )
        //配置完直接检查终端⽹络环境是否⽀持⼀键登录
        helper?.checkEnvAvailable(PhoneNumberAuthHelper.SERVICE_TYPE_LOGIN)
    }

    /**
     * px转dp
     */
    private fun px2dp(activity: Activity, num: Float) =
        OtherUtils.px2dp(
            activity,
            (PhoneInfoUtils.getWidth(activity).toFloat()
                .coerceAtMost(PhoneInfoUtils.getHeight(activity).toFloat())) / 360.0f * num
        )

    /**
     * 阿里云一键登录
     * @param directJumpLoginPage 是否直接跳转到登录界面
     */
    @SuppressLint("CheckResult")
    fun toQuickLogin(activity: Activity, directJumpLoginPage: Boolean = false) {
        if (directJumpLoginPage) {
            toLogin(activity, false)
            return
        }
        isInit = false
        helper?.getLoginToken(activity, 1500)
        DialogUtils.showBeautifulDialog(activity)
    }

    /**
     * 处理阿里云一键登录回调的返回
     */
    private fun toDealAliListener(
        activity: Activity,
        tokenRet: TokenRet
    ) {
        LogUtils.d("Ali=>toDealAliListener==code:${tokenRet.code}")
        when (tokenRet.code) {
            "600000" -> {
                if (isInit) return
                //获取token成功
                toRealLogin4Ail(activity, tokenRet.token)
            }

            "600001" -> {
                if (isInit) return
                //唤起授权⻚成功
                DialogUtils.dismissLoading()
            }

            "600002" -> {
                if (isInit) return
                //唤起授权⻚失败,建议切换到其他登录⽅式
                hideLoadingAndQuitLoginPage()
                toLogin(activity, true)
            }

            "600004" -> {
                if (isInit) return
                //获取运营商配置信息失败,创建⼯单联系⼯程师
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600005" -> {
                if (isInit) return
                //⼿机终端不安全,切换到其他登录⽅式
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600007" -> {
                if (isInit) return
                //未检测到sim卡,提示⽤户检查 SIM 卡后重试
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600008" -> {
                if (isInit) return
                //蜂窝⽹络未开启,提示⽤户开启移动⽹络后重试
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600009" -> {
                if (isInit) return
                //⽆法判断运营商,创建⼯单联系⼯程师
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600010" -> {
                if (isInit) return
                //未知异常,创建⼯单联系⼯程师
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600011" -> {
                if (isInit) return
                //创建⼯单联系⼯程师,切换到其他登录⽅式
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600012" -> {
                if (isInit) return
                //预取号失败
                hideLoadingAndQuitLoginPage()
                toLogin(activity, true)
            }

            "600013" -> {
                if (isInit) return
                //运营商维护升级,该功能不可⽤,创建⼯单联系⼯程师
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600014" -> {
                if (isInit) return
                //运营商维护升级，该功能已达最⼤调⽤次数,创建⼯单联系⼯程师
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600015" -> {
                if (isInit) return
                //接⼝超时,切换到其他登录⽅式
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }

            "600017" -> {
                if (isInit) return
                //AppID、Appkey解析失败
                hideLoadingAndQuitLoginPage()
                toLogin(activity, true)
            }

            "600021" -> {
                if (isInit) return
                //点击登录时检测到运营商已切换,提示⽤户退出授权⻚，重新登录
                hideLoadingAndQuitLoginPage()
                toLogin(activity, true)
            }

            "600023" -> {
                if (isInit) return
                //加载⾃定义控件异常
                hideLoadingAndQuitLoginPage()
                toLogin(activity, true)
            }

            "600024" -> {
                //终端环境检查⽀持认证
                helper?.accelerateLoginPage(3000, object : PreLoginResultListener {
                    override fun onTokenSuccess(p0: String?) {
                        //预取号成功
                        LogUtils.d("Ali=>预取号成功:$p0")
                    }

                    override fun onTokenFailed(p0: String?, p1: String?) {
                        //预取号失败
                        LogUtils.d("Ali=>预取号失败")
                    }
                })
            }

            "600025" -> {
                if (isInit) return
                //终端检测参数错误,检查传⼊参数类型与范围是否正确
                hideLoadingAndQuitLoginPage()
                toLogin(activity, true)
            }

            "600026" -> {
                if (isInit) return
                //授权⻚已加载时不允许调⽤加速或预取号接⼝, 检查是否有授权⻚拉起后，去调⽤preLogin 或者accelerateAuthPage的接⼝，该⾏为不 允许
            }

            "700000" -> {
                if (isInit) return
                //点击返回，⽤户取消免密登录
                hideLoadingAndQuitLoginPage()
            }

            "700001" -> {
                if (isInit) return
                //点击切换按钮，⽤户取消免密登录
                hideLoadingAndQuitLoginPage()
                toLogin(activity, false)
            }
        }
    }

    /**
     * 隐藏登录加载和退出登录页面
     */
    private fun hideLoadingAndQuitLoginPage() {
        helper?.hideLoginLoading()
        helper?.quitLoginPage()
    }

    /**
     * 跳转到登录界面
     * @param needRetry 是否需要重试
     */
    private fun toLogin(activity: Activity, needRetry: Boolean) {
        //失败之后再来一次,如果依然失败,否则进行短信验证码登录
        if (isFirstCheck && needRetry) {
            isFirstCheck = false
            helper?.getLoginToken(activity, 1500)
        } else {
            isFirstCheck = true
            DialogUtils.dismissLoading()
            LogUtils.d("toLogin..................")
            activity.startActivity(Intent(activity, LoginActivity::class.java))
        }
    }

    /**
     * 真的就去登录
     */
    @SuppressLint("CheckResult")
    private fun toRealLogin4Ail(
        activity: Activity,
        accessCode: String
    ) {
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, null)
        SPUtils.putValue(SPArgument.USER_ID, null)
        SPUtils.putValue(SPArgument.USER_ID_NEW, null)
        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
        SPUtils.putValue(SPArgument.ID_NAME, null)
        SPUtils.putValue(SPArgument.ID_NUM, null)
        val data = GameInfo4ClipboardBean.getData()
        val gameChannel = data?.channelId
        val gameVersion = data?.version
        var gameId: Int? = SPUtils.getInt(SPArgument.NEED_JUMP_GAME_ID_UPDATE, -1)
        if (gameId == -1) {
            gameId = null
        }
        val inviteInfo = SPUtils.getString(SPArgument.OPEN_INSTALL_INFO)
        val quickLogin4Ali = RetrofitUtils.builder().quickLogin4Ali(
            access_token = accessCode,
            device_id = GetDeviceId.getDeviceId(activity),
            game_channel = gameChannel,
            game_id = gameId,
            game_version = gameVersion,
            i = inviteInfo
        )
        quickLogin4AliObservable = quickLogin4Ali.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_UPDATE, -1)
                LogUtils.d("Result==>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.OPEN_INSTALL_USED, true)
                            SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT, true)
                            SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                            SPUtils.putValue(SPArgument.PHONE_NUMBER, it.data?.phone)
                            SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, it.data?.account)
                            SPUtils.putValue(SPArgument.USER_ID, it.data?.user_id)
                            SPUtils.putValue(SPArgument.USER_ID_NEW, it.data?.user_id_raw)
                            SPUtils.putValue(SPArgument.IS_HAVE_ID, it.data?.id_card)
                            if (it.data?.id_card == 1) {
                                SPUtils.putValue(SPArgument.ID_NAME, it.data?.card_name)
                                SPUtils.putValue(SPArgument.ID_NUM, it.data?.car_num)
                            }

                            // 是否有奖励积分可以弹框
                            var isHaveRewardInteger = false
                            if (it.data?.first_login == 1) {
                                // 首次注册的推广统计
//                                if (BaseAppUpdateSetting.isToPromoteVersion) {
                                PromoteUtils.promote(activity)
//                                }
                                // 打电话推广, 首次注册且有奖励积分的
//                                if (it.data?.integral != null && it.data?.integral!! > 0) {
//                                    isHaveRewardInteger = true
//                                    DialogActivity.showGetIntegral(
//                                        activity,
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
                            hideLoadingAndQuitLoginPage()
                        }

                        else -> {
                            helper?.hideLoginLoading()
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    helper?.hideLoginLoading()
                    ToastUtils.show(activity.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                helper?.hideLoginLoading()
                LogUtils.d("Fail==>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                ToastUtils.show(it.message.toString())
            })
    }
}