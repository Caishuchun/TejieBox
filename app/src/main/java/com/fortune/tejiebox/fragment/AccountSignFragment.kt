package com.fortune.tejiebox.fragment

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.utils.ToastUtils
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.fragment_account_sign.view.*
import java.util.concurrent.TimeUnit

class AccountSignFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = AccountSignFragment()
    }

    private var mView: View? = null
    private var signPassIsShow = false
    private var reSignPassIsShow = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_account_sign, container, false)
        initView()
        return mView
    }

    private fun initView() {
        mView?.iv_account_sign_back?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    Login4AccountActivity.getInstance()?.switchFragment(0)
                }
        }

        mView?.et_account_sign_account?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        ToastUtils.show("注册账号不得超过16位字符")
                        et.setText(it.substring(0, it.length - 1))
                        et.setSelection(it.length - 1)
                    }
                }
        }

        mView?.et_account_sign_pass?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        ToastUtils.show("注册密码不得超过16位字符")
                        et.setText(it.substring(0, it.length - 1))
                        et.setSelection(it.length - 1)
                    }
                }
        }

        mView?.et_account_sign_rePass?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        ToastUtils.show("确认密码不得超过16位字符")
                        et.setText(it.substring(0, it.length - 1))
                        et.setSelection(it.length - 1)
                    }
                }
        }

        mView?.iv_account_sign_pass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    signPassIsShow = !signPassIsShow
                    mView?.et_account_sign_pass?.let {
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

        mView?.iv_account_sign_rePass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    reSignPassIsShow = !reSignPassIsShow
                    mView?.et_account_sign_rePass?.let {
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

        mView?.iv_account_sign_title?.let {
            it.setImageResource(if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.app_title2 else R.mipmap.app_title)
        }

        mView?.tv_account_sign_login?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    toSignCheck()
                }
        }
    }

    /**
     * 注册检查
     */
    private fun toSignCheck() {
        val account = mView?.et_account_sign_account?.text.toString().trim()
        val pass = mView?.et_account_sign_pass?.text.toString().trim()
        val rePass = mView?.et_account_sign_rePass?.text.toString().trim()
        when {
            account.length < 8 -> {
                ToastUtils.show("注册账号长度不足8位字符")
            }
            pass.length < 8 -> {
                ToastUtils.show("注册密码长度不足8位字符")
            }
            rePass != pass -> {
                ToastUtils.show("两次输入的注册密码不一致")
            }
            else -> {
                toSign(account, pass)
            }
        }
    }

    /**
     * 注册
     */
    private fun toSign(account: String, pass: String) {
    }
}