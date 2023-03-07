package com.fortune.tejiebox.utils

import android.app.Activity
import android.content.Intent
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.activity.NewSplashActivity
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.room.CustomerServiceInfoDataBase
import com.fortune.tejiebox.room.SearchHisDataBase
import org.greenrobot.eventbus.EventBus
import kotlin.system.exitProcess

/**
 * Activity管理工具类
 */

object ActivityManager {

    private var activities = mutableListOf<Activity>()

    /**
     * 添加Activity到集合中
     */
    fun addActivity(activity: Activity) {
        activities.add(activity)
    }

    /**
     * 从集合中移除Activity
     */
    fun removeActivity(activity: Activity) {
        if (activities.contains(activity)) {
            activities.remove(activity)
        }
    }

    /**
     * 跳转到主界面
     */
    fun toMainActivity() {
        val stepList = mutableListOf<Activity>()
        for (index in 0 until activities.size) {
            if (activities[index] != MainActivity.getInstance()) {
                activities[index].finish()
                stepList.add(activities[index])
            }
        }
        for (activity in stepList) {
            activities.remove(activity)
        }
    }

    /**
     * 退到起始页面
     */
    fun toSplashActivity(activity: Activity) {
        EventBus.getDefault().postSticky(LoginStatusChange(false, null, null))
        VersionBean.clear()
        val channel = SPUtils.getString(SPArgument.UM_CHANNEL_ID, null)
        SPUtils.clear()
        SPUtils.putValue(SPArgument.UM_CHANNEL_ID, channel)
        SearchHisDataBase.getDataBase(activity).searchHisDao().deleteAll()
        CustomerServiceInfoDataBase.getDataBase(activity).customerServiceInfoDao().clear()
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)
        val intent = Intent(activity, NewSplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * 退出登录
     */
    fun exitLogin(activity: MainActivity) {
        VersionBean.clear()
        val channel = SPUtils.getString(SPArgument.UM_CHANNEL_ID, null)
        SPUtils.clear()
        SPUtils.putValue(SPArgument.UM_CHANNEL_ID, channel)
        SearchHisDataBase.getDataBase(activity).searchHisDao().deleteAll()
        CustomerServiceInfoDataBase.getDataBase(activity).customerServiceInfoDao().clear()
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)
        EventBus.getDefault().postSticky(LoginStatusChange(false, null, null))
    }

    /**
     * 退出app
     */
    fun exit() {
        exitProcess(0)
    }

}