package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.utils.SPUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_shade.*
import java.util.concurrent.TimeUnit

class ShadeActivity : BaseActivity() {

    private var currentTips = 1

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ShadeActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_shade

    @SuppressLint("CheckResult")
    override fun doSomething() {
        toChangeView()
        RxView.clicks(tv_shade_btn)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentTips == 4) {
                    SPUtils.putValue(SPArgument.IS_SHOW_SHADE, false)
                    finish()
                } else {
                    currentTips++
                    toChangeView()
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun toChangeView() {
        when (currentTips) {
            1 -> {
                tv_shade_tips.text = "点击消耗1颗能量星,可炸掉年兽1000血量"
                rl_shade_fireworks1.visibility = View.VISIBLE
                rl_shade_fireworks2.visibility = View.INVISIBLE
                rl_shade_fireworks3.visibility = View.INVISIBLE
                tv_shade_getStar.visibility = View.INVISIBLE
            }
            2 -> {
                tv_shade_tips.text = "点击消耗5颗能量星,可炸掉年兽5000血量"
                rl_shade_fireworks1.visibility = View.INVISIBLE
                rl_shade_fireworks2.visibility = View.VISIBLE
                rl_shade_fireworks3.visibility = View.INVISIBLE
                tv_shade_getStar.visibility = View.INVISIBLE
            }
            3 -> {
                tv_shade_tips.text = "点击消耗10颗能量星,可炸掉年兽10000血量"
                rl_shade_fireworks1.visibility = View.INVISIBLE
                rl_shade_fireworks2.visibility = View.INVISIBLE
                rl_shade_fireworks3.visibility = View.VISIBLE
                tv_shade_getStar.visibility = View.INVISIBLE
            }
            4 -> {
                tv_shade_tips.text = "点击可以查看如何获取能量星"
                tv_shade_btn.text = "完成"
                rl_shade_fireworks1.visibility = View.INVISIBLE
                rl_shade_fireworks2.visibility = View.INVISIBLE
                rl_shade_fireworks3.visibility = View.INVISIBLE
                tv_shade_getStar.visibility = View.VISIBLE
            }
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
