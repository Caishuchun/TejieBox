package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.text.Html
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.bean.GameInfo4ClipboardBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.StatusBarUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_invite_code.*
import java.util.concurrent.TimeUnit

class InviteCodeActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: InviteCodeActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_invite_code
    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)
        SPUtils.putValue(SPArgument.INVITE_CODE_USED, false)
        SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_JUMP, -1)
        SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_UPDATE, -1)
        GameInfo4ClipboardBean.setData(null)
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_inviteCode_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toMain()
            }

        RxView.clicks(tv_inviteCode_sure)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toCheckInviteCode()
            }

        tv_inviteCode_jump.text = Html.fromHtml("<u>不重要, 立即开玩</u>")
        RxView.clicks(tv_inviteCode_jump)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toMain()
            }
    }

    /**
     * 校验邀请码
     */
    private fun toCheckInviteCode() {
        val isInviteCodeOk = et_inviteCode_code.text.length == 6
        if (isInviteCodeOk) {
            toCheckInviteCode2(et_inviteCode_code.text.toString())
        } else {
            ToastUtils.show("请填写有效邀请码")
        }
    }

    /**
     * 网络校验验证码真实性
     */
    private fun toCheckInviteCode2(inviteCode: String) {
        SPUtils.putValue(SPArgument.INVITE_CODE_USED, true)
    }

    override fun onBackPressed() {
//        super.onBackPressed()
//        toMain()
        return
    }

    private fun toMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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
    }
}