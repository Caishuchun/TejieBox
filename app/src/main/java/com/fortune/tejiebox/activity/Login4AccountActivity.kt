package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.fragment.AccountLoginFragment
import com.fortune.tejiebox.fragment.AccountSignFragment
import com.fortune.tejiebox.utils.StatusBarUtils
import com.umeng.analytics.MobclickAgent

class Login4AccountActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: Login4AccountActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        @SuppressLint("StaticFieldLeak")
        lateinit var accountLoginFragment: AccountLoginFragment
    }

    private var currentIndex = 0
    private var accountSignFragment: AccountSignFragment? = null

    override fun getLayoutId() = R.layout.activity_login4_account

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this

        accountLoginFragment = AccountLoginFragment()
        val supportFragmentManager = supportFragmentManager
        val beginTransaction = supportFragmentManager.beginTransaction()

        beginTransaction
            .add(R.id.fl_login4Account, accountLoginFragment)
            .commit()
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

    /**
     * 切换Fragment
     */
    fun switchFragment(index: Int) {
        val supportFragmentManager = supportFragmentManager
        val beginTransaction = supportFragmentManager.beginTransaction()
        when (index) {
            0 -> {
                currentIndex = 0
                //登录界面
                beginTransaction
                    .replace(R.id.fl_login4Account, accountLoginFragment)
                    .commit()
            }
            1 -> {
                currentIndex = 1
                //注册界面
                accountSignFragment = AccountSignFragment()
                beginTransaction
                    .replace(R.id.fl_login4Account, accountSignFragment!!)
                    .commit()
            }
        }
    }

    override fun onBackPressed() {
        if (currentIndex == 1) {
            currentIndex = 0
            val beginTransaction = supportFragmentManager.beginTransaction()
            beginTransaction.replace(R.id.fl_login4Account, accountLoginFragment)
            beginTransaction.commitAllowingStateLoss()
        } else {
            super.onBackPressed()
        }
    }

}