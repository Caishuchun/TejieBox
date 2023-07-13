package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.bean.RoleListBean
import com.fortune.tejiebox.fragment.IntegralFragment
import com.fortune.tejiebox.fragment.RoleFragment
import com.fortune.tejiebox.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_integral.*
import java.util.concurrent.TimeUnit

class IntegralActivity : BaseActivity() {

    private var gameId = 0
    private var gameIcon: String = ""
    private var gameName: String = ""
    private var gameChannelId: String = ""

    private var currentFragment: Fragment? = null
    private var integralFragment: IntegralFragment? = null
    private var roleFragment: RoleFragment? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: IntegralActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val GAME_ID = "game_id"
        const val GAME_ICON = "game_icon"
        const val GAME_NAME = "game_name"
        const val GAME_CHANNEL_ID = "game_channelId"
    }

    override fun getLayoutId() = R.layout.activity_integral

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)
        gameId = intent.getIntExtra(GAME_ID, 0)
        gameIcon = intent.getStringExtra(GAME_ICON)!!
        gameName = intent.getStringExtra(GAME_NAME)!!
        gameChannelId = intent.getStringExtra(GAME_CHANNEL_ID)!!

        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_integral_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentFragment == integralFragment) {
                    finish()
                } else {
                    toIntegralFragment(null, null)
                }
            }

        integralFragment = IntegralFragment.newInstance(gameIcon, gameName, gameChannelId)
        currentFragment = integralFragment
        supportFragmentManager.beginTransaction()
            .add(R.id.fl_integral, integralFragment!!)
            .commit()
    }

    /**
     * 跳转到选择角色界面
     */
    fun toRoleFragment() {
        if (roleFragment == null) {
            roleFragment = RoleFragment.newInstance(gameId)
            currentFragment = roleFragment
            supportFragmentManager.beginTransaction()
                .hide(integralFragment!!)
                .add(R.id.fl_integral, roleFragment!!)
                .commit()
        } else {
            currentFragment = roleFragment
            supportFragmentManager.beginTransaction()
                .hide(integralFragment!!)
                .show(roleFragment!!)
                .commit()
        }
    }

    /**
     * 跳转到积分兑换界面
     */
    fun toIntegralFragment(role: RoleListBean.Data.Role?, gameVersion: String?) {
        currentFragment = integralFragment
        supportFragmentManager.beginTransaction()
            .hide(roleFragment!!)
            .show(integralFragment!!)
            .commit()
        if (role != null) {
            integralFragment?.setRoleInfo(role, gameVersion!!)
        }
    }

    override fun onBackPressed() {
        if (currentFragment == integralFragment) {
            super.onBackPressed()
        } else {
            toIntegralFragment(null, null)
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