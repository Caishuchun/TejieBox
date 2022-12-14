package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_account_bind.*
import java.util.concurrent.TimeUnit

class AccountBindActivity : BaseActivity() {

    private var signPassIsShow = false
    private var reSignPassIsShow = false

    private var checkAccountObservable: Disposable? = null
    private var accountBindObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: AccountBindActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_account_bind

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)

        initView()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        RxView.clicks(iv_accountBind_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        val account = SPUtils.getString(SPArgument.LOGIN_ACCOUNT, null)
        val pass = SPUtils.getString(SPArgument.LOGIN_ACCOUNT_PASS, null)
        if (!account.isNullOrBlank() && !pass.isNullOrBlank()) {
            ll_accountBind_rePass.visibility = View.GONE
            tv_accountBind_bind.visibility = View.GONE
            et_accountBind_account.isEnabled = false
            et_accountBind_account.setText(account)
            et_accountBind_account.setTextColor(resources.getColor(R.color.gray_C4C4C4))
            et_accountBind_pass.isEnabled = false
            et_accountBind_pass.setText(pass)
            et_accountBind_pass.setTextColor(resources.getColor(R.color.gray_C4C4C4))
        }

        et_accountBind_account?.let { et ->
            RxTextView.textChanges(et_accountBind_account)
                .skipInitialValue()
                .subscribe {
                    if (it.length > 16) {
                        tv_accountBind_account_tips?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = "* ??????????????????16?????????"
                            tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                        }
                    } else {
                        tv_accountBind_account_tips?.visibility = View.INVISIBLE
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        when {
                            et.text.length < 8 -> {
                                tv_accountBind_account_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* ??????????????????8?????????"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            !checkAccountIsOk(et.text.toString()) -> {
                                tv_accountBind_account_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* ??????????????????,???8-16????????????????????????"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            checkAccountIsOk(et.text.toString()) -> {
                                toCheckAccountCanBind(et.text.toString())
                            }
                            et.text.length > 16 -> {
                                tv_accountBind_account_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* ??????????????????16?????????"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                        }
                    }
                }
            }
        }

        et_accountBind_pass?.let { et ->
            if (account.isNullOrBlank() || pass.isNullOrBlank()) {
                RxTextView.textChanges(et)
                    .skipInitialValue()
                    .subscribe {
                        when {
                            it.length in 8..16 -> {
                                tv_accountBind_pass_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "??? ????????????"
                                    tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                                }
                            }
                            it.length > 16 -> {
                                tv_accountBind_pass_tips?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                    tv.text = "* ??????????????????16?????????"
                                    tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                }
                            }
                            else -> {
                                tv_accountBind_pass_tips?.visibility = View.INVISIBLE
                            }
                        }
                    }
                et.setOnFocusChangeListener { v, hasFocus ->
                    if (et.text.isNotEmpty()) {
                        if (!hasFocus) {
                            when {
                                et.text.length < 8 -> {
                                    tv_accountBind_pass_tips?.let { tv ->
                                        tv.visibility = View.VISIBLE
                                        tv.text = "* ??????????????????8?????????"
                                        tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                    }
                                }
                                et.text.length > 16 -> {
                                    tv_accountBind_pass_tips?.let { tv ->
                                        tv.visibility = View.VISIBLE
                                        tv.text = "* ??????????????????16?????????"
                                        tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                                    }
                                }
                                else -> {
                                    tv_accountBind_pass_tips?.let { tv ->
                                        tv.visibility = View.VISIBLE
                                        tv.text = "??? ????????????"
                                        tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        et_accountBind_rePass?.let { et ->
            RxTextView.textChanges(et)
                .skipInitialValue()
                .subscribe {
                    when {
                        it.length < 8 -> {
                            tv_accountBind_rePass_tips?.visibility = View.INVISIBLE
                        }
                        it.toString() == et_accountBind_pass?.text.toString() -> {
                            tv_accountBind_rePass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "??? ????????????????????????"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                        it.toString() != et_accountBind_pass?.text.toString() -> {
                            tv_accountBind_rePass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* ??????????????????????????????"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                        it.length > 16 -> {
                            tv_accountBind_rePass_tips?.let { tv ->
                                tv.visibility = View.VISIBLE
                                tv.text = "* ??????????????????16?????????"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            }
                        }
                    }
                }
            et.setOnFocusChangeListener { v, hasFocus ->
                if (et.text.isNotEmpty()) {
                    if (!hasFocus) {
                        tv_accountBind_rePass_tips?.let { tv ->
                            if (et.text.toString() != et_accountBind_pass?.text.toString()) {
                                tv.visibility = View.VISIBLE
                                tv.text = "* ??????????????????????????????"
                                tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                            } else {
                                tv.visibility = View.VISIBLE
                                tv.text = "??? ????????????????????????"
                                tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                            }
                        }
                    }
                }
            }
        }

        iv_accountBind_pass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    signPassIsShow = !signPassIsShow
                    et_accountBind_pass?.let {
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

        iv_accountBind_rePass?.let { iv ->
            RxView.clicks(iv)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    reSignPassIsShow = !reSignPassIsShow
                    et_accountBind_rePass?.let {
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

        tv_accountBind_bind?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    toBindCheck()
                }
        }
    }


    /**
     * ??????????????????????????????
     */
    private fun toCheckAccountCanBind(account: String) {
        val checkAccount = RetrofitUtils.builder().checkAccount(account)
        checkAccountObservable = checkAccount
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}==>${Gson().toJson(it)}")
                when (it.code) {
                    1 -> {
                        tv_accountBind_account_tips?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = "??? ??????????????????,?????????"
                            tv.setTextColor(resources.getColor(R.color.green_2EC8AC))
                        }
                    }
                    2 -> {
                        tv_accountBind_account_tips?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = "* ?????????????????????,???????????????"
                            tv.setTextColor(resources.getColor(R.color.red_F03D3D))
                        }
                    }
                    else -> {

                    }
                }
            }, {})
    }

    /**
     * ????????????????????????
     */
    private fun checkAccountIsOk(account: String): Boolean {
        val digits4Number = "0123456789"
        val digits4Letter = "abcdefghigklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        //?????????,??????????????????
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
            //???????????????????????????,?????????????????????
            val newAccount = account.substring(1)
            //???????????????????????????????????????
            var isAllNumber = true
            for (char in newAccount) {
                if (char !in digits4Number) {
                    isAllNumber = false
                }
            }
            //??????????????????????????????
            if (isAllNumber) {
                //1. ??????????????????0??????1??????,????????????
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
                //2. ?????????????????????,????????????
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
     * ????????????
     */
    private fun toBindCheck() {
        val accountIsOk =
            tv_accountBind_account_tips?.text.toString().trim().startsWith("???")
                    && tv_accountBind_account_tips?.visibility == View.VISIBLE
        val passIsOk = tv_accountBind_pass_tips?.text.toString().trim()
            .startsWith("???") && tv_accountBind_pass_tips?.visibility == View.VISIBLE
        val rePassIsOk = tv_accountBind_rePass_tips?.text.toString().trim()
            .startsWith("???") && tv_accountBind_rePass_tips?.visibility == View.VISIBLE
        val account = et_accountBind_account?.text.toString().trim()
        val pass = et_accountBind_pass?.text.toString().trim()
        if (accountIsOk && passIsOk && rePassIsOk) {
            toBind(account, pass)
        } else {
            ToastUtils.show("?????????????????????????????????????????????!")
        }
    }

    /**
     * ??????
     */
    private fun toBind(account: String, pass: String) {
        DialogUtils.showBeautifulDialog(this)
        val bingAccount = RetrofitUtils.builder().bingAccount(account, pass)
        accountBindObservable = bingAccount
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                when (it.code) {
                    1 -> {
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT, account)
                        SPUtils.putValue(SPArgument.LOGIN_ACCOUNT_PASS, pass)
                        ToastUtils.show("????????????")
                        finish()
                    }
                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(this)
                    }
                    else -> {
                        ToastUtils.show(it.msg)
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun destroy() {
        checkAccountObservable?.dispose()
        checkAccountObservable = null
        accountBindObservable?.dispose()
        accountBindObservable = null
    }
}