package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.NewYearShareActivity
import com.fortune.tejiebox.bean.NewYear4InviteBean
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.LineView
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_new_year_invite.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class NewYear4InviteFragment : Fragment() {

    private var mView: View? = null
    private var getInfoObservable: Disposable? = null
    private var openBoxObservable: Disposable? = null

    @SuppressLint("SimpleDateFormat")
    private val sb = SimpleDateFormat("yyyy.MM.dd")

    companion object {
        fun newInstance() = NewYear4InviteFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_new_year_invite, container, false)
        getInfo()
        return mView
    }

    /**
     * 获取数据
     */
    private fun getInfo() {
//        DialogUtils.showBeautifulDialog(requireContext())
        val invite = RetrofitUtils.builder().invite()
        getInfoObservable = invite.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            setView(it.data)
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

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setView(data: NewYear4InviteBean.Data) {
        val screenWidth = PhoneInfoUtils.getWidth(requireActivity())
        val picWidth = (screenWidth * (79f / 360)).toInt()

        mView?.iv_newYear_invite_qrCode?.let {
            it.setImageBitmap(
                QrCodeUtils.createQRCodeWithLogo(
                    QrCodeUtils.createQRCode(
                        data.share_link,
                        picWidth, picWidth,
                        "UTF-8",
                        "H",
                        "0",
                        Color.BLACK,
                        Color.WHITE
                    ),
                    (resources.getDrawable(R.mipmap.icon) as BitmapDrawable).bitmap,
                    0.2f
                )
            )

            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    val intent = Intent(requireContext(), NewYearShareActivity::class.java)
                    intent.putExtra(NewYearShareActivity.SHARE_LINK, data.share_link)
                    startActivity(intent)
                }
        }

        mView?.tv_newYear_invite_tips?.let {
            it.text = "新用户下载后需要累积玩游戏${data.total_time}分钟"
        }

        mView?.tv_newYear_invite_share?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    val intent = Intent(requireContext(), NewYearShareActivity::class.java)
                    intent.putExtra(NewYearShareActivity.SHARE_LINK, data.share_link)
                    startActivity(intent)
                }
        }

        mView?.lineView_newYear_invite?.let {
            it.changeState(data.treasure_list)
            it.setOnCallBack(object : LineView.OnCallBack {
                override fun click(index: Int) {
                    when (data.treasure_list[index].receive_state) {
                        0 -> {
                            DialogUtils.showInviteTipsDialog(
                                requireContext(),
                                data.treasure_list[index].give_num,
                                "成功邀请${data.treasure_list[index].invitation_num}人",
                                null
                            )
                        }
                        1 -> {
                            toGet(data.treasure_list, index)
                        }
                        else -> {
                            ToastUtils.show("您已领取过该宝箱奖励")
                        }
                    }
                }
            })
        }
    }

    /**
     * 领取邀请礼包
     */
    private fun toGet(treasureList: List<NewYear4InviteBean.Data.Treasure>, index: Int) {
        DialogUtils.showBeautifulDialog(requireContext())
        val openBox = RetrofitUtils.builder().openBox(treasureList[index].treasure_id)
        openBoxObservable = openBox.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            DialogActivity.showGetIntegral(
                                requireActivity(),
                                treasureList[index].give_num,
                                true, null
                            )
                            treasureList[index].receive_state = 2
                            mView?.lineView_newYear_invite?.let { lineView ->
                                lineView.changeState(treasureList)
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
                }, {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                })
    }

    override fun onDestroy() {
        super.onDestroy()
        getInfoObservable?.dispose()
        getInfoObservable = null

        openBoxObservable?.dispose()
        openBoxObservable = null
    }
}