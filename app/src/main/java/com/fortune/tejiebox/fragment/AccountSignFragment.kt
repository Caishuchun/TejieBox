package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.DialogActivity
import com.fortune.tejiebox.activity.Login4AccountActivity
import com.fortune.tejiebox.activity.LoginActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.DialogUtils
import com.fortune.tejiebox.utils.HttpExceptionUtils
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.PromoteUtils
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_account_sign.view.et_account_sign_account
import kotlinx.android.synthetic.main.fragment_account_sign.view.et_account_sign_pass
import kotlinx.android.synthetic.main.fragment_account_sign.view.et_account_sign_rePass
import kotlinx.android.synthetic.main.fragment_account_sign.view.iv_account_sign_back
import kotlinx.android.synthetic.main.fragment_account_sign.view.iv_account_sign_pass
import kotlinx.android.synthetic.main.fragment_account_sign.view.iv_account_sign_rePass
import kotlinx.android.synthetic.main.fragment_account_sign.view.iv_account_sign_title
import kotlinx.android.synthetic.main.fragment_account_sign.view.tv_account_sign_account_tips
import kotlinx.android.synthetic.main.fragment_account_sign.view.tv_account_sign_login
import kotlinx.android.synthetic.main.fragment_account_sign.view.tv_account_sign_pass_tips
import kotlinx.android.synthetic.main.fragment_account_sign.view.tv_account_sign_rePass_tips
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class AccountSignFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = AccountSignFragment()
    }

    private var mView: View? = null
    private var signPassIsShow = false
    private var reSignPassIsShow = false

    private var checkAccountObservable: Disposable? = null
    private var accountSignObservable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_account_sign, container, false)
        initView()
        return mView
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        mView?.iv_account_sign_back?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    Login4AccountActivity.getInstance()?.switchFragment(0)
                }
        }

        mView?.et_account_sign_account?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        mView?.tv_account_sign_account_tips?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = "* 注册账号不得超过16位字符"
                            tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                        }
                    } else {
                        mView?.tv_account_sign_account_tips?.visibility = View.INVISIBLE
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        when {
                            et.text.length < 8 -> {
                                mView?.tv_account_sign_account_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 注册账号不得少于8位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }

                            !checkAccountIsOk(et.text.toString()) -> {
                                mView?.tv_account_sign_account_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 注册账号过于简单,需8-16位数字加字母组合"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }

                            checkAccountIsOk(et.text.toString()) -> {
                                toCheckAccountCanSign(et.text.toString())
                            }

                            et.text.length > 16 -> {
                                mView?.tv_account_sign_account_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 注册账号不得超过16位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                        }
                    }
                }
            }
        }

        mView?.et_account_sign_pass?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    when {
                        it.length in 8..16 -> {
                            mView?.tv_account_sign_pass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 密码可用"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }

                        it.length > 16 -> {
                            mView?.tv_account_sign_pass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 注册密码不得超过16位字符"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }

                        else -> {
                            mView?.tv_account_sign_pass_tips?.visibility = View.INVISIBLE
                        }
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        when {
                            et.text.length < 8 -> {
                                mView?.tv_account_sign_pass_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 注册密码不得少于8位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }

                            et.text.length > 16 -> {
                                mView?.tv_account_sign_pass_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* 注册密码不得超过16位字符"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }

                            else -> {
                                mView?.tv_account_sign_pass_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "√ 密码可用"
                                    tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                                }
                            }
                        }
                    }
                }
            }
        }

        mView?.et_account_sign_rePass?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    when {
                        it.length < 8 -> {
                            mView?.tv_account_sign_rePass_tips?.visibility = View.INVISIBLE
                        }

                        it.toString() == mView?.et_account_sign_pass?.text.toString() -> {
                            mView?.tv_account_sign_rePass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 两次输入密码一致"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }

                        it.toString() != mView?.et_account_sign_pass?.text.toString() -> {
                            mView?.tv_account_sign_rePass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 两次输入的密码不一致"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }

                        it.length > 16 -> {
                            mView?.tv_account_sign_rePass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* 注册密码不得超过16位字符"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        mView?.tv_account_sign_rePass_tips?.let { tv ->
                            if (et.text.toString() != mView?.et_account_sign_pass?.text.toString()) {
                                tv.visibility = View.VISIBLE
                                tv.text = "* 两次输入的密码不一致"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            } else {
                                tv.visibility = View.VISIBLE
                                tv.text = "√ 两次输入密码一致"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                    }
                }
            }
        }

        mView?.iv_account_sign_pass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    signPassIsShow = !signPassIsShow
                    mView?.et_account_sign_pass?.let {
                        it.transformationMethod = if (signPassIsShow) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        it.setSelection(it.length())
                    }
                    iv.setImageResource(if (!signPassIsShow) R.mipmap.pass_show else R.mipmap.pass_unshow)
                }
        }

        mView?.iv_account_sign_rePass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    reSignPassIsShow = !reSignPassIsShow
                    mView?.et_account_sign_rePass?.let {
                        it.transformationMethod = if (reSignPassIsShow) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        it.setSelection(it.length())
                    }
                    iv.setImageResource(if (!reSignPassIsShow) R.mipmap.pass_show else R.mipmap.pass_unshow)
                }
        }

        mView?.iv_account_sign_title?.let {
            it.setImageResource(if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.app_title2 else R.mipmap.app_title)
        }

        mView?.tv_account_sign_login?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    toSignCheck()
                }
        }
    }

    /**
     * 检查该账号是否已注册
     */
    private fun toCheckAccountCanSign(account: String) {
        val checkAccount = RetrofitUtils.builder().checkAccount(account)
        checkAccountObservable = checkAccount
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}==>${Gson().toJson(it)}")
                when (it.code) {
                    1 -> {
                        mView?.tv_account_sign_account_tips?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = "√ 注册账号未被使用,可注册"
                            tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                        }
                    }

                    2 -> {
                        mView?.tv_account_sign_account_tips?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = "* 该账号已被注册,请重新输入"
                            tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                        }
                    }

                    else -> {

                    }
                }
            }, {})
    }

    /**
     * 检查账号是否合规
     */
    private fun checkAccountIsOk(account: String): Boolean {
        val digits4Number = "0123456789"
        val digits4Letter = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        //纯数字,纯字母都不行
        var isHaveNumber = false
        var isHaveLetter = false
        for (char in account) {
            if (char in digits4Number) {
                isHaveNumber = true
            }
            if (char in digits4Letter) {
                isHaveLetter = true
            }
        }
        if (!isHaveNumber || !isHaveLetter) {
            return false
        }
        if (account[0] in digits4Letter) {
            //如果首个字符是字母,截取剩下的账号
            val newAccount = account.substring(1)
            //判断之后的字符是否是纯数字
            var isAllNumber = true
            for (char in newAccount) {
                if (char !in digits4Number) {
                    isAllNumber = false
                }
            }
            //如果之后字符为纯数字
            if (isAllNumber) {
                //1. 如果数字是从0或者1递增,则不可用
                var isOk = true
                var startNum = newAccount[0]
                if (startNum == '0' || startNum == '1') {
                    for (char in newAccount.substring(1)) {
                        startNum = (startNum + 1) as Char
                        if (startNum == char) {
                            isOk = false
                        } else {
                            isOk = true
                            break
                        }
                    }
                }
                if (!isOk) {
                    return false
                }
                //2. 如果数字都一样,则不可用
                startNum = newAccount[0]
                for (char in newAccount.substring(1)) {
                    if (startNum == char) {
                        isOk = false
                    } else {
                        isOk = true
                        break
                    }
                }
                return isOk
            }
        }
        return true
    }

    /**
     * 注册检查
     */
    private fun toSignCheck() {
        val accountIsOk =
            mView?.tv_account_sign_account_tips?.text.toString().trim().startsWith("√")
                    && mView?.tv_account_sign_account_tips?.visibility == View.VISIBLE
        val passIsOk = mView?.tv_account_sign_pass_tips?.text.toString().trim()
            .startsWith("√") && mView?.tv_account_sign_pass_tips?.visibility == View.VISIBLE
        val rePassIsOk = mView?.tv_account_sign_rePass_tips?.text.toString().trim()
            .startsWith("√") && mView?.tv_account_sign_rePass_tips?.visibility == View.VISIBLE
        val account = mView?.et_account_sign_account?.text.toString().trim()
        val pass = mView?.et_account_sign_pass?.text.toString().trim()
        if (accountIsOk && passIsOk && rePassIsOk) {
            toSign(account, pass)
        } else {
            ToastUtils.show("请检查账号密码是否合规后再进行注册!")
        }
    }

    /**
     * 注册
     */
    private fun toSign(account: String, pass: String) {
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, null)
        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, null)
        SPUtils.putValue(SPArgument.USER_ID, null)
        SPUtils.putValue(SPArgument.USER_ID_NEW, null)
        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
        SPUtils.putValue(SPArgument.ID_NAME, null)
        SPUtils.putValue(SPArgument.ID_NUM, null)
        DialogUtils.showBeautifulDialog(requireContext())
        val accountSign = RetrofitUtils.builder().accountSign(account, pass)
        accountSignObservable = accountSign.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("success=>${Gson().toJson(it)}")
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT, true)
                        SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                        SPUtils.putValue(SPArgument.PHONE_NUMBER, it.data?.phone)
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, it.data?.account)
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, pass)
                        SPUtils.putValue(SPArgument.USER_ID, it.data?.user_id)
                        SPUtils.putValue(SPArgument.USER_ID_NEW, it.data?.user_id_raw)
                        SPUtils.putValue(SPArgument.IS_HAVE_ID, it.data?.id_card)
                        if (it.data?.id_card == 1) {
                            SPUtils.putValue(SPArgument.ID_NAME, it.data?.card_name)
                            SPUtils.putValue(SPArgument.ID_NUM, it.data?.car_num)
                        }

                        // 是否有奖励积分可以弹框
                        var isHaveRewardInteger = false
                        if (it.data?.first_login == 1) {
                            // 首次注册的推广统计
//                            if (BaseAppUpdateSetting.isToPromoteVersion) {
                            PromoteUtils.promote(requireActivity())
//                            }
                            // 打电话推广, 首次注册且有奖励积分的
//                            if (it.data?.integral != null && it.data?.integral!! > 0) {
//                                isHaveRewardInteger = true
//                                DialogActivity.showGetIntegral(
//                                    requireActivity(),
//                                    it.data?.integral!!,
//                                    true,
//                                    null
//                                )
//                            }
                        }

                        EventBus.getDefault().postSticky(
                            LoginStatusChange(
                                true,
                                it.data?.phone,
                                it.data?.account,
                                isHaveRewardInteger
                            )
                        )
                        toFinishAllLogin()
                    }

                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    /**
     * 关闭所有登录相关
     */
    private fun toFinishAllLogin() {
        LoginActivity.getInstance()?.finish()
        requireActivity().finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        checkAccountObservable?.dispose()
        checkAccountObservable = null
        accountSignObservable?.dispose()
        accountSignObservable = null
    }
}