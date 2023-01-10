package com.fortune.tejiebox.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.layout_top_tab_new_year.view.*
import java.util.concurrent.TimeUnit

/**
 * 礼物积分界面顶部tab
 */
@SuppressLint("CheckResult")
class TopTab4NewYear(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    /**
     * 实现回调接口的方法
     *
     * @param onItemListener 回调接口的实例
     */
    fun setOnItemListener(onItemListener: OnBottomBarItemSelectListener) {
        mOnItemListener = onItemListener
    }

    private var currentIndex = 0
    private var mOnItemListener: OnBottomBarItemSelectListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mView: View
    }

    // 是否有每日白嫖可领取奖励
    private var hasWhitePiao = false

    // 是否有邀请可领取奖励
    private var hasInvite = false

    // 是否有炸年兽可领取奖励
    private var hasNianShou = false

    fun setWhitePiaoPoint(isWhitePiaoPoint: Boolean) {
        hasWhitePiao = isWhitePiaoPoint
        mView.view_newYear_whitePiao.visibility = if (hasWhitePiao) VISIBLE else GONE
    }

    fun setInvitePoint(isShowInvitePoint: Boolean) {
        hasInvite = isShowInvitePoint
        mView.view_newYear_invite.visibility = if (hasInvite) VISIBLE else GONE
    }

    fun setInviteGiftPoint(isShowNianShouPoint: Boolean) {
        hasNianShou = isShowNianShouPoint
        mView.view_newYear_nianShou.visibility = if (hasNianShou) VISIBLE else GONE
    }

    fun setCurrentItem(index: Int) {
        changeItemStyle(index)
    }

    init {
        //获取布局文件
        mView = LayoutInflater.from(context).inflate(R.layout.layout_top_tab_new_year, this, true)

        if (BaseAppUpdateSetting.isToAuditVersion) {
            mView.tv_newYear_whitePiao.text = "每天来领奖"
        }

        RxView.clicks(mView.rl_newYear_whitePiao)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentIndex != 0) {
                    currentIndex = 0
                    changeItemStyle(0)
                }
            }
        RxView.clicks(mView.rl_newYear_invite)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentIndex != 1) {
                    currentIndex = 1
                    changeItemStyle(1)
                }
            }
        RxView.clicks(mView.rl_newYear_nianShou)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentIndex != 2) {
                    currentIndex = 2
                    changeItemStyle(2)
                }
            }
    }

    private fun changeItemStyle(index: Int) {
        mOnItemListener?.setOnItemSelectListener(index)

        mView.tv_newYear_whitePiao.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_newYear_invite.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        mView.tv_newYear_nianShou.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)

        mView.tv_newYear_whitePiao.setTextColor(Color.parseColor("#fde9bc"))
        mView.tv_newYear_invite.setTextColor(Color.parseColor("#fde9bc"))
        mView.tv_newYear_nianShou.setTextColor(Color.parseColor("#fde9bc"))

        mView.rl_newYear_whitePiao.setBackgroundResource(R.mipmap.bg_newyear_title_unselect)
        mView.rl_newYear_invite.setBackgroundResource(R.mipmap.bg_newyear_title_unselect)
        mView.rl_newYear_nianShou.setBackgroundResource(R.mipmap.bg_newyear_title_unselect)

        mView.view_newYear_whitePiao.visibility = if (hasWhitePiao) View.VISIBLE else View.GONE
        mView.view_newYear_invite.visibility = if (hasInvite) View.VISIBLE else View.GONE
        mView.view_newYear_nianShou.visibility = if (hasNianShou) View.VISIBLE else View.GONE

        when (index) {
            0 -> {
                mView.rl_newYear_whitePiao.setBackgroundResource(R.mipmap.bg_newyear_title_select)
                mView.tv_newYear_whitePiao.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.tv_newYear_whitePiao.setTextColor(Color.parseColor("#ee1212"))
            }
            1 -> {
                mView.rl_newYear_invite.setBackgroundResource(R.mipmap.bg_newyear_title_select)
                mView.tv_newYear_invite.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.tv_newYear_invite.setTextColor(Color.parseColor("#ee1212"))
            }
            2 -> {
                mView.rl_newYear_nianShou.setBackgroundResource(R.mipmap.bg_newyear_title_select)
                mView.tv_newYear_nianShou.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                mView.tv_newYear_nianShou.setTextColor(Color.parseColor("#ee1212"))
            }
        }
    }
}