package com.fortune.tejiebox.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.LogUtils
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * 游戏时长
 */
class GameDurationService : IntentService("GameDurationService") {

    private var gameDurationInterval: Disposable? = null
    private var updateGameDurationObservable: Disposable? = null

    companion object {
        private var mGameId: Int? = null

        /**
         * 开始游戏即开始计时
         */
        fun startGame(context: Context, gameId: Int) {
            mGameId = gameId
            val intent = Intent(context, GameDurationService::class.java)
            context.startService(intent)
        }

        fun stopGame(context: Context) {
            val intent = Intent(context, GameDurationService::class.java)
            context.stopService(intent)
        }
    }

    @SuppressLint("CheckResult")
    override fun onHandleIntent(intent: Intent?) {
        val data = VersionBean.getData()
        val time = if (data?.gm_user_fee_duration == null) {
            0
        } else {
            data.gm_user_fee_duration!! * 60
        }
        if(time==0){
            return
        }
        var count = 1
        gameDurationInterval?.dispose()
        gameDurationInterval = null
        gameDurationInterval = Observable.interval(0L, 1L, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count++
                LogUtils.d("===========count:$count")
                if (count >= time) {
                    //在线超过一定时长
                    toUpdateInfo()
                    gameDurationInterval?.dispose()
                    gameDurationInterval = null
                }
            }
    }

    /**
     * 在线2分钟的时候, 上传数据
     */
    private fun toUpdateInfo() {
        updateGameDurationObservable?.dispose()
        updateGameDurationObservable = null
        mGameId?.let {
            val updateGameDuration = RetrofitUtils.builder().updateGameDuration(it)
            updateGameDurationObservable = updateGameDuration.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bean ->
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(bean)}")
                }, { throwInfo ->
                    LogUtils.d("${javaClass.simpleName}=fail=>${throwInfo.message.toString()}")
                })
        }
    }

    override fun stopService(name: Intent?): Boolean {
        LogUtils.d("===========stopService")
        gameDurationInterval?.dispose()
        gameDurationInterval = null

        updateGameDurationObservable?.dispose()
        updateGameDurationObservable = null
        return super.stopService(name)
    }
}