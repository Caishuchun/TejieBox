package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.LoginUtils
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.layout_bottom_tab.view.*
import java.util.concurrent.TimeUnit

/**
 * 首页的底部tab
 */
@SuppressLint("CheckResult")
class BottomTab(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var currentPos = 0

    /**
     * 实现回调接口的方法
     *
     * @param onItemListener 回调接口的实例
     */
    fun setOnItemListener(onItemListener: OnBottomBarItemSelectListener) {
        mOnItemListener = onItemListener
    }

    fun setCurrentItem(index: Int) {
        currentPos = index
        changeItemStyle(index)
    }

    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    /**
     * 小红点的展示与否
     */
    fun showRedPoint(isShow: Boolean) {
        mView.iv_bottomTab_me_point.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_bottom_tab, this, true)
        RxView.clicks(mView.rl_bottomTab_home)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivity.mainPage = MainActivity.MainPage.MAIN
                if (currentPos != 0) {
                    currentPos = 0
                    changeItemStyle(0)
                }
            }
        RxView.clicks(mView.rl_bottomTab_playing)
            .throttleFirst(
                20, TimeUnit.MILLISECONDS
            )
            .subscribe {
                MainActivity.mainPage = MainActivity.MainPage.PLAYING
                if (currentPos != 1) {
                    if (MyApp.getInstance().isHaveToken()) {
                        currentPos = 1
                        changeItemStyle(1)
                    } else {
                        MainActivity.getInstance()?.let {
                            LoginUtils.toQuickLogin(it)
                        }
                    }
                }
            }
        RxView.clicks(mView.rl_bottomTab_like)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivity.mainPage = MainActivity.MainPage.LIKE
                if (currentPos != 2) {
                    if (MyApp.getInstance().isHaveToken()) {
                        currentPos = 2
                        changeItemStyle(2)
                    } else {
                        MainActivity.getInstance()?.let {
                            LoginUtils.toQuickLogin(it)
                        }
                    }
                }
            }
        RxView.clicks(mView.rl_bottomTab_me)
            .throttleFirst(20, TimeUnit.MILLISECONDS)
            .subscribe {
                MainActivity.mainPage = MainActivity.MainPage.ME
                if (currentPos != 3) {
                    currentPos = 3
                    changeItemStyle(3)
                }
            }
    }

    private fun changeItemStyle(index: Int) {
        mOnItemListener?.setOnItemSelectListener(index)

        mView.iv_bottomTab_home.setImageResource(R.mipmap.icon_home_unselect)
        mView.iv_bottomTab_playing.setImageResource(R.mipmap.icon_playing_unselect)
        mView.iv_bottomTab_like.setImageResource(R.mipmap.icon_like_unselect)
        mView.iv_bottomTab_me.setImageResource(R.mipmap.icon_me_unselect)

        mView.tv_bottomTab_home.setTextColor(Color.parseColor("#817F8E"))
        mView.tv_bottomTab_playing.setTextColor(Color.parseColor("#817F8E"))
        mView.tv_bottomTab_like.setTextColor(Color.parseColor("#817F8E"))
        mView.tv_bottomTab_me.setTextColor(Color.parseColor("#817F8E"))

        when (index) {
            0 -> {
                mView.iv_bottomTab_home.setImageResource(R.mipmap.icon_home_selected)
                mView.tv_bottomTab_home.setTextColor(Color.parseColor("#6E6FFF"))
            }
            1 -> {
                mView.iv_bottomTab_playing.setImageResource(R.mipmap.icon_playing_selected)
                mView.tv_bottomTab_playing.setTextColor(Color.parseColor("#6E6FFF"))
            }
            2 -> {
                mView.iv_bottomTab_like.setImageResource(R.mipmap.icon_like_selected)
                mView.tv_bottomTab_like.setTextColor(Color.parseColor("#6E6FFF"))
            }
            3 -> {
                mView.iv_bottomTab_me.setImageResource(R.mipmap.icon_me_selected)
                mView.tv_bottomTab_me.setTextColor(Color.parseColor("#6E6FFF"))
            }
        }
    }
}