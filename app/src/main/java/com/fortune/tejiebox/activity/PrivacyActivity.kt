package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_privacy.*
import java.util.concurrent.TimeUnit

class PrivacyActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: PrivacyActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_privacy

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this

        RxView.clicks(iv_privacy_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        RxView.clicks(ll_user_agreement)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(this, WebActivity::class.java)
                intent.putExtra(WebActivity.TYPE, WebActivity.USER_AGREEMENT)
                startActivity(intent)
            }

        RxView.clicks(ll_privacy_agreement)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(this, WebActivity::class.java)
                intent.putExtra(WebActivity.TYPE, WebActivity.PRIVACY_AGREEMENT)
                startActivity(intent)
            }
    }

    override fun destroy() {
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
