package com.fortune.tejiebox.fragment

import android.content.Intent
import android.os.Bundle
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
import kotlinx.android.synthetic.main.fragment_account_login.view.*
import kotlinx.android.synthetic.main.fragment_login_normal.view.*
import java.util.concurrent.TimeUnit

class AccountLoginFragment : Fragment() {

    companion object {
        fun newInstance() = AccountLoginFragment()
    }

    private var mView: View? = null

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
        if (account.length > 5 && pass.length > 5) {
            toLogin(account, pass)
        } else {
            ToastUtils.show("登录账号或登录密码错误,请重新输入")
        }
    }

    /**
     * 账号密码登录
     */
    private fun toLogin(account: String, pass: String) {

    }
}