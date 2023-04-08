package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * 时长统计工具类
 */
@SuppressLint("CheckResult")
object DurationUtils {

    private var durationObservable: Disposable? = null
    private var updateGameDurationObservable: Disposable? = null

    /**
     * 开始计时
     */
    fun startTiming(gameId: Int) {
        durationObservable?.dispose()
        durationObservable = null
        if (gameId > 10000) {
            return
        }
        val data = VersionBean.getData()
        val allTime = try {
            data?.gm_user_fee_duration?.times(60)
        } catch (e: Exception) {
            -1
        }
        LogUtils.d("计时器==>$allTime")
        val gameTimeInfo = SPUtils.getString(SPArgument.GAME_TIME_INFO) ?: return
        val split = gameTimeInfo.split("-")
        if (split.size < 2) return
        val savedGameId = try {
            split[0].toInt()
        } catch (e: Exception) {
            return
        }
        if (savedGameId != gameId) return
        val startTime = split[1]
        durationObservable = Observable.interval(0, 10, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val gameInfo = "$gameId-$startTime-${System.currentTimeMillis()}"
                LogUtils.d("计时器==>${it * 10}:$gameInfo")
                SPUtils.putValue(SPArgument.GAME_TIME_INFO, gameInfo)
                if (it.toInt() * 10 == allTime) {
                    toUpdateInfo(gameId)
                }
                if (it / 6 > 0 && it.toInt() % 6 == 0) {
                    //10s一次,6次就是一分钟,每分钟发个请求
                    LogUtils.d("计时器==>${it / 6}分钟了")
                }
            }
    }

    /**
     * 在线指定的时间, 上传数据
     */
    private fun toUpdateInfo(gameId: Int) {
        updateGameDurationObservable?.dispose()
        updateGameDurationObservable = null
        val updateGameDuration = RetrofitUtils.builder().updateGameDuration(gameId)
        updateGameDurationObservable = updateGameDuration.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bean ->
                LogUtils.d("计时器==>玩够规定时长=success=>${Gson().toJson(bean)}")
            }, { throwInfo ->
                LogUtils.d("计时器==>玩够规定时长=fail=>${throwInfo.message.toString()}")
            })
    }

    /**
     * 结束计时
     */
    fun stopTiming() {
        durationObservable?.dispose()
        durationObservable = null
    }
}