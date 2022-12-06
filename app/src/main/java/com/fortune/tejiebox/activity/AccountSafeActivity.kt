package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.StatusBarUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_account_safe.*
import java.util.concurrent.TimeUnit

class AccountSafeActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: AccountSafeActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_account_safe

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)
        initView()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        RxView.clicks(iv_accountSafe_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        val phone = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
        if (phone.isNullOrBlank()) {
            tv_accountSafe_phone.text = "未绑定"
            tv_accountSafe_phone.setTextColor(Color.parseColor("#FF982E"))
        } else {
            tv_accountSafe_phone.text = "${phone.substring(0, 3)}****${phone.substring(7)}"
            tv_accountSafe_phone.setTextColor(Color.parseColor("#5F60FF"))
        }

        val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT, null)
        if (account.isNullOrBlank()) {
            tv_accountSafe_account.text = "未添加"
            tv_accountSafe_account.setTextColor(Color.parseColor("#FF982E"))
        } else {
            tv_accountSafe_account.text = "${account.substring(0, 3)}****${account.substring(7)}"
            tv_accountSafe_account.setTextColor(Color.parseColor("#5F60FF"))
        }

        RxView.clicks(ll_accountSafe_phone)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val phone4Current = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
                if (phone4Current.isNullOrBlank()) {
                    val intent = Intent(this, ChangePhone1Activity::class.java)
                    intent.putExtra(ChangePhone1Activity.IS_BIND, true)
                    startActivity(intent)
                }
            }

        RxView.clicks(ll_accountSafe_account)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                startActivity(Intent(this, AccountBindActivity::class.java))
            }

        RxView.clicks(ll_accountSafe_changePhone)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val phone4Current = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
                if (phone4Current.isNullOrBlank()) {
                    ToastUtils.show("暂未绑定手机号,无法修改...")
                } else {
                    startActivity(Intent(this, ChangePhone1Activity::class.java))
                }
            }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        val phone = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
        if (phone.isNullOrBlank()) {
            tv_accountSafe_phone.text = "未绑定"
            tv_accountSafe_phone.setTextColor(Color.parseColor("#FF982E"))
        } else {
            tv_accountSafe_phone.text = "${phone.substring(0, 3)}****${phone.substring(7)}"
            tv_accountSafe_phone.setTextColor(Color.parseColor("#5F60FF"))
        }
        val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT, null)
        if (account.isNullOrBlank()) {
            tv_accountSafe_account.text = "未添加"
            tv_accountSafe_account.setTextColor(Color.parseColor("#FF982E"))
        } else {
            tv_accountSafe_account.text = "${account.substring(0, 3)}****${account.substring(7)}"
            tv_accountSafe_account.setTextColor(Color.parseColor("#5F60FF"))
        }
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun destroy() {
    }
}