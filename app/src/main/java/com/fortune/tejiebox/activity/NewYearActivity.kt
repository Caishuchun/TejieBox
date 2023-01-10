package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.fragment.NewYear4InviteFragment
import com.fortune.tejiebox.fragment.NewYear4NianshouFragment
import com.fortune.tejiebox.fragment.NewYear4WhitePiaoFragment
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.utils.PhoneInfoUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_new_year.*
import java.util.concurrent.TimeUnit

class NewYearActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: NewYearActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        @SuppressLint("StaticFieldLeak")
        lateinit var newYear4WhitePiaoFragment: NewYear4WhitePiaoFragment
    }

    private var currentFragment: Fragment? = null
    private var newYear4InviteFragment: NewYear4InviteFragment? = null
    private var newYear4NianshouFragment: NewYear4NianshouFragment? = null

    override fun getLayoutId() = R.layout.activity_new_year

    override fun doSomething() {
        newYear4WhitePiaoFragment = NewYear4WhitePiaoFragment.newInstance()
        currentFragment = newYear4WhitePiaoFragment
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.add(R.id.fl_newYear, newYear4WhitePiaoFragment)
        beginTransaction.commit()
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_newYear_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        top_newYear.setCurrentItem(0)
        toChangeFragment(0, true)

        top_newYear.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index, false)
            }
        })

        changeTopImg()
    }

    /**
     * 修改顶部图片
     */
    private fun changeTopImg() {
        val width = PhoneInfoUtils.getWidth(this)
        val height = PhoneInfoUtils.getHeight(this)
        if (height < width * 1.3f) {
            // 差不离就是个短手机
            val layoutParams = rl_newYear_top.layoutParams
            layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
            layoutParams.height = (64f / 360 * width).toInt()
            rl_newYear_top.layoutParams = layoutParams
            iv_newYear_top.setImageResource(R.mipmap.new_year_top_small)
        } else {
            //正常长手机
        }
    }

    /**
     * 设置活动日期
     */
    @SuppressLint("CheckResult")
    fun setActivityDate(date: String) {
        Observable.timer(100, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                tv_newYear_date.text = date
            }
    }

    /**
     * 跳转白嫖Fragment
     */
    fun toWhitePiaoFragment() {
        top_newYear.setCurrentItem(0)
        toChangeFragment(0, false)
    }

    /**
     * 跳转邀请Fragment
     */
    fun toInviteFragment() {
        top_newYear.setCurrentItem(1)
        toChangeFragment(1, false)
    }

    /**
     * 页面跳转
     */
    private fun toChangeFragment(page: Int, isFirst: Boolean) {
        if (isFirst) {
            return
        }
        clearFragment()
        val beginTransaction = supportFragmentManager.beginTransaction()
        when (page) {
            0 -> {
                newYear4WhitePiaoFragment = NewYear4WhitePiaoFragment.newInstance()
                currentFragment = newYear4WhitePiaoFragment
                beginTransaction.add(R.id.fl_newYear, newYear4WhitePiaoFragment)
                beginTransaction.commit()
            }
            1 -> {
                newYear4InviteFragment = NewYear4InviteFragment.newInstance()
                currentFragment = newYear4InviteFragment
                beginTransaction.add(R.id.fl_newYear, newYear4InviteFragment!!)
                beginTransaction.commit()
            }
            2 -> {
                newYear4NianshouFragment = NewYear4NianshouFragment.newInstance()
                currentFragment = newYear4NianshouFragment
                beginTransaction.add(R.id.fl_newYear, newYear4NianshouFragment!!)
                beginTransaction.commit()
            }
        }
    }

    /**
     * 清空Fragment
     */
    private fun clearFragment() {
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.remove(newYear4WhitePiaoFragment)
        beginTransaction.remove(newYear4InviteFragment ?: newYear4WhitePiaoFragment)
        beginTransaction.remove(newYear4NianshouFragment ?: newYear4WhitePiaoFragment)
        beginTransaction.commit()
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