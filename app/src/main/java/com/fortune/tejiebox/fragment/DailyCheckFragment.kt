package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.AccountSafeActivity
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.GiftActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.DailyCheckListBean
import com.fortune.tejiebox.bean.RedPointBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.GiftNeedNewInfo
import com.fortune.tejiebox.event.GiftShowPoint
import com.fortune.tejiebox.event.GiftShowState
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_daily_check.view.*
import kotlinx.android.synthetic.main.item_daily_check.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit

class DailyCheckFragment : Fragment() {

    private var mView: View? = null
    private var adapter: BaseAdapterWithPosition<DailyCheckListBean.DataBean.ListBean>? = null
    private var dailyCheckListObservable: Disposable? = null
    private var dailyCheckObservable: Disposable? = null
    private var mData = mutableListOf<DailyCheckListBean.DataBean.ListBean>()
    private var canClickPosition = 0
    private var isTodayGet = false //今天是否领取了

    companion object {
        fun newInstance() = DailyCheckFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_daily_check, container, false)
        initView()
        getData()
        return mView
    }

    private fun getData() {
//        DialogUtils.showBeautifulDialog(requireContext())
        val dailyCheckList = RetrofitUtils.builder().dailyCheckList()
        dailyCheckListObservable = dailyCheckList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()!!.list != null) {
                                if (it.getData()?.is_clock_in == 0) {
                                    isTodayGet = false
                                    //没签到,小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.SHOW,
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS
                                        )
                                    )
                                } else {
                                    isTodayGet = true
                                    //签到,去掉小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.UN_SHOW,
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS
                                        )
                                    )
                                }
                                if (it.getData()!!.list != null) {
                                    mData.clear()
                                    for (data in it.getData()!!.list!!) {
                                        data.let { dataInfo -> mData.add(dataInfo) }
                                    }
                                }
                                if (mData.size > 0) {
                                    for (index in mData.indices) {
                                        if (mData[index].status == 0) {
                                            canClickPosition = index
                                            break
                                        }
                                    }
                                }
                            }
                            val scrollToPosition = if (canClickPosition < 4) {
                                0
                            } else {
                                val i = canClickPosition / 4
                                i * 4
                            }
                            mView?.rv_gift_dailyCheck?.scrollToPosition(scrollToPosition)
                            LogUtils.d("canClickPosition:$canClickPosition")
                            adapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            (requireActivity()).finish()
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    (requireActivity()).finish()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                (requireActivity()).finish()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        adapter = BaseAdapterWithPosition.Builder<DailyCheckListBean.DataBean.ListBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_daily_check)
            .addBindView { itemView, itemData, position ->
                val scaleAnimation = ScaleAnimation(
                    1f,
                    1.2f,
                    1f,
                    1.2f,
                    PhoneInfoUtils.getWidth(requireActivity()) / 360f * 30f / 2,
                    PhoneInfoUtils.getWidth(requireActivity()) / 360f * 30f / 2
                )
                scaleAnimation.duration = 500
                scaleAnimation.repeatMode = ScaleAnimation.REVERSE
                scaleAnimation.repeatCount = Int.MAX_VALUE

                itemView.rl_item_dailyCheck_bg.setBackgroundResource(
                    if (itemData.status == 0) R.drawable.bg_daily_checkable
                    else R.drawable.bg_daily_checked
                )

                if (BaseAppUpdateSetting.isToAuditVersion) {
                    itemView.tv_item_dailyCheck_title.text =
                        if (itemData.status != 0) {
                            if (position == canClickPosition - 1) {
                                if (!isTodayGet) "已领取" else "今日已领取"
                            } else "已领取"
                        } else "第${position + 1}天"
                } else {
                    itemView.tv_item_dailyCheck_title.text =
                        if (itemData.status != 0) {
                            if (position == canClickPosition - 1) {
                                if (!isTodayGet) "已白嫖" else "今日已白嫖"
                            } else "已白嫖"
                        } else "第${position + 1}天"
                }

                itemView.iv_item_dailyCheck_type.setImageResource(
                    if (itemData.status == 0) R.mipmap.money else R.mipmap.money_ed
                )
                itemView.tv_item_dailyCheck_num.text =
                    if (BaseAppUpdateSetting.isToAuditVersion) "+${itemData.num}"
                    else "+${itemData.num?.div(10)}元"
                itemView.tv_item_dailyCheck_num.setTextColor(
                    if (itemData.status == 0) resources.getColor(R.color.orange_FF9C00)
                    else resources.getColor(R.color.gray_C4C4C4)
                )

                if (position == canClickPosition && !isTodayGet) {
                    //今天的没有领取的话,就开始布灵布灵的
                    itemView.tv_item_dailyCheck_title.text =
                        if (BaseAppUpdateSetting.isToAuditVersion) "今日可领取" else "今日可白嫖"
                    itemView.iv_item_dailyCheck_type.startAnimation(scaleAnimation)
                    itemView.rl_item_dailyCheck_bg.setBackgroundResource(R.drawable.bg_daily_checking)
                }

                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.status == 0 && position == canClickPosition && !isTodayGet) {
                            val phone = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
                            if (phone.isNullOrBlank()) {
                                DialogUtils.showDefaultDialog(
                                    requireContext(),
                                    "未绑定手机号",
                                    "需要绑定手机号才能领取奖励",
                                    "暂不绑定",
                                    "立即绑定",
                                    object : DialogUtils.OnDialogListener {
                                        override fun next() {
                                            startActivity(
                                                Intent(
                                                    requireContext(),
                                                    AccountSafeActivity::class.java
                                                )
                                            )
                                        }
                                    }
                                )
                            } else {
                                toAddExperienceAndIntegral(itemData.num!!, itemView)
                            }
                        }
                    }
            }
            .create()

        mView?.rv_gift_dailyCheck?.adapter = adapter
        mView?.rv_gift_dailyCheck?.setItemViewCacheSize(32)
        mView?.rv_gift_dailyCheck?.layoutManager =
            SafeStaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
    }

    /**
     * 签到获取经验或者积分
     */
    private fun toAddExperienceAndIntegral(num: Int, itemView: View) {
        DialogUtils.showBeautifulDialog(requireContext())
        val dailyCheck = RetrofitUtils.builder().dailyCheck()
        dailyCheckObservable = dailyCheck.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            itemView.iv_item_dailyCheck_type.clearAnimation()
                            itemView.setBackgroundResource(R.drawable.bg_daily_checked)
                            itemView.tv_item_dailyCheck_title.text =
                                if (BaseAppUpdateSetting.isToAuditVersion) "今日已领取"
                                else "今日已白嫖"
                            itemView.tv_item_dailyCheck_num.setTextColor(resources.getColor(R.color.gray_C4C4C4))
                            itemView.iv_item_dailyCheck_type.setImageResource(R.mipmap.money_ed)

                            //当签到成功时候,需要把小红点去掉
                            if (RedPointBean.getData() != null) {
                                val data = RedPointBean.getData()!!
                                data.daily_clock_in = 0
                                RedPointBean.setData(data)
                                EventBus.getDefault().postSticky(data)
                            }
                            EventBus.getDefault().postSticky(
                                GiftShowPoint(
                                    GiftShowState.UN_SHOW,
                                    GiftShowState.USELESS,
                                    GiftShowState.USELESS
                                )
                            )

                            (activity as GiftActivity).isFirstCreate = false
                            SPUtils.putValue(SPArgument.INTEGRAL, it.getData()?.user_integral)
                            DialogActivity.showGetIntegral(
                                requireActivity(),
                                num,
                                true,
                                object : DialogActivity.OnCallback {
                                    override fun cancel() {
                                        EventBus.getDefault().postSticky(
                                            IntegralChange(it.getData()?.user_integral!!)
                                        )
                                    }
                                })
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                (requireActivity()).finish()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    @Subscribe
    fun time12NeedNewInfo(giftNeedNewInfo: GiftNeedNewInfo) {
        if (MyApp.getInstance().isHaveToken() && giftNeedNewInfo.isShowDailyCheckNeed) {
            getData()
        }
    }

    override fun onDestroy() {
        dailyCheckListObservable?.dispose()
        dailyCheckListObservable = null

        dailyCheckObservable?.dispose()
        dailyCheckObservable = null
        super.onDestroy()
    }
}