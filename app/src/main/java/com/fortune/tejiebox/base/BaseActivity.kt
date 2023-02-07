package com.fortune.tejiebox.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadTaskListener
import com.arialyy.aria.core.task.DownloadTask
import com.fortune.tejiebox.utils.ActivityManager
import com.fortune.tejiebox.utils.StatusBarUtils

/**
 * Activity基类
 */

abstract class BaseActivity : AppCompatActivity(), DownloadTaskListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        StatusBarUtils.setTransparent(this)
        ActivityManager.addActivity(this)
        Aria.download(this).register()
        doSomething()

//        FPSUtils.getFps()
    }

    /**
     * 设置layoutId
     */
    abstract fun getLayoutId(): Int

    /**
     * 获取数据初始化等等
     */
    abstract fun doSomething()

    /**
     * 调用onDestroy之后执行
     */
    abstract fun destroy()

    override fun onDestroy() {
        destroy()
        Aria.download(this).unRegister()
        ActivityManager.removeActivity(this)
        super.onDestroy()
    }

    override fun onWait(task: DownloadTask?) {

    }

    override fun onPre(task: DownloadTask?) {
    }

    override fun onTaskPre(task: DownloadTask?) {
    }

    override fun onTaskResume(task: DownloadTask?) {
    }

    override fun onTaskStart(task: DownloadTask?) {
    }

    override fun onTaskStop(task: DownloadTask?) {
    }

    override fun onTaskCancel(task: DownloadTask?) {
    }

    override fun onTaskFail(task: DownloadTask?, e: Exception?) {
    }

    override fun onTaskComplete(task: DownloadTask?) {
    }

    override fun onTaskRunning(task: DownloadTask?) {
    }

    override fun onNoSupportBreakPoint(task: DownloadTask?) {
    }
}