package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.ChangePhone1Activity
import com.fortune.tejiebox.activity.GiftActivity
import com.fortune.tejiebox.activity.IdCardActivity
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.IntegralChange
import com.fortune.tejiebox.event.IsHaveIdChange
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.event.RedPointChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_mine.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class MineFragment : Fragment() {

    private var mView: View? = null
    private var getIntegralObservable: Disposable? = null

    companion object {
        @JvmStatic
        fun newInstance() = MineFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_mine, container, false)
        initView()
        if (MyApp.getInstance().isHaveToken()) {
            getIntegral()
        }
        return mView
    }

    /**
     * 获取现有积分
     */
    @SuppressLint("SetTextI18n")
    private fun getIntegral(needDialog: Boolean = true) {
        mView?.tv_mineFragment_integral?.let {
            it.text = "0积分"
        }
        val getIntegral = RetrofitUtils.builder().getIntegral()
        getIntegralObservable = getIntegral.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    LogUtils.d(Gson().toJson(it))
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.INTEGRAL, it.data.integral)
                            mView?.tv_mineFragment_integral?.let { tv ->
                                tv.text = "${it.data.integral}积分"
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
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                })
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        mView?.tv_mineFragment_phone?.let {
            if (MyApp.getInstance().isHaveToken()) {
                val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)
                it.text = "${phone?.substring(0, 3)}****${phone?.substring(7)}"
            } else {
                it.text = "未登录"
            }
        }

        mView?.tv_mineFragment_idCardMsg?.let {
            if (MyApp.getInstance().isHaveToken()) {
                val isHaveId = SPUtils.getInt(SPArgument.IS_HAVE_ID)
                if (isHaveId == 1) {
                    it.text = "已认证"
                    it.setTextColor(Color.parseColor("#5F60FF"))
                } else {
                    it.text = "未认证"
                    it.setTextColor(Color.parseColor("#FF982E"))
                }
            } else {
                it.text = "未认证"
                it.setTextColor(Color.parseColor("#FF982E"))
            }
        }

        mView?.tv_mineFragment_version?.let {
            it.text = "V ${MyApp.getInstance().getVersion()}"
        }
        mView?.tv_mineFragment_cache?.let {
            it.text = CacheUtils.getCacheSize()
        }

        mView?.ll_mineFragment_account?.let {
            RxView.clicks(it)
                .throttleFirst(
                    200,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        requireContext().startActivity(
                            Intent(
                                requireContext(),
                                ChangePhone1Activity::class.java
                            )
                        )
                    } else {
                        LoginUtils.toQuickLogin(requireActivity())
                    }
                }
        }

        mView?.ll_mineFragment_idCard?.let {
            RxView.clicks(it)
                .throttleFirst(
                    200,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        val isHaveId = SPUtils.getInt(SPArgument.IS_HAVE_ID)
                        val intent = Intent(requireContext(), IdCardActivity::class.java)
                        intent.putExtra(IdCardActivity.FROM, isHaveId)
                        requireContext().startActivity(intent)
                    } else {
                        LoginUtils.toQuickLogin(requireActivity())
                    }
                }
        }

        mView?.ll_mineFragment_integral?.let {
            RxView.clicks(it)
                .throttleFirst(
                    200,
                    TimeUnit.MILLISECONDS
                )
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        requireContext().startActivity(
                            Intent(
                                requireContext(),
                                GiftActivity::class.java
                            )
                        )
                    } else {
                        LoginUtils.toQuickLogin(requireActivity())
                    }
                }
        }

        mView?.ll_mineFragment_clearCache?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    DialogUtils.showDefaultDialog(
                        requireContext(),
                        "清理缓存",
                        "确定要清理缓存吗?",
                        "取消",
                        "确定",
                        object : DialogUtils.OnDialogListener {
                            override fun next() {
                                CacheUtils.clearCache(requireActivity())
                                mView?.tv_mineFragment_cache?.let { textView ->
                                    textView.text = CacheUtils.getCacheSize()
                                }
                            }
                        }
                    )
                }
        }
        mView?.tv_mineFragment_exit?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        DialogUtils.showDefaultDialog(
                            requireContext(),
                            "退出登录",
                            "确定要退出登录吗?",
                            "取消",
                            "确定",
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    ActivityManager.exitLogin(requireActivity() as MainActivity)
                                }
                            }
                        )
                    }
                }
        }
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        mView?.tv_mineFragment_phone?.let {
            if (loginStatusChange.isLogin) {
                val phone = loginStatusChange.phone
                it.text = "${phone?.substring(0, 3)}****${phone?.substring(7)}"
            } else {
                it.text = "未登录"
            }
        }
        mView?.tv_mineFragment_idCardMsg?.let {
            if (MyApp.getInstance().isHaveToken()) {
                val isHaveId = SPUtils.getInt(SPArgument.IS_HAVE_ID)
                if (isHaveId == 1) {
                    it.text = "已认证"
                    it.setTextColor(Color.parseColor("#5F60FF"))
                } else {
                    it.text = "未认证"
                    it.setTextColor(Color.parseColor("#FF982E"))
                }
            } else {
                it.text = "未认证"
                it.setTextColor(Color.parseColor("#FF982E"))
            }
        }

        if (loginStatusChange.isLogin) {
            getIntegral()
        } else {
            mView?.tv_mineFragment_integral?.let {
                it.text = "0积分"
            }
        }
    }

    /**
     * 随时修改当前积分
     */
    @SuppressLint("SetTextI18n")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeIntegral(integralChange: IntegralChange) {
        if (integralChange.integral == -1) {
            if (MyApp.getInstance().isHaveToken()) {
                getIntegral(false)
            }
        }
        if (integralChange.integral >= 0) {
            SPUtils.putValue(SPArgument.INTEGRAL, integralChange.integral)
            mView?.tv_mineFragment_integral?.let {
                it.text = "${integralChange.integral}积分"
            }
        }
    }

    /**
     * 是否实名认证过了
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun isHaveId(isHaveIdChange: IsHaveIdChange) {
        mView?.tv_mineFragment_idCardMsg?.let {
            if (isHaveIdChange.isHaveId == 1) {
                it.text = "已认证"
                it.setTextColor(Color.parseColor("#5F60FF"))
            } else {
                it.text = "未认证"
                it.setTextColor(Color.parseColor("#FF982E"))
            }
        }
    }

    /**
     * 是否显示小红点
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun isShowRedPoint(redPointChange: RedPointChange) {
        if (redPointChange.isShow) {
            Thread{
                Thread.sleep(200)
                requireActivity().runOnUiThread {
                    mView?.iv_mineFragment_point?.visibility = View.VISIBLE
                }
            }.start()
        } else {
            Thread{
                Thread.sleep(200)
                requireActivity().runOnUiThread {
                    mView?.iv_mineFragment_point?.visibility = View.GONE
                }
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        getIntegralObservable?.dispose()
        getIntegralObservable = null
    }
}