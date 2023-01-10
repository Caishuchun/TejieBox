package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.activity.NewYearActivity
import com.fortune.tejiebox.activity.ShadeActivity
import com.fortune.tejiebox.bean.NewYear4NianShouBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.BloodVolumeView
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_new_year4_nianshou.view.*
import java.util.concurrent.TimeUnit

class NewYear4NianshouFragment : Fragment() {

    private var mView: View? = null
    private var getNianShouInfoObservable: Disposable? = null
    private var fightNianShouObservable: Disposable? = null
    private var openBox4NianShouObservable: Disposable? = null
    private var mData: NewYear4NianShouBean.Data? = null

    companion object {
        fun newInstance() = NewYear4NianshouFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_new_year4_nianshou, container, false)
        val isShowShade = SPUtils.getBoolean(SPArgument.IS_SHOW_SHADE, true)
        if (isShowShade) {
            startActivity(Intent(requireContext(), ShadeActivity::class.java))
        }
        getInfo()
        return mView
    }

    private fun getInfo() {
//        DialogUtils.showBeautifulDialog(requireContext())
        val nianShouInfo = RetrofitUtils.builder().nianShouInfo()
        getNianShouInfoObservable = nianShouInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            mData = it.data
                            setView()
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

    private fun setView() {
        mView?.tv_newYear_nianShou_star?.let {
            it.text = "${mData!!.current_energy}"
        }

        mView?.blood_newYear_nianShou?.let {
            it.toChangeState(mData!!)
            it.setOnCallBack(object : BloodVolumeView.OnCallBack {
                override fun click(index: Int) {
                    when (mData!!.treasure_list[index].receive_state) {
                        0 -> {
                            //等待开
                            DialogUtils.showInviteTipsDialog(
                                requireContext(),
                                mData!!.treasure_list[index].give_num,
                                if (index == mData!!.treasure_list.size - 1) {
                                    "成功消灭年兽"
                                } else {
                                    "成功炸掉年兽${mData!!.treasure_list[index].blood}血量"
                                },
                                null
                            )
                        }
                        1 -> {
                            //可以开
                            toOpenBox(
                                index,
                                mData!!.treasure_list[index].treasure_id,
                                mData!!.treasure_list[index].give_num
                            )
                        }
                        else -> {
                            ToastUtils.show("您已领取过该宝箱奖励")
                        }
                    }
                }
            })
        }

        mView?.rl_newYear_fireworks1?.let {
            RxView.clicks(it)
                .throttleFirst(2000, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (mData!!.current_energy >= 1) {
                        toFightNianShou(1)
                    } else {
                        ToastUtils.show("您的能量星不足1个")
                    }
                }
        }
        mView?.rl_newYear_fireworks2?.let {
            RxView.clicks(it)
                .throttleFirst(2000, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (mData!!.current_energy >= 5) {
                        toFightNianShou(2)
                    } else {
                        ToastUtils.show("您的能量星不足5个")
                    }
                }
        }
        mView?.rl_newYear_fireworks3?.let {
            RxView.clicks(it)
                .throttleFirst(2000, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (mData!!.current_energy >= 10) {
                        toFightNianShou(3)
                    } else {
                        ToastUtils.show("您的能量星不足10个")
                    }
                }
        }
        mView?.tv_newYear_nianShou_getStar?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    DialogUtils.showGetStarDialog(
                        requireActivity(),
                        object : DialogUtils.OnDialogListener4Star {
                            override fun click(index: Int) {
                                when (index) {
                                    0 -> {
                                        (activity as NewYearActivity).toWhitePiaoFragment()
                                    }
                                    1 -> {
                                        (activity as NewYearActivity).toInviteFragment()
                                    }
                                    2 -> {
                                        MainActivity.getInstance()?.toMainFragment()
                                        requireActivity().finish()
                                    }
                                }
                            }
                        }
                    )
                }
        }
    }

    /**
     * 开宝箱
     */
    private fun toOpenBox(index: Int, treasureId: Int, giveNum: Int) {
        DialogUtils.showBeautifulDialog(requireContext())
        val openBox4NianShou = RetrofitUtils.builder().openBox4NianShou(treasureId)
        openBox4NianShouObservable = openBox4NianShou.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            DialogActivity.showGetIntegral(
                                requireActivity(),
                                giveNum,
                                true,
                                object : DialogActivity.OnCallback {
                                    override fun cancel() {
                                        //宝箱要变成已领取状态
                                        mData!!.treasure_list[index].receive_state = 2
                                        setView()
                                    }
                                }
                            )
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

    /***
     * 开始炸年兽
     */
    private fun toFightNianShou(index: Int) {
        DialogUtils.showBeautifulDialog(requireContext())
        val fightNianShou = RetrofitUtils.builder().fightNianShou(index)
        fightNianShouObservable = fightNianShou.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            val loseBlood = when (index) {
                                1 -> 1000
                                2 -> 5000
                                3 -> 10000
                                else -> 1000
                            }
                            mData!!.current_energy = it.data.current_energy
                            mData!!.current_blood = it.data.current_blood
                            for (info in mData!!.treasure_list) {
                                if (mData!!.total_blood - info.blood >= mData!!.current_blood) {
                                    if (info.receive_state == 0) {
                                        info.receive_state = 1
                                    }
                                }
                            }
                            setView()
                            DialogUtils.showFireworksDialog(
                                requireContext(),
                                index,
                                object : DialogUtils.OnDialogListener {
                                    override fun next() {
                                        loseBlood(loseBlood)
                                        toShakeNianShou()
                                    }
                                })
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

    /**
     * 让年兽抖动一下
     */
    private fun toShakeNianShou() {
        mView?.iv_newYear_nianShou?.let {
            FlipAnimUtils.startShakeByPropertyAnim(
                it, 0.95f, 1.05f, 2f, 800, 2000, false
            )
        }
    }

    /**
     * 掉血
     */
    @SuppressLint("SetTextI18n")
    private fun loseBlood(blood: Int) {
        mView?.tv_newYear_loseBlood?.let {
            it.visibility = View.VISIBLE
            it.text = "-$blood"
            it.postDelayed({
                it.visibility = View.GONE
            }, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getNianShouInfoObservable?.dispose()
        getNianShouInfoObservable = null

        fightNianShouObservable?.dispose()
        fightNianShouObservable = null

        openBox4NianShouObservable?.dispose()
        openBox4NianShouObservable = null
    }
}