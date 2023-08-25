package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.AccountSafeActivity
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.GiftActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.GetShareListBean
import com.fortune.tejiebox.bean.InviteListNewBean
import com.fortune.tejiebox.bean.RedPointBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.GiftNeedNewInfo
import com.fortune.tejiebox.event.GiftShowPoint
import com.fortune.tejiebox.event.GiftShowState
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_invite_gift.tv_inviteGift_share
import kotlinx.android.synthetic.main.fragment_invite_gift.view.*
import kotlinx.android.synthetic.main.item_invite_gift.view.*
import kotlinx.android.synthetic.main.item_invite_time.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit

class InviteGiftFragment : Fragment() {

    private var mView: View? = null
    private var mAdapter: BaseAdapterWithPosition<GetShareListBean.DataBean.ListBean>? = null
    private var mData = mutableListOf<GetShareListBean.DataBean.ListBean>()

    private var isShare = false
    private var getShareListObservable: Disposable? = null
    private var getInviteGiftObservable: Disposable? = null
    private var getShareUrlObservable: Disposable? = null

    private var inviteReward = 0 //邀请奖励
    private var shareReward = 0 //分享奖励等级

    private var canGet = 0 //有多少个可以领取的
    private var isCreateGet = true
    private var wantToShare = false //想要去分享

    private var mAdapter4New: BaseAdapterWithPosition<InviteListNewBean.Data>? = null
    private var mData4New = mutableListOf<InviteListNewBean.Data>()

    private var getInviteListObservable: Disposable? = null
    private var getInviteRechargeObservable: Disposable? = null

    companion object {
        fun newInstance() = InviteGiftFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_invite_gift, container, false)
        initView()
        if (isCreateGet) {
            getShareList()
            isCreateGet = false
        }
        return mView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            getShareList()
        }
    }

    /**
     * 获取分享btn
     */
    fun getShareBtn() = tv_inviteGift_share

    /**
     * 获取分享列表
     */
    @SuppressLint("SetTextI18n")
    private fun getShareList() {
        canGet = 0
        DialogUtils.showBeautifulDialog(requireContext())
        val getShareList = RetrofitUtils.builder().getShareList()
        getShareListObservable = getShareList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                shareReward = it.getData()?.share_reward!!
                                inviteReward = it.getData()?.invite_reward!!
                                mView?.tv_inviteGift_inviteMoney?.text =
                                    if (BaseAppUpdateSetting.isToAuditVersion) "+$inviteReward"
                                    else "+${inviteReward / 10}元"
                                if (it.getData()?.list != null) {
                                    mData.clear()
                                    for (data in it.getData()!!.list!!) {
                                        mData.add(data)
                                        if (data.receive == 0) {
                                            //可领取
                                            canGet++
                                        }
                                    }
                                    formatInfo()
                                    mAdapter?.notifyDataSetChanged()
                                }
                                if (canGet == 0) {
                                    if (RedPointBean.getData() != null) {
                                        val data = RedPointBean.getData()!!
                                        data.invite = 0
                                        RedPointBean.setData(data)
                                        EventBus.getDefault().postSticky(data)
                                    }
                                    //都没有可领取的话,没有小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.UN_SHOW
                                        )
                                    )
                                } else {
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.SHOW
                                        )
                                    )
                                }
                            }
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })

        val inviteList = RetrofitUtils.builder().inviteList()
        getInviteListObservable = inviteList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mData4New.clear()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data != null) {
                                for (info in it.data) {
                                    if (info.h1 == 1) {
                                        canGet++
                                    }
                                    if (info.h2 == 1) {
                                        canGet++
                                    }
                                    if (info.h3 == 1) {
                                        canGet++
                                    }
                                    if (info.h4 == 1) {
                                        canGet++
                                    }
                                    if (info.h5 == 1) {
                                        canGet++
                                    }
                                }
                                mData4New.addAll(it.data)
                                mAdapter4New?.notifyDataSetChanged()
                                if (canGet == 0) {
                                    if (RedPointBean.getData() != null) {
                                        val data = RedPointBean.getData()!!
                                        data.invite = 0
                                        RedPointBean.setData(data)
                                        EventBus.getDefault().postSticky(data)
                                    }
                                    //都没有可领取的话,没有小红点
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.UN_SHOW
                                        )
                                    )
                                } else {
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.USELESS,
                                            GiftShowState.SHOW
                                        )
                                    )
                                }
                            }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        mView?.tv_inviteGift_share?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    val phone = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
                    if (phone.isNullOrBlank() && !wantToShare) {
                        DialogUtils.showDefaultDialog(
                            requireContext(),
                            "未绑定手机号",
                            "需要绑定手机号后邀请用户才能领取邀请大礼",
                            "不绑定,要分享",
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
                        wantToShare = true
                    } else {
                        toGetShareUrl()
                    }
                }
        }

        mAdapter = BaseAdapterWithPosition.Builder<GetShareListBean.DataBean.ListBean>()
            .setLayoutId(R.layout.item_invite_gift)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                itemView.iv_item_gift_from.setImageResource(
                    when (itemData.channel) {
                        "wx" -> R.mipmap.gift_qq
                        "qq" -> R.mipmap.gift_wechat
                        else -> R.mipmap.icon
                    }
                )
                itemView.tv_item_gift_name.text = "成功邀请${itemData.user?.user_phone ?: "特戒用户"}获得"
                itemView.tv_item_gift_integral.text =
                    if (BaseAppUpdateSetting.isToAuditVersion) "+$inviteReward"
                    else "+${inviteReward / 10}元"
                when (itemData.receive) {
                    0 -> {
                        //没有领取
                        itemView.tv_item_gift_get.text = getString(R.string.get)
                        itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.white_FFFFFF))
                        itemView.tv_item_gift_get.setBackgroundResource(R.drawable.bg_invite_gift_can_get)
                    }
                    1 -> {
                        //领取了
                        itemView.tv_item_gift_get.text = "已领取"
                        itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.green_63C5AD))
                        itemView.tv_item_gift_get.setBackgroundResource(R.drawable.transparent)
                    }
                }

                RxView.clicks(itemView.tv_item_gift_get)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.receive == 0) {
                            toGetInviteGift(itemData.id, itemView)
                            canGet--
                            if (canGet == 0) {
                                if (RedPointBean.getData() != null) {
                                    val data = RedPointBean.getData()!!
                                    data.invite = 0
                                    RedPointBean.setData(data)
                                    EventBus.getDefault().postSticky(data)
                                }
                                //都没有可领取的话,没有小红点
                                EventBus.getDefault().postSticky(
                                    GiftShowPoint(
                                        GiftShowState.USELESS,
                                        GiftShowState.USELESS,
                                        GiftShowState.UN_SHOW
                                    )
                                )
                            } else {
                                EventBus.getDefault().postSticky(
                                    GiftShowPoint(
                                        GiftShowState.USELESS,
                                        GiftShowState.USELESS,
                                        GiftShowState.SHOW
                                    )
                                )
                            }
                        }
                    }
            }.create()

        mView?.rv_inviteGift?.adapter = mAdapter
        mView?.rv_inviteGift?.layoutManager = SafeLinearLayoutManager(requireContext())

        mAdapter4New = BaseAdapterWithPosition.Builder<InviteListNewBean.Data>()
            .setData(mData4New)
            .setLayoutId(R.layout.item_invite_time)
            .addBindView { itemView, itemData, position ->
                itemView.tv_invite_user_phoneEnding.text =
                    Html.fromHtml("成功邀请尾号<b>XXXX</b>的用户".replace("XXXX", itemData.phone_ending))
                itemView.pb_invite_user_progress.setProgress(itemData.total_duration)

                setInviteHourImg(itemView.iv_invite_user_h1, itemData.h1)
                setInviteHourImg(itemView.iv_invite_user_h2, itemData.h2)
                setInviteHourImg(itemView.iv_invite_user_h3, itemData.h3)
                setInviteHourImg(itemView.iv_invite_user_h4, itemData.h4)
                setInviteHourImg(itemView.iv_invite_user_h5, itemData.h5)

                setInviteBtnState(
                    itemView.tv_invite_user_h1,
                    itemData.h1,
                    position,
                    itemData.share_id,
                    1
                )
                setInviteBtnState(
                    itemView.tv_invite_user_h2,
                    itemData.h2,
                    position,
                    itemData.share_id,
                    2
                )
                setInviteBtnState(
                    itemView.tv_invite_user_h3,
                    itemData.h3,
                    position,
                    itemData.share_id,
                    3
                )
                setInviteBtnState(
                    itemView.tv_invite_user_h4,
                    itemData.h4,
                    position,
                    itemData.share_id,
                    4
                )
                setInviteBtnState(
                    itemView.tv_invite_user_h5,
                    itemData.h5,
                    position,
                    itemData.share_id,
                    5
                )
            }.create()

        mView?.rv_inviteUser?.adapter = mAdapter4New
        mView?.rv_inviteUser?.layoutManager = SafeLinearLayoutManager(requireContext())
    }

    /**
     * 设置邀请时长图标
     */
    private fun setInviteHourImg(view: ImageView, state: Int) {
        view.setImageResource(if (state == 0) R.mipmap.bg_invite_hour_cannot else R.mipmap.bg_invite_hour_can)
    }

    /**
     * 设置邀请奖励领取状态
     */
    @SuppressLint("CheckResult")
    private fun setInviteBtnState(
        view: TextView,
        state: Int,
        position: Int,
        shareId: Int,
        h_id: Int
    ) {
        view.text = if (state == 2) "已领" else "领取"
        when (state) {
            0 -> {
                view.setTextColor(resources.getColor(R.color.white_FFFFFF))
                view.setBackgroundResource(R.mipmap.bg_invite_btn_cannot)
            }
            1 -> {
                view.setTextColor(resources.getColor(R.color.white_FFFFFF))
                view.setBackgroundResource(R.mipmap.bg_invite_btn_can)
            }
            2 -> {
                view.setTextColor(resources.getColor(R.color.green_2EC8AC))
                view.setBackgroundResource(R.drawable.transparent)
            }
        }
//        view.setBackgroundResource(if (state == 1) R.mipmap.bg_invite_btn_can else R.mipmap.bg_invite_btn_cannot)
        RxView.clicks(view)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (state == 1) {
                    toGetInviteRecharge(position, shareId, h_id)
                }
            }
    }

    /**
     * 邀请人后的领取积分
     */
    private fun toGetInviteRecharge(position: Int, shareId: Int, h_id: Int) {
        DialogUtils.showBeautifulDialog(requireContext())
        val inviteListGetRecharge = RetrofitUtils.builder().inviteListGetRecharge(shareId, h_id)
        getInviteRechargeObservable = inviteListGetRecharge.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            //领取成功之后的变形
                            when (h_id) {
                                1 -> mData4New[position].h1 = 2
                                2 -> mData4New[position].h2 = 2
                                3 -> mData4New[position].h3 = 2
                                4 -> mData4New[position].h4 = 2
                                5 -> mData4New[position].h5 = 2
                                else -> mData4New[position].h1 = 2
                            }
                            mAdapter4New?.notifyItemChanged(position)

                            canGet--
                            if (canGet == 0) {
                                if (RedPointBean.getData() != null) {
                                    val data = RedPointBean.getData()!!
                                    data.invite = 0
                                    RedPointBean.setData(data)
                                    EventBus.getDefault().postSticky(data)
                                }
                                //都没有可领取的话,没有小红点
                                EventBus.getDefault().postSticky(
                                    GiftShowPoint(
                                        GiftShowState.USELESS,
                                        GiftShowState.USELESS,
                                        GiftShowState.UN_SHOW
                                    )
                                )
                            } else {
                                EventBus.getDefault().postSticky(
                                    GiftShowPoint(
                                        GiftShowState.USELESS,
                                        GiftShowState.USELESS,
                                        GiftShowState.SHOW
                                    )
                                )
                            }

                            (activity as GiftActivity).isFirstCreate = false
                            SPUtils.putValue(SPArgument.INTEGRAL, it.getData()?.user_integral)
                            DialogActivity.showGetIntegral(
                                requireActivity(),
                                20,
                                true,
                                object : DialogActivity.OnCallback {
                                    override fun cancel() {
                                        EventBus.getDefault().postSticky(
                                            IntegralChange(it.getData()?.user_integral!!)
                                        )
                                    }
                                }
                            )
                        }
                        -1 -> {
                            ToastUtils.show(it.getMsg())
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            ToastUtils.show(it.getMsg())
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    /**
     * 格式化一下数据进行排序,将已领取放在底下
     */
    private fun formatInfo() {
        val canGetList = mutableListOf<GetShareListBean.DataBean.ListBean>()
        val overGetList = mutableListOf<GetShareListBean.DataBean.ListBean>()
        if (mData.size > 0) {
            for (data in mData) {
                if (data.receive == 0) {
                    //没领取
                    canGetList.add(data)
                } else if (data.receive == 1) {
                    //已领取
                    overGetList.add(data)
                }
            }
        }
        mData.clear()
        mData.addAll(canGetList)
        mData.addAll(overGetList)
    }

    /**
     * 领取邀请奖励
     */
    private fun toGetInviteGift(id: Int?, itemView: View) {
        DialogUtils.showBeautifulDialog(requireContext())
        val getInviteGift = RetrofitUtils.builder().getInviteGift(id!!)
        getInviteGiftObservable = getInviteGift.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                //领取成功之后的变形
                                itemView.tv_item_gift_get.text = "已领取"
                                itemView.tv_item_gift_get.setTextColor(resources.getColor(R.color.green_63C5AD))
                                itemView.tv_item_gift_get.setBackgroundResource(R.drawable.transparent)

                                (activity as GiftActivity).isFirstCreate = false
                                SPUtils.putValue(SPArgument.INTEGRAL, it.getData()?.user_integral)
                                DialogActivity.showGetIntegral(
                                    requireActivity(),
                                    inviteReward,
                                    true,
                                    object : DialogActivity.OnCallback {
                                        override fun cancel() {
                                            EventBus.getDefault().postSticky(
                                                IntegralChange(it.getData()?.user_integral!!)
                                            )
                                        }
                                    }
                                )

                            }
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    /**
     * 先获取分享链接
     */
    private fun toGetShareUrl() {
        DialogUtils.showBeautifulDialog(requireContext())
        val getShareUrl = RetrofitUtils.builder().getShareUrl(
            if (BaseAppUpdateSetting.isToPromoteVersion) 1
            else null
        )
        getShareUrlObservable = getShareUrl.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null && it.getData()?.url != null) {
                                ShareJumpUtils.showDefaultDialog(
                                    requireActivity(),
                                    message = "免费充值天天送，好玩的服处处有 点击下载${resources.getString(R.string.app_name)}: ${it.getData()?.url!!}",
                                )
                            }
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
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    @Subscribe
    fun time12NeedNewInfo(giftNeedNewInfo: GiftNeedNewInfo) {
        if (MyApp.getInstance().isHaveToken() && giftNeedNewInfo.isShowInviteGiftNeed) {
            getShareList()
        }
    }

    override fun onDestroy() {
        getShareListObservable?.dispose()
        getShareListObservable = null

        getInviteGiftObservable?.dispose()
        getInviteGiftObservable = null

        getShareUrlObservable?.dispose()
        getShareUrlObservable = null

        getInviteListObservable?.dispose()
        getInviteListObservable = null

        getInviteRechargeObservable?.dispose()
        getInviteListObservable = null
        super.onDestroy()
    }
}