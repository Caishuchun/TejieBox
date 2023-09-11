package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.AccountSafeActivity
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.GiftActivity
import com.fortune.tejiebox.activity.VerificationCodeActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.RedPointBean
import com.fortune.tejiebox.bean.WhitePiaoListBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.GiftNeedNewInfo
import com.fortune.tejiebox.event.GiftShowPoint
import com.fortune.tejiebox.event.GiftShowState
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.ActivityManager
import com.fortune.tejiebox.utils.DialogUtils
import com.fortune.tejiebox.utils.HttpExceptionUtils
import com.fortune.tejiebox.utils.IsMultipleOpenAppUtils
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.snail.antifake.jni.EmulatorDetectUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_white_piao.view.rv_whitePiao
import kotlinx.android.synthetic.main.item_white_piao.view.iv_white_piao_integral
import kotlinx.android.synthetic.main.item_white_piao.view.rl_white_piao_bg
import kotlinx.android.synthetic.main.item_white_piao.view.tv_white_piao_btn
import kotlinx.android.synthetic.main.item_white_piao.view.tv_white_piao_integral
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class WhitePiaoFragment : Fragment() {

    private var mView: View? = null
    private var adapter: BaseAdapterWithPosition<WhitePiaoListBean.DataBean>? = null
    private var whitePiaoListObservable: Disposable? = null
    private var whitePiaoObservable: Disposable? = null
    private var mDate = mutableListOf<WhitePiaoListBean.DataBean>()
    private var canClick = false

    companion object {
        fun newInstance() = WhitePiaoFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_white_piao, container, false)
        getInfo()
        return mView
    }

    private var canGetItem: View? = null
    fun getCanGetItem() = canGetItem ?: tempFirstItem

    /**
     * 获取数据
     */
    private fun getInfo() {
        DialogUtils.showBeautifulDialog(requireContext())
        val whitePiaoList = RetrofitUtils.builder().whitePiaoList()
        whitePiaoListObservable = whitePiaoList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            mDate = it.getData() as MutableList<WhitePiaoListBean.DataBean>
                            for (data in mDate) {
                                if (data.status == 1) {
                                    canClick = true
                                    EventBus.getDefault().postSticky(
                                        GiftShowPoint(
                                            GiftShowState.USELESS,
                                            GiftShowState.SHOW,
                                            GiftShowState.USELESS
                                        )
                                    )
                                }
                            }
                            initView()
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
                    ToastUtils.show(resources.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                (requireActivity()).finish()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    private var tempFirstItem: View? = null

    @SuppressLint("SimpleDateFormat", "SetTextI18n", "CheckResult")
    private fun initView() {
        val currentTimeMillis = System.currentTimeMillis()
        val format = SimpleDateFormat("yyyyMMDD HH:mm:ss").format(currentTimeMillis)
        val currentHour = format.split(" ")[1].split(":")[0].toInt()
        LogUtils.d("currentHour = $currentHour")

        val id = when (currentHour) {
            in 5 until 9 -> {
                1
            }

            in 10 until 14 -> {
                2
            }

            in 15 until 19 -> {
                3
            }

            in 20 until 24 -> {
                mView?.rv_whitePiao?.postDelayed({
                    mView?.rv_whitePiao?.scrollToPosition(3)
                }, 100)
                4
            }

            else -> {
                0
            }
        }

        adapter = BaseAdapterWithPosition.Builder<WhitePiaoListBean.DataBean>()
            .setLayoutId(R.layout.item_white_piao)
            .setData(mDate)
            .addBindView { itemView, itemData, position ->

                if (position == 0) {
                    tempFirstItem = itemView
                }

                itemView.tv_white_piao_integral.text =
                    if (BaseAppUpdateSetting.isToAuditVersion) "+${itemData.integral}"
                    else "+${itemData.integral?.div(10)}元"



                if (itemData.id == id && itemData.status == 1) {
                    canGetItem = itemView
                }

                when (itemData.id) {
                    1 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(resources.getString(R.string.time_5_9), itemView)
                            }

                            1 -> {
                                get(resources.getString(R.string.time_5_9), itemView)
                            }

                            2 -> {
                                overdue(resources.getString(R.string.time_5_9), itemView)
                            }

                            3 -> {
                                got(resources.getString(R.string.time_5_9), itemView)
                            }
                        }
                    }

                    2 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(resources.getString(R.string.time_10_14), itemView)
                            }

                            1 -> {
                                get(resources.getString(R.string.time_10_14), itemView)
                            }

                            2 -> {
                                overdue(resources.getString(R.string.time_10_14), itemView)
                            }

                            3 -> {
                                got(resources.getString(R.string.time_10_14), itemView)
                            }
                        }
                    }

                    3 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(resources.getString(R.string.time_15_19), itemView)
                            }

                            1 -> {
                                get(resources.getString(R.string.time_15_19), itemView)
                            }

                            2 -> {
                                overdue(resources.getString(R.string.time_15_19), itemView)
                            }

                            3 -> {
                                got(resources.getString(R.string.time_15_19), itemView)
                            }
                        }
                    }

                    4 -> {
                        when (itemData.status) {
                            0 -> {
                                canGet(resources.getString(R.string.time_20_24), itemView)
                            }

                            1 -> {
                                get(resources.getString(R.string.time_20_24), itemView)
                            }

                            2 -> {
                                overdue(resources.getString(R.string.time_20_24), itemView)
                            }

                            3 -> {
                                got(resources.getString(R.string.time_20_24), itemView)
                            }
                        }
                    }
                }

                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.status == 1 && canClick) {
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
                                if (EmulatorDetectUtil.isEmulator(requireContext())) {
                                    //如果检测到是模拟器
                                    ToastUtils.show("模拟器不支持白嫖领取")
                                    return@subscribe
                                } else if (IsMultipleOpenAppUtils.isMultipleOpenApp(requireContext())) {
                                    //如果检测到有多开软件存在
                                    ToastUtils.show("检测到设备存在恶意多开软件, 无法进行白嫖领取")
                                    return@subscribe
                                } else {
                                    toWhitePiao(itemData.id!!, position, itemData, itemView)
                                }
                            }
                        }
                    }
            }
            .create()
        mView?.rv_whitePiao?.adapter = adapter
        mView?.rv_whitePiao?.layoutManager =
            SafeLinearLayoutManager(requireContext())
    }

    /**
     * 进行白嫖
     */
    fun toWhitePiao(id: Int, position: Int, itemData: WhitePiaoListBean.DataBean, itemView: View) {
        val idNum = SPUtils.getString(SPArgument.ID_NUM)
        if (idNum.isNullOrEmpty()) {
            val todayDate = SPUtils.getString(SPArgument.TODAY_DATE)
            val currentTimeMillis = System.currentTimeMillis()
            val simpleDateFormat = SimpleDateFormat("yyyyMMdd")
            val currentDate = simpleDateFormat.format(currentTimeMillis)
            if (todayDate.isNullOrEmpty()) {
                // 没有存时间, 说明是第一次来
                SPUtils.putValue(SPArgument.TODAY_DATE, currentDate)
                DialogUtils.show48HDialog(requireActivity(), false)
                return
            } else {
                if (currentDate == todayDate) {
                    //今天弹过了
                } else {
                    SPUtils.putValue(SPArgument.TODAY_DATE, currentDate)
                    DialogUtils.show48HDialog(requireActivity(), false)
                    return
                }
            }
        }
        DialogUtils.showBeautifulDialog(requireContext())
        val whitePiao = RetrofitUtils.builder().whitePiao(id)
        whitePiaoObservable = whitePiao.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            canClick = false
                            //白嫖成功后,去掉小红点
                            if (RedPointBean.getData() != null) {
                                val data = RedPointBean.getData()!!
                                data.limit_time = 0
                                RedPointBean.setData(data)
                                EventBus.getDefault().postSticky(data)
                            }
                            EventBus.getDefault().postSticky(
                                GiftShowPoint(
                                    GiftShowState.USELESS,
                                    GiftShowState.UN_SHOW,
                                    GiftShowState.USELESS
                                )
                            )

                            (activity as GiftActivity).isFirstCreate = false
                            SPUtils.putValue(
                                SPArgument.INTEGRAL,
                                it.getData()?.user_integral
                            )
                            DialogActivity.showGetIntegral(
                                requireActivity(),
                                itemData.integral!!,
                                true,
                                object : DialogActivity.OnCallback {
                                    override fun cancel() {
                                        EventBus.getDefault().postSticky(
                                            IntegralChange(it.getData()?.user_integral!!)
                                        )

                                        when (position) {
                                            0 -> {
                                                got(
                                                    resources.getString(R.string.time_5_9),
                                                    itemView
                                                )
                                            }

                                            1 -> {
                                                got(
                                                    resources.getString(R.string.time_10_14),
                                                    itemView
                                                )
                                            }

                                            2 -> {
                                                got(
                                                    resources.getString(R.string.time_15_19),
                                                    itemView
                                                )
                                            }

                                            3 -> {
                                                got(
                                                    resources.getString(R.string.time_20_24),
                                                    itemView
                                                )
                                            }
                                        }
                                    }
                                })
                        }

                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(requireActivity())
                        }

                        3 -> {
                            it.getMsg()?.let { it1 ->
                                DialogUtils.showDefaultDialog(
                                    requireContext(), "提醒", it1, null, "好的", null
                                )
                            }
                        }

                        4 -> {
                            //首次白嫖
                            tempId = id
                            tempPosition = position
                            tempItemData = itemData
                            tempItemView = itemView
                            val intent = Intent(
                                requireContext(),
                                VerificationCodeActivity::class.java
                            )
                            val bundle = Bundle()
                            bundle.putSerializable(
                                VerificationCodeActivity.TYPE,
                                VerificationCodeActivity.TITLE.FIRST_WHITE_PIAO
                            )
                            intent.putExtras(bundle)
                            requireActivity().startActivityForResult(intent, 10102)
                        }

                        5 -> {
                            // 超过48小时未实名认证
                            DialogUtils.show48HDialog(requireActivity(), true, it.getMsg())
                        }

                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(resources.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(
                    HttpExceptionUtils.getExceptionMsg(
                        requireContext(),
                        it
                    )
                )
            })
    }

    /**
     * 已领取
     */
    @SuppressLint("SetTextI18n")
    private fun got(time: String, itemView: View) {
        itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_got)
        itemView.iv_white_piao_integral.setImageResource(R.mipmap.money_ed)
        itemView.tv_white_piao_btn.text =
            "$time  已领取"
    }

    /**
     * 点击领取
     */
    @SuppressLint("SetTextI18n")
    private fun get(time: String, itemView: View) {
        itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_get)
        itemView.iv_white_piao_integral.setImageResource(R.mipmap.money)
        itemView.tv_white_piao_btn.text =
            "$time  点击领取"
    }

    /**
     * 可以领取
     */
    @SuppressLint("SetTextI18n")
    private fun canGet(time: String, itemView: View) {
        itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_can_get)
        itemView.iv_white_piao_integral.setImageResource(R.mipmap.money_ed)
        itemView.tv_white_piao_btn.text =
            "$time  ${resources.getString(R.string.can_get)}"
    }

    /**
     * 已过期
     */
    @SuppressLint("SetTextI18n")
    private fun overdue(time: String, itemView: View) {
        itemView.rl_white_piao_bg.setBackgroundResource(R.drawable.bg_white_piao_overdue)
        itemView.iv_white_piao_integral.setImageResource(R.mipmap.money_ed)
        itemView.tv_white_piao_btn.text =
            "$time  ${resources.getString(R.string.overdue)}"
    }


    private var tempId: Int? = null
    private var tempPosition: Int? = null
    private var tempItemData: WhitePiaoListBean.DataBean? = null
    private var tempItemView: View? = null

    /**
     * Activity返回的结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10102 && resultCode == Activity.RESULT_OK) {
            //首次白嫖的短信验证码验证成功
            val code = data?.getIntExtra("code", -1)
            LogUtils.d("${javaClass.simpleName}=code=>$code")
            if (code == 2) {
                if (tempId != null && tempPosition != null && tempItemData != null && tempItemView != null) {
                    toWhitePiao(tempId!!, tempPosition!!, tempItemData!!, tempItemView!!)
                    tempId = null
                    tempPosition = null
                    tempItemData = null
                    tempItemView = null
                }
            }
        }
    }

    @Subscribe
    fun time12NeedNewInfo(giftNeedNewInfo: GiftNeedNewInfo) {
        if (MyApp.getInstance().isHaveToken() && giftNeedNewInfo.isShowWhitePiaoNeed) {
            getInfo()
        }
    }

    override fun onDestroy() {
        whitePiaoListObservable?.dispose()
        whitePiaoListObservable = null

        whitePiaoObservable?.dispose()
        whitePiaoObservable = null
        super.onDestroy()
    }
}