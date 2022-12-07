package com.fortune.tejiebox.fragment

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.activity.LoginActivity
import com.fortune.tejiebox.activity.WebActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_account_login.view.*
import kotlinx.android.synthetic.main.fragment_login_normal.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class AccountLoginFragment : Fragment() {

    companion object {
        fun newInstance() = AccountLoginFragment()
    }

    private var mView: View? = null
    private var loginPassIsShow = false
    private var accountLoginObservable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_account_login, container, false)
        initView()
        return mView
    }

    private fun initView() {
        val isAgree = SPUtils.getBoolean(SPArgument.IS_CHECK_AGREEMENT, false)
        if (isAgree || !BaseAppUpdateSetting.isToPromoteVersion) {
            mView?.cb_account_login?.isChecked = true
        }

        val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT, null)
        val pass = SPUtils.getString(SPArgument.LOGIN_ACCOUNT_PASS, null)
        if (!account.isNullOrBlank() && !pass.isNullOrBlank()) {
            mView?.et_account_login_account?.setText(account)
            mView?.et_account_login_pass?.setText(pass)
        }

        mView?.iv_account_login_back?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    requireActivity().finish()
                }
        }

        mView?.iv_account_login_title?.let {
            it.setImageResource(if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.app_title2 else R.mipmap.app_title)
        }

        mView?.et_account_login_account?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        ToastUtils.show("登录账号不得超过16位字符")
                        et.setText(it.substring(0, it.length - 1))
                        et.setSelection(it.length - 1)
                    }
                }
        }

        mView?.et_account_login_pass?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        ToastUtils.show("登录密码不得超过16位字符")
                        et.setText(it.substring(0, it.length - 1))
                        et.setSelection(it.length - 1)
                    }
                }
        }

        mView?.iv_account_login_pass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    loginPassIsShow = !loginPassIsShow
                    mView?.et_account_login_pass?.let {
                        it.transformationMethod = if (loginPassIsShow) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        it.setSelection(it.length())
                    }
                    iv.setImageResource(if (!loginPassIsShow) R.mipmap.pass_show else R.mipmap.pass_unshow)
                }
        }

        mView?.tv_account_login_login?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (mView?.cb_account_login?.isChecked == true) {
                        toCheckLogin()
                    } else {
                        DialogUtils.showAgreementDialog(
                            requireContext(),
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    mView?.cb_account_login?.isChecked = true
                                    toCheckLogin()
                                }
                            }
                        )
                        return@subscribe
                    }
                }
        }

        mView?.tv_account_login_sign?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    Login4AccountActivity.getInstance()?.switchFragment(1)
                }
        }

        mView?.tv_account_login_userAgreement?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    val intent = Intent(activity, WebActivity::class.java)
                    intent.putExtra(WebActivity.TYPE, WebActivity.USER_AGREEMENT)
                    requireActivity().startActivity(intent)
                }
        }
        mView?.tv_account_login_privacyAgreement?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    val intent = Intent(activity, WebActivity::class.java)
                    intent.putExtra(WebActivity.TYPE, WebActivity.PRIVACY_AGREEMENT)
                    requireActivity().startActivity(intent)
                }
        }
    }

    /**
     * 检查登录
     */
    private fun toCheckLogin() {
        val account = mView?.et_account_login_account?.text.toString().trim()
        val pass = mView?.et_account_login_pass?.text.toString().trim()
        when {
            account.length < 8 -> {
                ToastUtils.show("登录账号长度不足8位字符")
            }
            pass.length < 8 -> {
                ToastUtils.show("登录密码长度不足8位字符")
            }
            else -> {
                toLogin(account, pass)
            }
        }
    }

    /**
     * 账号密码登录
     */
    private fun toLogin(account: String, pass: String) {
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, null)
        SPUtils.putValue(SPArgument.USER_ID, null)
        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
        SPUtils.putValue(SPArgument.ID_NAME, null)
        SPUtils.putValue(SPArgument.ID_NUM, null)
        DialogUtils.showBeautifulDialog(requireContext())
        val accountLogin = RetrofitUtils.builder().accountLogin(account, pass)
        accountLoginObservable = accountLogin.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("success=>${Gson().toJson(it)}")
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT, true)
                        SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                        SPUtils.putValue(SPArgument.PHONE_NUMBER, it.data?.phone)
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, it.data?.account)
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, pass)
                        SPUtils.putValue(SPArgument.USER_ID, it.data?.user_id)
                        SPUtils.putValue(SPArgument.IS_HAVE_ID, it.data?.id_card)
                        if (it.data?.id_card == 1) {
                            SPUtils.putValue(SPArgument.ID_NAME, it.data?.card_name)
                            SPUtils.putValue(SPArgument.ID_NUM, it.data?.car_num)
                        }

                        // 是否有奖励积分可以弹框
                        var isHaveRewardInteger = false
                        if (it.data?.first_login == 1) {
                            // 首次注册的推广统计
                            if (BaseAppUpdateSetting.isToPromoteVersion) {
                                PromoteUtils.promote(requireActivity())
                            }
                            // 首次注册且有奖励积分的
                            if (it.data?.integral != null && it.data?.integral!! > 0) {
                                isHaveRewardInteger = true
                                DialogActivity.showGetIntegral(
                                    requireActivity(),
                                    it.data?.integral!!,
                                    true,
                                    null
                                )
                            }
                        }

                        EventBus.getDefault().postSticky(
                            LoginStatusChange(
                                true,
                                it.data?.phone,
                                it.data?.account,
                                isHaveRewardInteger
                            )
                        )
                        toFinishAllLogin()
                    }
                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            }
            )
    }

    /**
     * 关闭所有登录相关
     */
    private fun toFinishAllLogin() {
        LoginActivity.getInstance()?.finish()
        requireActivity().finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        accountLoginObservable?.dispose()
        accountLoginObservable = null
    }
}