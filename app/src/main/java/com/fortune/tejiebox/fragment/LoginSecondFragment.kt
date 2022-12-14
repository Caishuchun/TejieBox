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
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.LoginActivity
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginChangePage
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login_second.*
import kotlinx.android.synthetic.main.fragment_login_second.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class LoginSecondFragment() : Fragment() {

    private var areaCode: String? = null
    private var phone: String? = null

    private var sendCodeObservable: Disposable? = null
    private var loginObservable: Disposable? = null
    private var timer: Disposable? = null

    private var lastTime = 59

    companion object {
        fun newInstance(areaCode: String, phone: String) = LoginSecondFragment().apply {
            arguments = Bundle().apply {
                putString(AREA_CODE, areaCode)
                putString(PHONE, phone)
            }
        }

        const val AREA_CODE = "area_code"
        const val PHONE = "phone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            areaCode = it.getString(AREA_CODE)
            phone = it.getString(PHONE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login_second, container, false)
        initView(view)
        val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME, 0L)
        val currentTimeMillis = SystemClock.uptimeMillis()
        if (oldTimeMillis == 0L) {
            //????????????????????????,?????????????????????
        } else {
            when {
                currentTimeMillis - oldTimeMillis > 60 * 1000 -> {
                    //??????????????????????????????1??????,???????????????
                }
                currentTimeMillis < oldTimeMillis -> {
                    //??????????????????????????????,?????????????????????,???????????????
                }
                else -> {
                    //????????????????????????,?????????
                    lastTime -= ((currentTimeMillis - oldTimeMillis) / 1000).toInt()
                }
            }
        }
        toShowTime(view)
        return view
    }

    /**
     * ?????????????????????
     */
    @SuppressLint("SetTextI18n")
    private fun toShowTime(view: View) {
        timer?.dispose()
        timer = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (!MyApp.isBackground) {
                    if (lastTime > 0) {
                        view.tv_login_second_reSend.isEnabled = false
                        view.tv_login_second_reSend.text =
                            "${MyApp.getInstance().getString(R.string.resend)}(${lastTime}s)"
                        lastTime--
                    } else {
                        lastTime = 59
                        timer?.dispose()
                        view.tv_login_second_reSend.isEnabled = true
                        view.tv_login_second_reSend.text =
                            MyApp.getInstance().getString(R.string.resend)
                    }
                }
            }, {})
    }


    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView(view: View) {
        view.tv_login_second_phone.text =
            "${getString(R.string.send_to)} $areaCode $phone"

        RxView.clicks(view.iv_login_second_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                EventBus.getDefault().postSticky(LoginChangePage(2, null, null))
            }
        RxView.clicks(view.tv_login_second_reSend)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toGetCode(view)
            }

        RxTextView.textChanges(view.et_login_second_code)
            .skipInitialValue()
            .subscribe {
                changeCodeBg(it.toString(), view)
                if (it.length == 6) {
                    toLogin(it.toString())
                }
            }
    }

    /**
     * ??????????????????
     */
    private fun toLogin(code: String) {
        DialogUtils.showBeautifulDialog(requireContext())
        val login = RetrofitUtils.builder().login(phone!!, code.toInt())
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, null)
        SPUtils.putValue(SPArgument.USER_ID, null)
        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
        SPUtils.putValue(SPArgument.ID_NAME, null)
        SPUtils.putValue(SPArgument.ID_NUM, null)
        loginObservable = login.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success==>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT, true)
                            SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                            SPUtils.putValue(SPArgument.PHONE_NUMBER, it.data?.phone)
                            SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, it.data?.account)
                            SPUtils.putValue(SPArgument.IS_HAVE_ID, it.data?.id_card)
                            SPUtils.putValue(SPArgument.USER_ID, it.data?.user_id)
                            if (it.data?.id_card == 1) {
                                SPUtils.putValue(SPArgument.ID_NAME, it.data?.card_name)
                                SPUtils.putValue(SPArgument.ID_NUM, it.data?.car_num)
                            }

                            // ?????????????????????????????????
                            var isHaveRewardInteger = false
                            if (it.data?.first_login == 1) {
                                // ???????????????????????????
                                if (BaseAppUpdateSetting.isToPromoteVersion) {
                                    PromoteUtils.promote(requireActivity())
                                }
                                // ?????????????????????????????????
                                if (it.data?.integral != null && it.data?.integral!! > 0) {
                                    isHaveRewardInteger = true
                                    DialogActivity.showGetIntegral(
                                        requireActivity(),
                                        it.data?.integral!!,
                                        true,
                                        null
                                    )
                                }
                            }
                            EventBus.getDefault().postSticky(
                                LoginStatusChange(
                                    true,
                                    it.data?.phone,
                                    it.data?.account,
                                    isHaveRewardInteger
                                )
                            )
                            (activity as LoginActivity).finish()
                        }
                        else -> {
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                            et_login_second_code.setText("")
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

    /**
     * ??????????????????
     */
    private fun toMain() {
        SPUtils.putValue(SPArgument.IS_LOGIN, true)
        startActivity(Intent(activity, MainActivity::class.java))
        activity?.finish()
    }

    /**
     * ???????????????????????????
     */
    private fun changeCodeBg(code: String, view: View) {
        view.tv_code_1.setBackgroundResource(R.drawable.bg_code_unenter)
        view.tv_code_2.setBackgroundResource(R.drawable.bg_code_unenter)
        view.tv_code_3.setBackgroundResource(R.drawable.bg_code_unenter)
        view.tv_code_4.setBackgroundResource(R.drawable.bg_code_unenter)
        view.tv_code_5.setBackgroundResource(R.drawable.bg_code_unenter)
        view.tv_code_6.setBackgroundResource(R.drawable.bg_code_unenter)
        view.tv_code_1.text = ""
        view.tv_code_2.text = ""
        view.tv_code_3.text = ""
        view.tv_code_4.text = ""
        view.tv_code_5.text = ""
        view.tv_code_6.text = ""
        when (code.length) {
            0 -> {
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entering)
            }
            1 -> {
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_entering)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
            }
            2 -> {
                view.tv_code_3.setBackgroundResource(R.drawable.bg_code_entering)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
            }
            3 -> {
                view.tv_code_4.setBackgroundResource(R.drawable.bg_code_entering)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
            }
            4 -> {
                view.tv_code_5.setBackgroundResource(R.drawable.bg_code_entering)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_4.text = code[3].toString()
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_4.setBackgroundResource(R.drawable.bg_code_entered)
            }
            5 -> {
                view.tv_code_6.setBackgroundResource(R.drawable.bg_code_entering)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_4.text = code[3].toString()
                view.tv_code_5.text = code[4].toString()
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_4.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_5.setBackgroundResource(R.drawable.bg_code_entered)
            }
            6 -> {
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_4.text = code[3].toString()
                view.tv_code_5.text = code[4].toString()
                view.tv_code_6.text = code[5].toString()
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_3.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_4.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_5.setBackgroundResource(R.drawable.bg_code_entered)
                view.tv_code_6.setBackgroundResource(R.drawable.bg_code_entered)
            }
        }
    }

    /**
     * ?????????????????????
     */
    private fun toGetCode(view: View) {
        DialogUtils.showBeautifulDialog(requireContext())
        val sendCode = RetrofitUtils.builder().sendCode(phone!!)
        sendCodeObservable = sendCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            toShowTime(view)
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
        timer?.dispose()
        sendCodeObservable?.dispose()
        loginObservable?.dispose()

        timer = null
        sendCodeObservable = null
        loginObservable = null
        super.onDestroy()
    }
}