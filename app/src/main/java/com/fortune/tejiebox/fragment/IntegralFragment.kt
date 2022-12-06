package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.IntegralActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.RechargeListBean
import com.fortune.tejiebox.bean.RoleListBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_integral.view.*
import kotlinx.android.synthetic.main.item_integral.view.*
import net.center.blurview.ShapeBlurView
import net.center.blurview.enu.BlurMode
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

private const val GAME_ICON: String = "game_icon"
private const val GAME_NAME: String = "game_name"
private const val GAME_CHANNEL_ID: String = "game_channel_id"

class IntegralFragment : Fragment() {
    private var mView: View? = null
    private var gameIcon: String? = null
    private var gameName: String? = null
    private var gameChannelId: String? = null

    private var mGameVersion = ""
    private var mSelectPosition = -1
    private var mSelectRecharge: RechargeListBean.Data.Pricelist? = null
    private var mSelectRole: RoleListBean.Data.Role? = null

    private var getGameRechargeObservable: Disposable? = null
    private var mData = arrayListOf<RechargeListBean.Data.Pricelist>()
    private var mAdapter: BaseAdapterWithPosition<RechargeListBean.Data.Pricelist>? = null

    private var gameRechargeObservable: Disposable? = null
    private var getIntegralObservable: Disposable? = null

    companion object {
        @JvmStatic
        fun newInstance(gameIcon: String, gameName: String, gameChannelId: String) =
            IntegralFragment().apply {
                arguments = Bundle().apply {
                    putString(GAME_ICON, gameIcon)
                    putString(GAME_NAME, gameName)
                    putString(GAME_CHANNEL_ID, gameChannelId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            gameIcon = it.getString(GAME_ICON)
            gameName = it.getString(GAME_NAME)
            gameChannelId = it.getString(GAME_CHANNEL_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_integral, container, false)
        initView()

        getIntegral()
        return mView
    }

    /**
     * 获取剩余积分
     */
    @SuppressLint("SetTextI18n")
    private fun getIntegral() {
//        DialogUtils.showBeautifulDialog(requireContext())
        val getIntegral = RetrofitUtils.builder().getIntegral()
        getIntegralObservable = getIntegral.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d(Gson().toJson(it))
                    when (it.code) {
                        1 -> {
                            mView?.tv_integralFragment_currentIntegral?.let { tv ->
                                tv.text = if (BaseAppUpdateSetting.isToAuditVersion) {
                                    "剩余积分: ${it.data.integral}"
                                } else {
                                    "剩余余额: ${it.data.integral / 10}元"
                                }
                            }
                            SPUtils.putValue(SPArgument.INTEGRAL, it.data.integral)
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
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        mView?.riv_integralFragment_icon?.let {
            Glide.with(this)
                .load(gameIcon)
                .placeholder(R.drawable.bg_gray_6)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(it)
        }

        mView?.tv_integralFragment_gameName?.let {
            it.text = gameName
        }

        mView?.ll_integralFragment_role?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    (activity as IntegralActivity).toRoleFragment()
                }
        }

        mAdapter = BaseAdapterWithPosition.Builder<RechargeListBean.Data.Pricelist>()
            .setData(mData)
            .setLayoutId(R.layout.item_integral)
            .addBindView { itemView, itemData, position ->
                itemView.tv_item_integral.text = "${itemData.money}元充值"
//                itemView.tv_item_recharge.text = "余额兑换"
//                itemView.tv_item_integral.text = "${itemData.money.toInt() * 10}积分"
//                itemView.tv_item_recharge.text = "${itemData.money}元充值"

                if (position == mSelectPosition) {
                    itemView.ll_item_bg.setBackgroundResource(R.drawable.bg_integral_selected)
                } else {
                    itemView.ll_item_bg.setBackgroundResource(R.drawable.bg_integral_unselect)
                }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (position != mSelectPosition) {
                            mSelectPosition = position
                            mSelectRecharge = itemData
                            mAdapter?.notifyDataSetChanged()
                            mView?.tv_integralFragment_tips?.let {
                                it.text =
                                    if (BaseAppUpdateSetting.isToAuditVersion) "使用${itemData.money.toInt() * 10}积分兑换${itemData.money}充值"
                                    else "使用特戒余额充值${itemData.money}元"
                            }
                        }
                    }
            }
            .create()
        mView?.rv_integralFragment_recharge?.let {
            it.adapter = mAdapter
            it.layoutManager = SafeStaggeredGridLayoutManager(3, OrientationHelper.VERTICAL)
        }

        mView?.btn_integralFragment_recharge?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (mSelectRecharge == null || mSelectRole == null) {
                        ToastUtils.show("请选择区服角色和充值额度!")
                    } else {
                        val currentIntegral = SPUtils.getInt(SPArgument.INTEGRAL, 0)
                        val needIntegral = mSelectRecharge!!.money.toInt() * 10
                        if (needIntegral > currentIntegral) {
                            DialogActivity.showRechargeResult(
                                requireActivity(),
                                false,
                                if (BaseAppUpdateSetting.isToAuditVersion) "剩余积分不足"
                                else "余额不足"
                            )
                        } else {
                            val playerId = mSelectRole!!.roleId
                            val rechargeId = mSelectRecharge!!.id
                            val channel = gameChannelId!!
                            val rechargeMoney = mSelectRecharge!!.money.toInt()
                            toRecharge(playerId, rechargeId, channel, rechargeMoney)
                        }
                    }
                }
        }

        val screenWidth = PhoneInfoUtils.getWidth(requireActivity())
        val float = screenWidth.toFloat() / 360
        mView?.blur_integralFragment_start?.let {
            it.refreshView(
                ShapeBlurView.build()
                    .setBlurMode(BlurMode.MODE_RECTANGLE)
                    .setBlurRadius(5 * float)
                    .setDownSampleFactor(0.1f * float)
                    .setOverlayColor(Color.parseColor("#FFFFFF"))
            )
        }
    }

    /**
     * 进行充值
     */
    @SuppressLint("SetTextI18n")
    private fun toRecharge(playerId: Int, rechargeId: Int, channel: String, rechargeMoney: Int) {
        DialogUtils.showBeautifulDialog(requireContext())
        val gameRecharge = RetrofitUtils.builder().gameRecharge(playerId, rechargeId, channel)
        gameRechargeObservable = gameRecharge.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            EventBus.getDefault().postSticky(
                                IntegralChange(-1)
                            )
                            DialogActivity.showRechargeResult(
                                requireActivity(),
                                true,
                                if (BaseAppUpdateSetting.isToAuditVersion) "成功使用特戒积分兑换${rechargeMoney}充值"
                                else "成功使用特戒余额充值${rechargeMoney}元"
                            )

                            mView?.tv_integralFragment_currentIntegral?.let { tv ->
                                val currentIntegral =
                                    if (BaseAppUpdateSetting.isToAuditVersion) {
                                        tv.text.toString().trim()
                                            .replace("剩余积分: ", "").toInt()
                                    } else {
                                        tv.text.toString().trim()
                                            .replace("剩余余额: ", "").replace("元", "")
                                            .toInt() * 10
                                    }
                                val residue = currentIntegral - rechargeMoney * 10
                                tv.text = if (BaseAppUpdateSetting.isToAuditVersion) {
                                    "剩余余额: ${residue / 10}元"
                                } else {
                                    "剩余积分: $residue"
                                }
                                SPUtils.putValue(SPArgument.INTEGRAL, residue)
                            }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            DialogActivity.showRechargeResult(
                                requireActivity(),
                                false,
                                it.msg
                            )
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
     * 设置数据
     */
    @SuppressLint("SetTextI18n")
    fun setRoleInfo(role: RoleListBean.Data.Role, gameVersion: String) {
        mView?.ll_integralFragment_roleInfo?.let {
            it.visibility = View.VISIBLE
        }
        mView?.tv_integralFragment_hint?.let {
            it.visibility = View.GONE
        }

        mView?.tv_integralFragment_roleName?.let {
            it.text = role.roleName
        }
        mView?.tv_integralFragment_RoleLevel?.let {
            it.text = "${role.lev}级"
        }
        mView?.tv_integralFragment_roleJob?.let {
            it.text = role.jobName
        }
        mView?.tv_integralFragment_areaName?.let {
            it.text = role.areaName
        }
        mView?.tv_integralFragment_serviceName?.let {
            it.text = role.serverName
        }

        mGameVersion = gameVersion
        if (null == mSelectRole || mSelectRole != role) {
            mSelectPosition = -1
            mSelectRecharge = null
            mView?.tv_integralFragment_tips?.let {
                it.text = if (BaseAppUpdateSetting.isToAuditVersion) "使用特戒积分免费充值"
                else "使用特戒余额免费充值"
            }
            mSelectRole = role
            toGetGameRechargeList(role)
        }
    }

    /**
     * 获取游戏充值基数
     */
    private fun toGetGameRechargeList(role: RoleListBean.Data.Role) {
        DialogUtils.showBeautifulDialog(requireContext())
        val gameRecharge = RetrofitUtils.builder().getGameRecharge(role.serverId, mGameVersion)
        getGameRechargeObservable = gameRecharge.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            mData.clear()
                            sort(it.data.pricelist)
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                            mAdapter?.notifyDataSetChanged()
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
     * 数据排序
     */
    private fun sort(pricelist: List<RechargeListBean.Data.Pricelist>) {
        val sortedBy = pricelist.sortedBy {
            it.money.toInt()
        }
        mData.addAll(sortedBy)
        mAdapter?.notifyDataSetChanged()
    }


    override fun onDestroy() {
        super.onDestroy()
        getGameRechargeObservable?.dispose()
        getGameRechargeObservable = null

        gameRechargeObservable?.dispose()
        gameRechargeObservable = null
    }

}