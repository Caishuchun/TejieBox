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
import com.fortune.tejiebox.activity.AccountSafeActivity
import com.fortune.tejiebox.activity.CustomerServiceActivity
import com.fortune.tejiebox.activity.IdCardActivity
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.IsHaveIdChange
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.event.ShowNumChange
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_mine.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class MineFragment : Fragment() {

    private var mView: View? = null

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
        return mView
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        mView?.tv_mineFragment_phone?.let {
            if (MyApp.getInstance().isHaveToken()) {
                val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)
                val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT)
                when {
                    phone.isNullOrBlank() -> {
                        it.text = "未绑定手机号"
                        it.setTextColor(Color.parseColor("#FF982E"))
                    }
                    account.isNullOrBlank() -> {
                        it.text = "未绑定账号"
                        it.setTextColor(Color.parseColor("#FF982E"))
                    }
                    else -> {
                        it.text = "已绑定"
                        it.setTextColor(Color.parseColor("#5F60FF"))
                    }
                }
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

        val channel = SPUtils.getString(SPArgument.UM_CHANNEL_ID, null)
        var patch = ""
        if (channel != null) {
            patch = when (channel) {
                "100" -> ""
                "101" -> ".1"
                else -> ""
            }
        }

        mView?.tv_mineFragment_version?.let {
            it.text = "V${MyApp.getInstance().getVersion()}$patch"
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
                        requireActivity().startActivity(
                            Intent(
                                requireContext(),
                                AccountSafeActivity::class.java
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
                        requireActivity().startActivity(intent)
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

        mView?.ll_mineFragment_customerService?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (MyApp.getInstance().isHaveToken()) {
                        startActivity(Intent(requireContext(), CustomerServiceActivity::class.java))
                    } else {
                        LoginUtils.toQuickLogin(requireActivity())
                    }
                }
        }
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        if (loginStatusChange == null) {
            return
        }
        mView?.tv_mineFragment_phone?.let {
            if (loginStatusChange.isLogin) {
                val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)
                val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT)
                when {
                    phone.isNullOrBlank() -> {
                        it.text = "未绑定手机号"
                        it.setTextColor(Color.parseColor("#FF982E"))
                    }
                    account.isNullOrBlank() -> {
                        it.text = "未绑定账号"
                        it.setTextColor(Color.parseColor("#FF982E"))
                    }
                    else -> {
                        it.text = "已绑定"
                        it.setTextColor(Color.parseColor("#5F60FF"))
                    }
                }
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

//        if (loginStatusChange.isLogin) {
//            getIntegral()
//        } else {
//            mView?.tv_mineFragment_integral?.let {
//                it.text = "0元"
//            }
//        }
    }

    /**
     * 展示客服未读消息数
     */
    @SuppressLint("CheckResult")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeShowNum(showNumChange: ShowNumChange) {
        Observable.timer(100, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (showNumChange.num > 0) {
                    mView?.tv_mineFragment_customerService?.visibility = View.VISIBLE
                    mView?.tv_mineFragment_customerService?.text = "${showNumChange.num}"
                } else {
                    mView?.tv_mineFragment_customerService?.visibility = View.GONE
                }
            }
    }

    /**
     * 是否实名认证过了
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun isHaveId(isHaveIdChange: IsHaveIdChange) {
        if (isHaveIdChange == null) {
            return
        }
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            mView?.tv_mineFragment_phone?.let {
                if (MyApp.getInstance().isHaveToken()) {
                    val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)
                    val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT)
                    when {
                        phone.isNullOrBlank() -> {
                            it.text = "未绑定手机号"
                            it.setTextColor(Color.parseColor("#FF982E"))
                        }
                        account.isNullOrBlank() -> {
                            it.text = "未绑定账号"
                            it.setTextColor(Color.parseColor("#FF982E"))
                        }
                        else -> {
                            it.text = "已绑定"
                            it.setTextColor(Color.parseColor("#5F60FF"))
                        }
                    }
                } else {
                    it.text = "未登录"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mView?.tv_mineFragment_phone?.let {
            if (MyApp.getInstance().isHaveToken()) {
                val phone = SPUtils.getString(SPArgument.PHONE_NUMBER)
                val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT)
                when {
                    phone.isNullOrBlank() -> {
                        it.text = "未绑定手机号"
                        it.setTextColor(Color.parseColor("#FF982E"))
                    }
                    account.isNullOrBlank() -> {
                        it.text = "未绑定账号"
                        it.setTextColor(Color.parseColor("#FF982E"))
                    }
                    else -> {
                        it.text = "已绑定"
                        it.setTextColor(Color.parseColor("#5F60FF"))
                    }
                }
            } else {
                it.text = "未登录"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}