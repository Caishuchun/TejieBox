package com.fortune.tejiebox.myapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.graphics.*
import android.os.Bundle
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.utils.*
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.umcrash.UMCrash

/**
 * Author: 蔡小树
 * Time: 2020/4/14 9:56
 * Description:
 */

class MyApp : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MyApp? = null
        var isBackground = false
        fun getInstance() = instance!!
    }

    private var currentActivity: String = ""

    override fun onCreate() {
        super.onCreate()
        instance = this
        val formatStrategy: FormatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false)
            .tag("TejieBox")
            .build()

        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        CrashHandler.instance!!.init(this)
        UMConfigure.init(
            this, "62bd575605844627b5d180c2", null,
            UMConfigure.DEVICE_TYPE_PHONE, ""
        )
        UMConfigure.setLogEnabled(BaseAppUpdateSetting.isDebug)
        UMCrash.registerUMCrashCallback {
            return@registerUMCrashCallback "UMCrash_TejieBox"
        }
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)
        UMConfigure.setProcessEvent(true)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity.javaClass.simpleName
                if (isBackground) {
                    isBackground = false
                    LogUtils.d("app 跑前台了")
                }
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            isBackground = true
            LogUtils.d("app 跑后台去了")
            val gameTimeInfo = SPUtils.getString(SPArgument.GAME_TIME_INFO)
            if (null != gameTimeInfo && gameTimeInfo.split("-").size >= 2) {
                val split = gameTimeInfo.split("-")
                val gameId = split[0]
                val startTime = split[1]
                val newInfo = "$gameId-$startTime-${System.currentTimeMillis()}"
                LogUtils.d("gameTimeInfo=>$newInfo")
                SPUtils.putValue(SPArgument.GAME_TIME_INFO, newInfo)
            }
        }
    }

    /**
     * 获取当前Activity
     */
    fun getCurrentActivity() = currentActivity

    /**
     * 获取版本,eg:1.2.0
     */
    fun getVersion() = packageManager.getPackageInfo(packageName, 0).versionName!!

    /**
     * 获取版本号.eg:打包的时间戳
     */
    fun getVersionCode() = packageManager.getPackageInfo(packageName, 0).versionCode

    /**
     * 是否登录过了
     */
    fun isHaveToken(): Boolean {
        val loginToken = SPUtils.getString(SPArgument.LOGIN_TOKEN, null)
        return loginToken?.isNotEmpty() == true
    }
}