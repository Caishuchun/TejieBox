package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.text.Html
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.IsHaveIdChange
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import com.unity3d.player.JumpUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_id_card.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class IdCardActivity : BaseActivity() {

    //0 来自我的界面未认证 1 实名认证过了 2 来自启动游戏时未认证
    private var from = 0
    private var gameId: Int? = null
    private var gameChannel: String? = null
    private var isNameOver = false
    private var isIdOver = false
    private var idCardObservable: Disposable? = null
    private var addPlayingGameObservable: Disposable? = null
    private var isPlayingGame = false
    private var updateGameTimeInfoObservable: Disposable? = null

    private var gameStyle: String? = null

    private var account: String? = null
    private var password: String? = null
    private var isIntegral = 0

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: IdCardActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val FROM = "from"
        const val GAME_ID = "gameId"
        const val GAME_CHANNEL = "gameChannel"
        const val GAME_STYLE = "gameStyle"

        const val ACCOUNT = "account"
        const val PASSWORD = "password"
        const val GAME_IS_INTEGRAL = "game_is_integral" //游戏是否是置顶游戏
    }

    override fun getLayoutId() = R.layout.activity_id_card

    @SuppressLint("CheckResult")
    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)

        RxView.clicks(iv_idCard_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        isIntegral = intent.getIntExtra(GAME_IS_INTEGRAL, 0)
        from = intent.getIntExtra(FROM, 0)
        gameId = intent.getIntExtra(GAME_ID, -1)
        gameChannel = intent.getStringExtra(GAME_CHANNEL)
        gameStyle = intent.getStringExtra(GAME_STYLE)

        account = intent.getStringExtra(ACCOUNT)
        password = intent.getStringExtra(PASSWORD)
        when (from) {
            1 -> {
                //用来展示
                et_idCard_name.isEnabled = false
                et_idCard_id.isEnabled = false

                et_idCard_name.setText(SPUtils.getString(SPArgument.ID_NAME))
                et_idCard_id.setText(SPUtils.getString(SPArgument.ID_NUM))

                btn_idCard_issue.visibility = View.GONE
            }
            else -> {
                //需要填写
                et_idCard_name.requestFocus()
                if (from == 2) {
                    tv_idCard_next.text = Html.fromHtml("<u>下次再说</U>")
                    RxView.clicks(tv_idCard_next)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toStartGame()
                        }
                }

                RxTextView.textChanges(et_idCard_name)
                    .skipInitialValue()
                    .subscribe {
                        isNameOver = it.length >= 2
                        if (isNameOver && isIdOver) {
                            btn_idCard_issue.setBackgroundResource(R.drawable.bg_start_btn_big)
                            btn_idCard_issue.isEnabled = true
                        } else {
                            btn_idCard_issue.isEnabled = false
                            btn_idCard_issue.setBackgroundResource(R.drawable.bg_game_shade)
                        }
                    }

                RxTextView.textChanges(et_idCard_id)
                    .skipInitialValue()
                    .subscribe {
                        isIdOver = it.length == 18
                        if (isNameOver && isIdOver) {
                            btn_idCard_issue.setBackgroundResource(R.drawable.bg_start_btn_big)
                            btn_idCard_issue.isEnabled = true
                        } else {
                            btn_idCard_issue.isEnabled = false
                            btn_idCard_issue.setBackgroundResource(R.drawable.bg_game_shade)
                        }
                    }

                RxView.clicks(btn_idCard_issue)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toCertif(
                            et_idCard_name.text.toString().trim(),
                            et_idCard_id.text.toString().trim()
                        )
                    }
            }
        }
    }

    /**
     * 实名认证
     */
    private fun toCertif(name: String, idNum: String) {
        DialogUtils.showBeautifulDialog(this)
        val idCard = RetrofitUtils.builder().idCard(name, idNum)
        idCardObservable = idCard.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success==>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.IS_HAVE_ID, 1)
                            SPUtils.putValue(SPArgument.ID_NAME, name)
                            SPUtils.putValue(SPArgument.ID_NUM, idNum)
                            et_idCard_id.isEnabled = false
                            et_idCard_name.isEnabled = false
                            btn_idCard_issue.visibility = View.GONE
                            EventBus.getDefault().postSticky(IsHaveIdChange(1))
                            when (from) {
                                0 -> {
                                    ToastUtils.show("实名认证成功!")
                                }
                                2 -> {
                                    ToastUtils.show("实名认证成功,即将启动游戏!")
                                    toStartGame()
                                }
                            }
                        }
                        else -> {
                            it.msg.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 开始玩游戏
     */
    private fun toStartGame() {
        DialogUtils.showBeautifulDialog(this)
        val addPlayingGame = RetrofitUtils.builder().addPlayingGame(gameId!!, 1, isIntegral)
        addPlayingGameObservable = addPlayingGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            //起个子线程的页面
                            startActivity(Intent(this, ProcessActivity::class.java))

                            EventBus.getDefault().post(PlayingDataChange(""))
                            if (gameId!! < 10000) {
                                SPUtils.putValue(
                                    SPArgument.GAME_TIME_INFO,
                                    "$gameId-${System.currentTimeMillis()}"
                                )
                                DurationUtils.startTiming(gameId!!)
                            }
                            isPlayingGame = true
                            if (account == null) {
                                JumpUtils.jump2Game(
                                    this,
                                    gameChannel + Box2GameUtils.getPhoneAndToken(),
                                    gameStyle
                                )
                            } else {
                                JumpUtils.jump2Game(
                                    this,
                                    "$gameChannel|phone=$account|token=$password|logintype=2",
                                    null
                                )
                            }
//                            finish()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun destroy() {
        DurationUtils.stopTiming()

        idCardObservable?.dispose()
        idCardObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null

        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        if (isPlayingGame) {
            isPlayingGame = false
            LogUtils.d("=====退出游戏")
            DurationUtils.stopTiming()
            val gameTimeInfo = SPUtils.getString(SPArgument.GAME_TIME_INFO)
            if (null != gameTimeInfo && gameTimeInfo.split("-").size >= 2) {
                val split = gameTimeInfo.split("-")
                val gameId = split[0].toInt()
                val startTime = split[1].toLong()
                val endTime = System.currentTimeMillis()
//                if (endTime - startTime >= 1 * 60 * 1000) {
                val updateGameTimeInfo = RetrofitUtils.builder().updateGameTimeInfo(
                    gameId,
                    startTime.toString(),
                    endTime.toString()
                )
                updateGameTimeInfoObservable = updateGameTimeInfo.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({}, {})
//                }
            }
        }
        SPUtils.putValue(SPArgument.GAME_TIME_INFO, null)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}