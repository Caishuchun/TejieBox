package com.fortune.tejiebox.activity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.GiftShowPoint
import com.fortune.tejiebox.event.GiftShowState
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.event.RedPointChange
import com.fortune.tejiebox.fragment.DailyCheckFragment
import com.fortune.tejiebox.fragment.InviteGiftFragment
import com.fortune.tejiebox.fragment.WhitePiaoFragment
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.GuideItem
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_gift.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class GiftActivity : BaseActivity() {

    private var currentFragment: Fragment? = null
    private var dailyCheckFragment: DailyCheckFragment? = null
    private var whitePiaoFragment: WhitePiaoFragment? = null
    private var inviteGiftFragment: InviteGiftFragment? = null
    private var getIntegralObservable: Disposable? = null
    private var canGetIntegralObservable: Disposable? = null
    var isFirstCreate = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: GiftActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val NEED_SHOW_GUIDE = "need_show_guide"
    }

    private var needShowGuide = false

    override fun getLayoutId() = R.layout.activity_gift

    override fun doSomething() {
        instance = this
        isFirstCreate = true
        StatusBarUtils.setTextDark(this, true)
        EventBus.getDefault().register(this)
        dailyCheckFragment = DailyCheckFragment.newInstance()

        needShowGuide = intent.getBooleanExtra(NEED_SHOW_GUIDE, false)
        initView()
        getIntegral()
        toCheckCanGetIntegral()
    }

    //为了不保存Fragment,直接清掉
    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
    }

    /**
     * 获取现有积分
     */
    @SuppressLint("SetTextI18n")
    private fun getIntegral() {
//        DialogUtils.showBeautifulDialog(this)
        val getIntegral = RetrofitUtils.builder().getIntegral()
        getIntegralObservable = getIntegral.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
//                DialogUtils.dismissLoading()
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.INTEGRAL, it.data.integral)
                        if (BaseAppUpdateSetting.isToAuditVersion) {
                            tv_gift_integral.text = it.data.integral.toString()
                        } else {
                            tv_gift_integral.text = "${it.data.integral / 10}元"
                        }
                        EventBus.getDefault().postSticky(IntegralChange(it.data.integral))
                    }

                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(this)
                    }

                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
//                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        if (BaseAppUpdateSetting.isToAuditVersion) {
            tv_gift_title.text = "特戒积分"
            tv_gift_integralTitle.text = "我的积分:"
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.fl_gift, dailyCheckFragment!!)
            .commit()

        RxView.clicks(iv_gift_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        tt_gift.setCurrentItem(0)
        toChangeFragment(0)
        tt_gift.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index)
            }
        })

        tv_gift_tips.text =
            "进入任一游戏详情页 --> 点击\"免费充值\" --> 选择区服角色 --> 选择充值额度 --> 点击\"确认充值\" --> 充值成功"

        RxView.clicks(tv_gift_tip2)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                DialogUtils.showDefaultDialog(
                    this, "规则说明",
                    """
                        1. 余额有效期30天，领取后超过30天不使用就会自动作废清除；使用余额时会优先使用最早领取的余额。
                        2. 同一个手机多个账号，每天只有一个账号可以白嫖。
                        3. 邀请好友时，多个账号同个手机只算邀请成功一次。
                        4. 被邀请的好友，每玩1个小时的游戏，可以获得2元，最多10元。
                    """.trimIndent(),
                    null, "确定", null
                )
            }
    }

    private fun toChangeFragment(index: Int) {
        hideAll()
        when (index) {
            0 -> {
                currentFragment = dailyCheckFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }

            1 -> {
                if (whitePiaoFragment == null) {
                    whitePiaoFragment = WhitePiaoFragment.newInstance()
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_gift, whitePiaoFragment!!)
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .show(whitePiaoFragment!!)
                        .commit()
                }
                currentFragment = whitePiaoFragment
            }

            2 -> {
                if (inviteGiftFragment == null) {
                    inviteGiftFragment = InviteGiftFragment.newInstance()
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_gift, inviteGiftFragment!!)
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .show(inviteGiftFragment!!)
                        .commit()
                }
                currentFragment = inviteGiftFragment
            }
        }
    }

    private fun hideAll() {
        supportFragmentManager.beginTransaction()
            .hide(dailyCheckFragment!!)
            .hide(whitePiaoFragment ?: dailyCheckFragment!!)
            .hide(inviteGiftFragment ?: dailyCheckFragment!!)
            .commit()
    }

    /**
     * 积分增加动画
     */
    @SuppressLint("SetTextI18n")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeIntegral(integralChange: IntegralChange) {
        if (integralChange == null) {
            return
        }
        if (isFirstCreate) {
            isFirstCreate = false
            return
        }
        if (integralChange.integral > 0) {
            val oldIntegral = if (BaseAppUpdateSetting.isToAuditVersion) {
                tv_gift_integral.text.toString().trim().toInt()
            } else {
                tv_gift_integral.text.toString().trim().replace("元", "").toInt() * 10
            }
            val newIntegral = integralChange.integral
            SPUtils.putValue(SPArgument.INTEGRAL, newIntegral)
            if (oldIntegral == newIntegral) {
                return
            }
            val animator = ValueAnimator.ofInt(oldIntegral, newIntegral)
            animator.duration = 500
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (BaseAppUpdateSetting.isToAuditVersion) {
                    tv_gift_integral.text = "${animator.animatedValue.toString().toInt()}"
                } else {
                    tv_gift_integral.text = "${animator.animatedValue.toString().toInt() / 10}元"
                }
            }
            animator.start()
        }
    }

    @Subscribe(sticky = true)
    fun showPoint(giftShowPoint: GiftShowPoint) {
        if (giftShowPoint == null) {
            return
        }
        when (giftShowPoint.isShowDailyCheck) {
            GiftShowState.SHOW -> tt_gift.setDailyCheckPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setDailyCheckPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
        when (giftShowPoint.isShowWhitePiao) {
            GiftShowState.SHOW -> tt_gift.setWhitePiaoPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setWhitePiaoPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
        when (giftShowPoint.isShowInviteGift) {
            GiftShowState.SHOW -> tt_gift.setInviteGiftPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setInviteGiftPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
    }

    /**
     * 检查是否能够领取奖励
     */
    private fun toCheckCanGetIntegral() {
        val canGetIntegral = RetrofitUtils.builder().canGetIntegral()
        canGetIntegralObservable = canGetIntegral.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            val data = it.data
                            if (null != data) {
                                if (data.daily_clock_in == 1 || data.limit_time == 1 || data.invite == 1) {
                                    EventBus.getDefault().postSticky(RedPointChange(true))
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            if (data.daily_clock_in == 1) GiftShowState.SHOW else GiftShowState.UN_SHOW,
                                            if (data.limit_time == 1) GiftShowState.SHOW else GiftShowState.UN_SHOW,
                                            if (data.invite == 1) GiftShowState.SHOW else GiftShowState.UN_SHOW
                                        )
                                    )
                                } else {
                                    EventBus.getDefault().postSticky(RedPointChange(false))
                                }
                            }
                            toShowTitleGuide(0)
                        }

                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }

                        else -> {
                            ToastUtils.show(it.msg)
                            finish()
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                toShowTitleGuide(0)
            })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!canBack) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private var canBack = true

    /**
     * 显示遮罩引导层
     */
    private fun toShowTitleGuide(index: Int) {
        if (!needShowGuide) {
            return
        }
        if (index >= 3) {
            canBack = true
            return
        }
        canBack = false

        val layout = when (index) {
            0 -> R.layout.layout_guide_left_top
            1 -> R.layout.layout_guide_center_top
            else -> R.layout.layout_guide_right_top
        }

        val content = when (index) {
            0 -> "点击此处进入签到活动"
            1 -> "点击此处进入白嫖活动"
            else -> "点击此处进入邀请活动"
        }

        GuideUtils.showGuide(
            activity = this,
            backgroundColor = Color.parseColor("#88000000"),
            highLightView = tt_gift.getCurrentItem(index),
            highLightShape = GuideItem.SHAPE_RECT,
            guideLayout = layout,
            guideLayoutGravity = Gravity.BOTTOM,
            guideViewOffsetProvider = { point, rectF, view ->
                when (index) {
                    0 -> {
                        point.offset(-rectF.width().toInt() / 2, 0)
                    }

                    1 -> {
                        point.offset(((rectF.width() - view.width) / 2).toInt(), 0)
                    }

                    else -> {
                        point.offset(-(rectF.width() * 1.8).toInt(), 0)
                    }
                }
            },
            guideViewAttachedListener = { view, controller ->
                view.findViewById<TextView>(R.id.tv_guide_msg).text = content
                view.setOnClickListener {
                    controller.dismiss()
                    toShowItemGuide(index)
                }
            },
            highLightClickListener = { controller ->
                controller.dismiss()
                toShowItemGuide(index)
            },
            guideShowListener = { isShowing -> },
            drawHighLightCallback = { canvas, rect, paint ->
                canvas.drawRoundRect(rect, 30f, 30f, paint)
            }
        )
    }

    /**
     * 显示遮罩引导层
     */
    private fun toShowItemGuide(index: Int) {
        if (index >= 3) {
            canBack = true
            return
        }

        val layout = when (index) {
            0 -> R.layout.layout_guide_left_top
            1 -> R.layout.layout_guide_center_bottom
            else -> R.layout.layout_guide_center_bottom
        }

        val content = when (index) {
            0 -> "点击此处签到"
            1 -> "点击此处白嫖"
            else -> "点击此处邀请好友得奖励"
        }

        val highLightView = when (index) {
            0 -> dailyCheckFragment?.getCanGetItem()
            1 -> whitePiaoFragment?.getCanGetItem()
            else -> inviteGiftFragment?.getShareBtn()
        } ?: return

        val gravity = when (index) {
            0 -> Gravity.BOTTOM
            1 -> Gravity.TOP
            else -> Gravity.TOP
        }

        GuideUtils.showGuide(
            activity = this,
            backgroundColor = Color.parseColor("#88000000"),
            highLightView = highLightView,
            highLightShape = GuideItem.SHAPE_RECT,
            guideLayout = layout,
            guideLayoutGravity = gravity,
            guideViewOffsetProvider = { point, rectF, view ->
                when (index) {
                    0 -> {
                        point.offset(-(rectF.width().toInt()) / 2, 0)
                    }

                    1 -> {
                        point.offset(((rectF.width() - view.width) / 2).toInt(), 0)
                    }

                    else -> {
                        point.offset(((rectF.width() - view.width) / 2).toInt(), 0)
                    }
                }
            },
            guideViewAttachedListener = { view, controller ->
                view.findViewById<TextView>(R.id.tv_guide_msg).text = content
                view.setOnClickListener {
                    controller.dismiss()
                    if (index < 2) {
                        tt_gift.setCurrentItem(index + 1)
                        toShowTitleGuide(index + 1)
                    } else {
                        canBack = true
                    }
                }
            },
            highLightClickListener = { controller ->
                controller.dismiss()
                if (index < 2) {
                    tt_gift.setCurrentItem(index + 1)
                    toShowTitleGuide(index + 1)
                }else {
                    canBack = true
                }
            },
            guideShowListener = { isShowing -> },
            drawHighLightCallback = { canvas, rect, paint ->
                canvas.drawRoundRect(rect, 30f, 30f, paint)
            }
        )
    }


    override fun destroy() {
        EventBus.getDefault().unregister(this)
        getIntegralObservable?.dispose()
        getIntegralObservable = null

        canGetIntegralObservable?.dispose()
        canGetIntegralObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            for (fragment in fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}