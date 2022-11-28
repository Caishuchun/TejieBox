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
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.activity.WebActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.utils.DialogUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.fragment_account_login.view.*
import kotlinx.android.synthetic.main.fragment_login_normal.view.*
import java.util.concurrent.TimeUnit

class AccountLoginFragment : Fragment() {

    companion object {
        fun newInstance() = AccountLoginFragment()
    }

    private var mView: View? = null
    private var loginPassIsShow = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_account_login, container, false)
        initView()
        return mView
    }

    private fun initView() {
        if (!BaseAppUpdateSetting.isToPromoteVersion) {
            mView?.cb_account_login?.isChecked = true
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

    }
}