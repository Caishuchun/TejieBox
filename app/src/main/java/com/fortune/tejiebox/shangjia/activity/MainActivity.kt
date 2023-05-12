package com.fortune.tejiebox.shangjia.activity

import android.annotation.SuppressLint
import android.graphics.Color
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.fragment.MineFragment
import com.fortune.tejiebox.shangjia.fragment.CommunityFragment
import com.fortune.tejiebox.shangjia.fragment.HomeFragment
import com.fortune.tejiebox.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.activity_main2.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: MainActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

    }

    private var homeFragment: HomeFragment? = null
    private var communityFragment: CommunityFragment? = null
    private var mineFragment: MineFragment? = null

    override fun getLayoutId() = R.layout.activity_main2

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)
        toChangeBottomTabStyle()
        initView()
    }

    /**
     * 初始化布局
     */
    @SuppressLint("CheckResult")
    private fun initView() {
        homeFragment = HomeFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.fl_main_fragment, homeFragment!!)
            .commit()

        RxView.clicks(ll_main_home)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toChangeBottomTabStyle(0)
                supportFragmentManager.beginTransaction()
                    .hide(communityFragment ?: homeFragment!!)
                    .hide(mineFragment ?: homeFragment!!)
                    .show(homeFragment!!)
                    .commit()
            }

        RxView.clicks(ll_main_community)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toChangeBottomTabStyle(1)
                if (communityFragment == null) {
                    communityFragment = CommunityFragment.newInstance()
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_main_fragment, communityFragment!!)
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .hide(homeFragment!!)
                        .hide(mineFragment ?: homeFragment!!)
                        .show(communityFragment!!)
                        .commit()
                }
            }

        RxView.clicks(ll_main_message)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toChangeBottomTabStyle(2)
            }

        RxView.clicks(ll_main_mine)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toChangeBottomTabStyle(3)
                if (mineFragment == null) {
                    mineFragment = MineFragment.newInstance()
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_main_fragment, mineFragment!!)
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .hide(homeFragment!!)
                        .hide(communityFragment ?: homeFragment!!)
                        .show(mineFragment!!)
                        .commit()
                }
            }
    }

    /**
     * 修改底部tab样式
     */
    private fun toChangeBottomTabStyle(index: Int = 0) {
        iv_main_home.setImageResource(R.mipmap.home_unselect)
        iv_main_community.setImageResource(R.mipmap.community_unselect)
        iv_main_message.setImageResource(R.mipmap.message_unselect)
        iv_main_mine.setImageResource(R.mipmap.mine_unselect)

        tv_main_home.setTextColor(Color.parseColor("#999999"))
        tv_main_community.setTextColor(Color.parseColor("#999999"))
        tv_main_message.setTextColor(Color.parseColor("#999999"))
        tv_main_mine.setTextColor(Color.parseColor("#999999"))

        when (index) {
            0 -> {
                iv_main_home.setImageResource(R.mipmap.home_selected)
                tv_main_home.setTextColor(Color.parseColor("#121A28"))
            }
            1 -> {
                iv_main_community.setImageResource(R.mipmap.community_selected)
                tv_main_community.setTextColor(Color.parseColor("#121A28"))
            }
            2 -> {
                iv_main_message.setImageResource(R.mipmap.message_selected)
                tv_main_message.setTextColor(Color.parseColor("#121A28"))
            }
            3 -> {
                iv_main_mine.setImageResource(R.mipmap.mine_selected)
                tv_main_mine.setTextColor(Color.parseColor("#121A28"))
            }
        }
    }

    override fun destroy() {
    }
}