package com.fortune.tejiebox.activity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.animation.LinearInterpolator
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
    }

    override fun getLayoutId() = R.layout.activity_gift

    override fun doSomething() {
        instance = this
        isFirstCreate = true
        StatusBarUtils.setTextDark(this, true)
        EventBus.getDefault().register(this)
        dailyCheckFragment = DailyCheckFragment.newInstance()
        whitePiaoFragment = WhitePiaoFragment.newInstance()
        inviteGiftFragment = InviteGiftFragment.newInstance()
        initView()
        getIntegral()
    }

    //???????????????Fragment,????????????
    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
    }

    /**
     * ??????????????????
     */
    @SuppressLint("SetTextI18n")
    private fun getIntegral() {
//        DialogUtils.showBeautifulDialog(this)
        val getIntegral = RetrofitUtils.builder().getIntegral()
        getIntegralObservable = getIntegral.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.INTEGRAL, it.data.integral)
                        if (BaseAppUpdateSetting.isToAuditVersion) {
                            tv_gift_integral.text = it.data.integral.toString()
                        } else {
                            tv_gift_integral.text = "${it.data.integral / 10}???"
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
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        if (BaseAppUpdateSetting.isToAuditVersion) {
            tv_gift_title.text = "????????????"
            tv_gift_integralTitle.text = "????????????:"
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.fl_gift, dailyCheckFragment!!)
            .add(R.id.fl_gift, whitePiaoFragment!!)
            .add(R.id.fl_gift, inviteGiftFragment!!)
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

        tv_gift_tips.text = "??????????????????????????? --> ??????\"????????????\" --> ?????????????????? --> ?????????????????? --> ??????\"????????????\" --> ????????????"
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
                currentFragment = whitePiaoFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }
            2 -> {
                currentFragment = inviteGiftFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commit()
            }
        }
    }

    private fun hideAll() {
        supportFragmentManager.beginTransaction()
            .hide(dailyCheckFragment!!)
            .hide(whitePiaoFragment!!)
            .hide(inviteGiftFragment!!)
            .commit()
    }

    /**
     * ??????????????????
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
                tv_gift_integral.text.toString().trim().replace("???", "").toInt() * 10
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
                    tv_gift_integral.text = "${animator.animatedValue.toString().toInt() / 10}???"
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
                //?????????????????????
            }
        }
        when (giftShowPoint.isShowWhitePiao) {
            GiftShowState.SHOW -> tt_gift.setWhitePiaoPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setWhitePiaoPoint(false)
            GiftShowState.USELESS -> {
                //?????????????????????
            }
        }
        when (giftShowPoint.isShowInviteGift) {
            GiftShowState.SHOW -> tt_gift.setInviteGiftPoint(true)
            GiftShowState.UN_SHOW -> tt_gift.setInviteGiftPoint(false)
            GiftShowState.USELESS -> {
                //?????????????????????
            }
        }

        toCheckCanGetIntegral()
    }

    /**
     * ??????????????????????????????
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
                                } else {
                                    EventBus.getDefault().postSticky(RedPointChange(false))
                                }
                            }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
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
}