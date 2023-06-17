package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.activity.LoginActivity
import com.fortune.tejiebox.activity.WebActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginChangePage
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login_normal.*
import kotlinx.android.synthetic.main.fragment_login_normal.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class LoginFirstFragment : Fragment() {

    private var sendCodeObservable: Disposable? = null

    companion object {
        fun newInstance() = LoginFirstFragment()
    }

    private var oldPhone = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login_normal, container, false)
        initView(view)
        return view
    }

    @SuppressLint("CheckResult")
    private fun initView(view: View) {
        val isAgree = SPUtils.getBoolean(SPArgument.IS_CHECK_AGREEMENT, false)
        if (isAgree
//            || !BaseAppUpdateSetting.isToPromoteVersion
        ) {
            view.cb_login_first?.isChecked = true
        }
        view.iv_login_first_title.setImageResource(
            if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.app_title2
            else R.mipmap.app_title
        )

        RxView.clicks(view.iv_login_first_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                activity?.finish()
            }
        RxView.clicks(view.ll_login_first_areaCode)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {

            }
        RxView.clicks(view.tv_login_first_login)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (view.cb_login_first?.isChecked == true) {
                    if (view.et_login_first_phone?.text?.length != 11) {
                        ToastUtils.show("请输入正确的手机号")
                        return@subscribe
                    }
                    var isChange = false
                    if (oldPhone != et_login_first_phone.text.toString().trim()) {
                        oldPhone = et_login_first_phone.text.toString().trim()
                        isChange = true
                    }
                    if (et_login_first_phone.text.toString().trim() == "13888888888") {
                        EventBus.getDefault().postSticky(LoginChangePage(1, "+86", "13888888888"))
                        return@subscribe
                    }
                    //如果手机号发生了变化,就需要重新发送短信验证码
                    if (isChange) {
                        SPUtils.putValue(SPArgument.CODE_TIME, 0L)
                    }
                    val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME, 0L)
                    val currentTimeMillis = SystemClock.uptimeMillis()
                    if (oldTimeMillis == 0L) {
                        //历史时间没有的话,就要重新发验证码
                        toGetCode()
                    } else {
                        when {
                            currentTimeMillis - oldTimeMillis > 60 * 1000 -> {
                                //当前时间超过历史时间1分钟,重新发送
                                toGetCode()
                            }
                            currentTimeMillis < oldTimeMillis -> {
                                //当前时间小于历史时间,说明重新开机过,重新发送短信
                                toGetCode()
                            }
                            else -> {
                                //直接跳转
                                EventBus.getDefault().postSticky(
                                    LoginChangePage(
                                        1,
                                        "+86",
                                        et_login_first_phone.text.toString().trim()
                                    )
                                )
                            }
                        }
                    }
                } else {
                    DialogUtils.showAgreementDialog(
                        requireContext(),
                        false,
                        object : DialogUtils.OnDialogListener {
                            override fun next() {
                                view.cb_login_first?.isChecked = true
                                toGetCode()
                            }
                        }
                    )
                    return@subscribe
                }
            }
        RxView.clicks(view.tv_login_first_account)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                startActivity(Intent(requireContext(), Login4AccountActivity::class.java))
            }

        RxView.clicks(view.tv_login_first_userAgreement)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(activity, WebActivity::class.java)
                intent.putExtra(WebActivity.TYPE, WebActivity.USER_AGREEMENT)
                requireActivity().startActivity(intent)
            }
        RxView.clicks(view.tv_login_first_privacyAgreement)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(activity, WebActivity::class.java)
                intent.putExtra(WebActivity.TYPE, WebActivity.PRIVACY_AGREEMENT)
                startActivity(intent)
            }

//        RxTextView.textChanges(view.et_login_first_phone)
//            .skipInitialValue()
//            .subscribe {
//                if (it.length == 11) {
//                    val isPhone = OtherUtils.isPhone(it.toString())
//                    view.tv_login_first_login.isEnabled = isPhone
//                } else {
//                    view.tv_login_first_login.isEnabled = false
//                }
//            }
    }

    /**
     * 获取短信验证码
     */
    private fun toGetCode() {
        val phone = et_login_first_phone.text.toString().trim()
        DialogUtils.showBeautifulDialog(requireContext())
        val sendCode = RetrofitUtils.builder().sendCode(phone)
        sendCodeObservable = sendCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.CODE_TIME, SystemClock.uptimeMillis())
                            EventBus.getDefault().postSticky(LoginChangePage(1, "+86", phone))
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
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as LoginActivity, it))
            })
    }

    override fun onDestroy() {
        sendCodeObservable?.dispose()
        sendCodeObservable = null
        super.onDestroy()
    }
}