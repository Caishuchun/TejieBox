package com.fortune.tejiebox.utils

import android.app.Activity
import android.content.Intent
import com.fortune.tejiebox.activity.MainActivity
import com.fortune.tejiebox.activity.SplashActivity
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.room.*
import org.greenrobot.eventbus.EventBus
import kotlin.system.exitProcess

/**
 * Author: 蔡小树
 * Time: 2020/4/14 9:42
 * Description: Activity管理工具类
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
        EventBus.getDefault().postSticky(LoginStatusChange(false,null))
        VersionBean.clear()
        SPUtils.clear()
        SearchHisDataBase.getDataBase(activity).searchHisDao().deleteAll()
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)
        val intent = Intent(activity, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * 退出登录
     */
    fun exitLogin(activity: MainActivity) {
        VersionBean.clear()
        SPUtils.clear()
        SearchHisDataBase.getDataBase(activity).searchHisDao().deleteAll()
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)
        EventBus.getDefault().postSticky(LoginStatusChange(false,null))
    }

    /**
     * 退出app
     */
    fun exit() {
        exitProcess(0)
    }

}