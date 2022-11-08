package com.fortune.tejiebox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.utils.ToastUtils
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_account_sign.view.*
import java.util.concurrent.TimeUnit

class AccountSignFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = AccountSignFragment()
    }

    private var mView: View? = null

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
        if (account.length > 5 && pass.length > 5) {

        } else {
            ToastUtils.show("注册账号或注册密码不符合规定,请重新填写")
        }
    }
}