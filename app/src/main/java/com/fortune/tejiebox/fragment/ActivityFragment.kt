package com.fortune.tejiebox.fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.GiftShowPoint
import com.fortune.tejiebox.event.GiftShowState
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.event.RedPointChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.utils.ActivityManager
import com.fortune.tejiebox.utils.DialogUtils
import com.fortune.tejiebox.utils.GuideUtils
import com.fortune.tejiebox.utils.HttpExceptionUtils
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.fortune.tejiebox.widget.GuideItem
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_activity.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

/**
 * 活动页面
 */

private const val IS_NEED_SHOW_GUIDE = "isNeedShowGuide"

class ActivityFragment : Fragment() {

    private var isNeedShowGuide = false
    private var mView: View? = null
    private var dailyCheckFragment: DailyCheckFragment? = null
    private var whitePiaoFragment: WhitePiaoFragment? = null
    private var inviteGiftFragment: InviteGiftFragment? = null
    private var currentFragment: Fragment? = null

    private var getIntegralObservable: Disposable? = null
    private var canGetIntegralObservable: Disposable? = null

    companion object {
        @JvmStatic
        fun newInstance(isNeedShowGuide: Boolean) = ActivityFragment().apply {
            arguments = Bundle().apply {
                putBoolean(IS_NEED_SHOW_GUIDE, isNeedShowGuide)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isNeedShowGuide = it.getBoolean(IS_NEED_SHOW_GUIDE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        EventBus.getDefault().register(this)
        mView = inflater.inflate(R.layout.fragment_activity, container, false)
        isNeedShowGuide = SPUtils.getBoolean(SPArgument.IS_NEED_SHOW_GUIDE)
        if (isNeedShowGuide) {
            SPUtils.putValue(SPArgument.IS_NEED_SHOW_GUIDE, false)
        }
        initView()
        getIntegral()
        toCheckCanGetIntegral()
        return mView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        LogUtils.d("+++++++++++++++ActivityFragment_onHiddenChanged:$hidden")
        if(!hidden){
            getIntegral()
            toCheckCanGetIntegral()
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        dailyCheckFragment = DailyCheckFragment.newInstance()

        if (BaseAppUpdateSetting.isToAuditVersion) {
            mView?.tv_activity_title?.text = "特戒积分"
            mView?.tv_activity_integralTitle?.text = "我的积分:"
        }

        childFragmentManager.beginTransaction()
            .add(R.id.fl_activity, dailyCheckFragment!!)
            .commit()

        mView?.tt_activity?.setCurrentItem(0)
        toChangeFragment(0)
        mView?.tt_activity?.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index)
            }
        })

        mView?.tv_activity_tips?.text =
            "进入任一游戏详情页 --> 点击\"免费充值\" --> 选择区服角色 --> 选择充值额度 --> 点击\"确认充值\" --> 充值成功"

        RxView.clicks(mView?.tv_activity_tip2!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                DialogUtils.showDefaultDialog(
                    requireContext(), "规则说明",
                    """
                        1. 余额有效期30天，领取后超过30天不使用就会自动作废清除；使用余额时会优先使用最早领取的余额。
                        2. 同一个手机多个账号，每天只有一个账号可以白嫖。
                        3. 邀请好友时，多个账号同个手机只算邀请成功一次。
                        4. 被邀请的好友，每玩1个小时的游戏，可以获得2元，最多10元。
                    """.trimIndent(),
                    null, "确定", null, Gravity.START
                )
            }
    }

    private fun toChangeFragment(index: Int) {
        hideAll()
        when (index) {
            0 -> {
                currentFragment = dailyCheckFragment
                childFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }

            1 -> {
                if (whitePiaoFragment == null) {
                    whitePiaoFragment = WhitePiaoFragment.newInstance()
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_activity, whitePiaoFragment!!)
                        .commit()
                } else {
                    childFragmentManager.beginTransaction()
                        .show(whitePiaoFragment!!)
                        .commit()
                }
                currentFragment = whitePiaoFragment
            }

            2 -> {
                if (inviteGiftFragment == null) {
                    inviteGiftFragment = InviteGiftFragment.newInstance()
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_activity, inviteGiftFragment!!)
                        .commit()
                } else {
                    childFragmentManager.beginTransaction()
                        .show(inviteGiftFragment!!)
                        .commit()
                }
                currentFragment = inviteGiftFragment
            }
        }
    }

    private fun hideAll() {
        childFragmentManager.beginTransaction()
            .hide(dailyCheckFragment!!)
            .hide(whitePiaoFragment ?: dailyCheckFragment!!)
            .hide(inviteGiftFragment ?: dailyCheckFragment!!)
            .commit()
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
                            mView?.tv_activity_integral?.text = it.data.integral.toString()
                        } else {
                            mView?.tv_activity_integral?.text = "${it.data.integral / 10}元"
                        }
                        EventBus.getDefault().postSticky(IntegralChange(it.data.integral))
                    }

                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(requireActivity())
                    }

                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
//                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
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
                            ActivityManager.toSplashActivity(requireActivity())
                        }

                        else -> {
                            ToastUtils.show(it.msg)
                            MainActivity.getInstance()?.toMainFragment()
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                toShowTitleGuide(0)
            })
    }

    /**
     * 显示遮罩引导层
     */
    private fun toShowTitleGuide(index: Int) {
        if (!isNeedShowGuide) {
            return
        }

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
            activity = requireActivity(),
            backgroundColor = Color.parseColor("#88000000"),
            highLightView = mView?.tt_activity?.getCurrentItem(index)!!,
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
            activity = requireActivity(),
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
                        mView?.tt_activity?.setCurrentItem(index + 1)
                        toShowTitleGuide(index + 1)
                    } else {
                        GuideUtils.showGuideOverDialog(
                            requireContext(),
                            object : GuideUtils.OnGuideOverCallback {
                                override fun over() {
                                    mView?.tt_activity?.setCurrentItem(0)
                                }
                            })
                    }
                }
            },
            highLightClickListener = { controller ->
                controller.dismiss()
                if (index < 2) {
                    mView?.tt_activity?.setCurrentItem(index + 1)
                    toShowTitleGuide(index + 1)
                } else {
                    GuideUtils.showGuideOverDialog(
                        requireContext(),
                        object : GuideUtils.OnGuideOverCallback {
                            override fun over() {
                                mView?.tt_activity?.setCurrentItem(0)
                            }
                        })
                }
            },
            guideShowListener = { isShowing -> },
            drawHighLightCallback = { canvas, rect, paint ->
                canvas.drawRoundRect(rect, 30f, 30f, paint)
            }
        )
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
        if (integralChange.integral > 0) {
            val oldIntegral = if (BaseAppUpdateSetting.isToAuditVersion) {
                mView?.tv_activity_integral?.text.toString().trim().toInt()
            } else {
                mView?.tv_activity_integral?.text.toString().trim().replace("元", "").toInt() * 10
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
                    mView?.tv_activity_integral?.text =
                        "${animator.animatedValue.toString().toInt()}"
                } else {
                    mView?.tv_activity_integral?.text =
                        "${animator.animatedValue.toString().toInt() / 10}元"
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
            GiftShowState.SHOW -> mView?.tt_activity?.setDailyCheckPoint(true)
            GiftShowState.UN_SHOW -> mView?.tt_activity?.setDailyCheckPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
        when (giftShowPoint.isShowWhitePiao) {
            GiftShowState.SHOW -> mView?.tt_activity?.setWhitePiaoPoint(true)
            GiftShowState.UN_SHOW -> mView?.tt_activity?.setWhitePiaoPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
        when (giftShowPoint.isShowInviteGift) {
            GiftShowState.SHOW -> mView?.tt_activity?.setInviteGiftPoint(true)
            GiftShowState.UN_SHOW -> mView?.tt_activity?.setInviteGiftPoint(false)
            GiftShowState.USELESS -> {
                //啥也不干就可以
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)

        getIntegralObservable?.dispose()
        getIntegralObservable = null

        canGetIntegralObservable?.dispose()
        canGetIntegralObservable = null
        super.onDestroy()
    }
}