package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.activity.NewYearActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.NewYear4WhitePiaoBean
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_new_year_white_piao.view.*
import kotlinx.android.synthetic.main.item_new_year_white_piao.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class NewYear4WhitePiaoFragment : Fragment() {

    private var mView: View? = null
    private var getInfoObservable: Disposable? = null
    private var receiveObservable: Disposable? = null
    private var endDate = ""
    private var mAdapter: BaseAdapterWithPosition<NewYear4WhitePiaoBean.Data.Status>? = null

    @SuppressLint("SimpleDateFormat")
    private val sb = SimpleDateFormat("yyyy.MM.dd")

    companion object {
        fun newInstance() = NewYear4WhitePiaoFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_new_year_white_piao, container, false)
        getInfo()
        return mView
    }

    /**
     * 获取数据
     */
    private fun getInfo() {
//        DialogUtils.showBeautifulDialog(requireContext())
        val freeGive = RetrofitUtils.builder().freeGive()
        getInfoObservable = freeGive.subscribeOn(Schedulers.io())
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

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables", "CheckResult")
    private fun setView(data: NewYear4WhitePiaoBean.Data) {
        val startDate = sb.format(data.start_time * 1000L)
        endDate = sb.format(data.end_time * 1000L)
        (activity as NewYearActivity).setActivityDate("活动时间: $startDate 至 $endDate")

        val resultData = data.status_list.sortedBy {
            it.activity_sort
        }

        mAdapter = BaseAdapterWithPosition.Builder<NewYear4WhitePiaoBean.Data.Status>()
            .setData(resultData)
            .setLayoutId(R.layout.item_new_year_white_piao)
            .addBindView { itemView, itemData, position ->
                itemView.tv_newYear_whitePiao_title.text = itemData.activity_title
                itemView.tv_newYear_whitePiao_msg.text = itemData.activity_msg
                itemView.tv_newYear_whitePiao_num.text = "+${itemData.give_num / 10}元"
                itemView.tv_newYear_whitePiao_date.text =
                    if (position == 0) "进行中" else itemData.date
                when (itemData.activity_state) {
                    0 -> {
                        //未领取
                        itemView.tv_newYear_whitePiao_state.visibility = View.VISIBLE
                        itemView.tv_newYear_whitePiao_state.text =
                            "已完成 ${Math.min(itemData.current_time, 30)}/${itemData.total_time}"
                        itemView.tv_newYear_whitePiao_btn.text = "前往"
                        itemView.tv_newYear_whitePiao_btn.setTextColor(Color.parseColor("#FFFFFF"))
                        itemView.tv_newYear_whitePiao_btn.setBackgroundResource(R.drawable.bg_trans_circle)
                    }
                    1 -> {
                        //可领取
                        itemView.tv_newYear_whitePiao_state.visibility = View.GONE
                        itemView.tv_newYear_whitePiao_btn.text = "领取"
                        itemView.tv_newYear_whitePiao_btn.setTextColor(Color.parseColor("#EE1212"))
                        itemView.tv_newYear_whitePiao_btn.setBackgroundResource(R.drawable.bg_new_year_wp_get)
                    }
                    2 -> {
                        //已领取
                        itemView.tv_newYear_whitePiao_state.visibility = View.GONE
                        itemView.tv_newYear_whitePiao_btn.text = "已领取"
                        itemView.tv_newYear_whitePiao_btn.setTextColor(Color.parseColor("#FFFFFF"))
                        itemView.tv_newYear_whitePiao_btn.setBackgroundResource(R.drawable.bg_new_year_wp_got)
                    }
                    3 -> {
                        //明日领取
                        itemView.tv_newYear_whitePiao_state.visibility = View.VISIBLE
                        itemView.tv_newYear_whitePiao_btn.text = "明日领取"
                        itemView.tv_newYear_whitePiao_state.text =
                            "已完成 ${Math.min(itemData.current_time, 30)}/${itemData.total_time}"
                        itemView.tv_newYear_whitePiao_btn.setTextColor(Color.parseColor("#FFFFFF"))
                        itemView.tv_newYear_whitePiao_btn.setBackgroundResource(R.drawable.bg_new_year_wp_got)
                    }
                    4 -> {
                        //已过期
                        itemView.tv_newYear_whitePiao_state.visibility = View.GONE
                        itemView.tv_newYear_whitePiao_btn.text = "已过期"
                        itemView.tv_newYear_whitePiao_btn.setTextColor(Color.parseColor("#FFFFFF"))
                        itemView.tv_newYear_whitePiao_btn.setBackgroundResource(R.drawable.bg_new_year_wp_got)
                    }
                }
                RxView.clicks(itemView.tv_newYear_whitePiao_btn)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        when (itemData.activity_state) {
                            0 -> {
                                //未领取
                                MainActivity.getInstance()?.toMainFragment()
                                requireActivity().finish()
                            }
                            1 -> {
                                //可领取
                                toGet()
                            }
                        }
                    }
            }.create()

        mView?.rv_newYear_whitePiao?.let {
            it.adapter = mAdapter
            it.layoutManager = SafeLinearLayoutManager(requireContext())
        }
    }

    /**
     * 领取礼包
     */
    private fun toGet() {
//        DialogUtils.showBeautifulDialog(requireContext())
        val receiveFreeGive = RetrofitUtils.builder().receiveFreeGive()
        receiveObservable = receiveFreeGive.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    when (it.code) {
                        1 -> {
                            //领取成功,修改提示
                            mView?.tv_newYear_whitePiao_btn?.let { tv ->
                                //已领取
                                tv.text = "已领取"
                                tv.setTextColor(Color.parseColor("#FFFFFF"))
                                tv.setBackgroundResource(R.drawable.bg_new_year_wp_got)
                            }
                            mView?.tv_newYear_whitePiao_state?.let { tv ->
                                //已领取
                                val currentTimeMillis = System.currentTimeMillis()
                                val today = sb.format(currentTimeMillis)
                                if (today == endDate) {
                                    tv.visibility = View.GONE
                                } else {
                                    tv.visibility = View.VISIBLE
                                    tv.text = "明日领取"
                                }
                            }
                            DialogActivity.showGetIntegral(
                                requireActivity(), 50, true, null
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

    override fun onDestroy() {
        super.onDestroy()
        getInfoObservable?.dispose()
        getInfoObservable = null

        receiveObservable?.dispose()
        receiveObservable = null
    }
}