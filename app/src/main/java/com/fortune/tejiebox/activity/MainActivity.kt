package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.manager.SubTaskManager
import com.arialyy.aria.core.task.DownloadTask
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.*
import com.fortune.tejiebox.fragment.*
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.utils.ActivityManager
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search_game.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {

    private var mainFragment: GameFragment? = null
    private var playingFragment: GameFragment? = null
    private var likeFragment: GameFragment? = null
    private var mineFragment: MineFragment? = null

    private var canQuit = false
    var currentFragment: Fragment? = null
    private var downloadPath = "" //下载的安装路径
    private var isDownloadApp = false //是否在下载app
    private var updateGameTimeInfoObservable: Disposable? = null
    private var canGetIntegralObservable: Disposable? = null

    private var intentFilter: IntentFilter? = null
    private var timeChangeReceiver: TimeChangeReceiver? = null

    private var splashUrlList = mutableListOf<String>()
    private var getSplashUrlObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: MainActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        var mainPage: MainPage = MainPage.MAIN
    }

    @SuppressLint("SimpleDateFormat")
    class TimeChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK -> {
                    if (MyApp.getInstance().isHaveToken()) {
                        val currentTimeMillis = System.currentTimeMillis()
                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        val currentTime = df.format(currentTimeMillis)
                        val hour = currentTime.split(" ")[1].split(":")[0]
                        val minute = currentTime.split(" ")[1].split(":")[1]
                        if ((hour == "05" || hour == "10" || hour == "15" || hour == "20") && minute == "00") {
                            LogUtils.d("=======$hour:$minute")
                            EventBus.getDefault().postSticky(RedPointChange(true))
                            //好巧不巧,正好处于白嫖界面等着的话,需要通知白嫖获取新数据
                            if (MyApp.getInstance().getCurrentActivity() == "GiftActivity") {
                                EventBus.getDefault().post(
                                    GiftNeedNewInfo(
                                        isShowDailyCheckNeed = false,
                                        isShowWhitePiaoNeed = true,
                                        isShowInviteGiftNeed = false
                                    )
                                )
                            }
                        }
                        //如果更巧的话,在晚上12点卡点,礼包三个界面都需要重新获取数据
                        if (hour == "00" && minute == "00") {
                            EventBus.getDefault().postSticky(RedPointChange(true))
                            if (MyApp.getInstance().getCurrentActivity() == "GiftActivity") {
                                EventBus.getDefault().post(
                                    GiftNeedNewInfo(
                                        isShowDailyCheckNeed = true,
                                        isShowWhitePiaoNeed = true,
                                        isShowInviteGiftNeed = true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    enum class MainPage {
        MAIN, PLAYING, LIKE, ME
    }

    override fun getLayoutId() = R.layout.activity_main

    //为了不保存Fragment,直接清掉
    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
    }

    /**
     * 双击退出
     */
    @SuppressLint("CheckResult")
    override fun onBackPressed() {
        if (canQuit) {
            MobclickAgent.onKillProcess(this)
            super.onBackPressed()
        } else {
            ToastUtils.show(getString(R.string.double_click_quit))
            canQuit = true
            Observable.timer(2, TimeUnit.SECONDS)
                .subscribe {
                    canQuit = false
                }
        }
    }

    @SuppressLint("CheckResult")
    override fun doSomething() {
        instance = this
        EventBus.getDefault().register(this)
        StatusBarUtils.setTextDark(this, true)
        Aria.download(this).register()

        mainFragment = GameFragment.newInstance(0)

        initView()

        intentFilter = IntentFilter()
        intentFilter?.addAction(Intent.ACTION_TIME_TICK)
        if (timeChangeReceiver == null) {
            timeChangeReceiver = TimeChangeReceiver()
        }
        registerReceiver(timeChangeReceiver, intentFilter)

        ll_shade_root.postDelayed({
            if (MyApp.getInstance().isHaveToken()) {
                toCheckCanGetIntegral()
            }
        }, 1000)
    }

    /**
     * 检查是否能够领取奖励
     */
    private fun toCheckCanGetIntegral() {
        val canGetIntegral = RetrofitUtils.builder().canGetIntegral()
        canGetIntegralObservable = canGetIntegral.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            val data = it.data
                            if (null != data) {
                                if (data.daily_clock_in == 1 || data.limit_time == 1 || data.invite == 1) {
                                    //取消了这里进入App后主动弹出余额页面的功能
//                                    if (null != data.is_click && data.is_click == 0) {
//                                        startActivity(Intent(this, GiftActivity::class.java))
//                                    }
                                    EventBus.getDefault().postSticky(RedPointChange(true))
                                    tab_main.showRedPoint(true)
                                } else {
                                    EventBus.getDefault().postSticky(RedPointChange(false))
                                    tab_main.showRedPoint(false)
                                }
                            }
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * apk下载
     */
    private fun toDownloadApk(updateUrl: String) {
        isDownloadApp = true
        Aria.download(this)
            .load(updateUrl) //读取下载地址
            .setFilePath(downloadPath, true) //设置文件保存的完整路径
            .ignoreFilePathOccupy()
            .ignoreCheckPermissions()
            .create()
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        if (mainFragment == null && fragment is GameFragment && mainPage == MainPage.MAIN) {
            mainFragment = fragment
        } else if (playingFragment == null && fragment is GameFragment && mainPage == MainPage.PLAYING) {
            playingFragment = fragment
        } else if (likeFragment == null && fragment is GameFragment && mainPage == MainPage.LIKE) {
            likeFragment = fragment
        } else if (mineFragment == null && fragment is MineFragment) {
            mineFragment = fragment
        }
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fl_main, mainFragment!!)
            .commit()

        val data = VersionBean.getData()
        if (data != null) {
            val newVersion = data.version_name!!.replace(".", "").toInt()
            val currentVersion = MyApp.getInstance().getVersion().replace(".", "").toInt()
            LogUtils.d("toDownLoadApk==>newVersion = $newVersion, currentVersion = $currentVersion")
            //获取实际的下载地址
            val updateUrl = if (BaseAppUpdateSetting.isToPromoteVersion) {
                if (data.update_url2 == null || data.update_url2?.isEmpty() == true) {
                    data.update_url!!
                } else {
                    data.update_url2!!
                }
            } else {
                data.update_url!!
            }
            val versionName = updateUrl.substring(updateUrl.lastIndexOf("/") + 1)
            downloadPath =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + versionName
            SPUtils.putValue(SPArgument.APP_DOWNLOAD_PATH, downloadPath)
            if (newVersion > currentVersion) {
                SPUtils.putValue(SPArgument.IS_NEED_UPDATE_DIALOG, true)
                //需要更新的话,直接更新
                if (isApkDownload(File(downloadPath))) {
                    installAPK(File(downloadPath))
                } else {
                    toDownloadApk(updateUrl)
                }
            } else {
                if (isApkDownload(File(downloadPath))) {
                    toDeleteApk()
                }
                //已经更新过,默认的当然是不显示的
                val isNeedUpdateDialog = SPUtils.getBoolean(SPArgument.IS_NEED_UPDATE_DIALOG, false)
                if (isNeedUpdateDialog) {
                    SPUtils.putValue(SPArgument.IS_NEED_UPDATE_DIALOG, false)
                    VersionDialog.show(
                        this@MainActivity,
                        data.update_msg.toString(),
                        object : VersionDialog.OnUpdateAPP {
                            override fun onUpdate() {
                            }
                        }
                    )
                }
                toGetSplashImgUrl()
            }
        }

        RxView.clicks(tv_know)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                SPUtils.putValue(SPArgument.IS_NEED_SHADE_NEW, false)
                ll_shade_root.visibility = View.GONE
            }

        RxView.clicks(ll_shade_root)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //作用就是,禁止点击遮罩层下面的东西
            }

        tab_main.setCurrentItem(0)
        toChangeFragment(0)

        tab_main.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index)
            }
        })

        /**
         * 当进入APP,检查剪贴板的数据,如果数据对上的话,跳转到相应的游戏详情页
         */
        window.decorView.postDelayed({
            val gameChannelId = ClipboardUtils.getClipboardContent(this)
            if (gameChannelId != "" && gameChannelId.startsWith("tejieBox_game_channelId=")) {
                val intent = Intent(this, GameDetailActivity::class.java)
                intent.putExtra(
                    GameDetailActivity.GAME_CHANNEL_ID,
                    gameChannelId.replace("tejieBox_game_channelId=", "")
                )
                startActivity(intent)
            }
        }, 1000)

        toCheckIsNeedUpdateGameInfo()
    }

    /**
     * 获取启动页图片url
     */
    private fun toGetSplashImgUrl() {
        splashUrlList.clear()
        val getSplashUrl = RetrofitUtils.builder().getSplashUrl(
            if (BaseAppUpdateSetting.isToPromoteVersion) 1
            else null
        )
        getSplashUrlObservable = getSplashUrl.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data.isNotEmpty()) {
                                splashUrlList.addAll(it.data)
                                toCheckSplashImg()
                            } else {
                                toDeleteAllSplashImg()
                            }
                        }
                    }
                }
            }, {
            })
    }

    /**
     * 检查是否需要下载更新背景图片
     */
    private fun toCheckSplashImg() {
        val splashDir = getExternalFilesDir("splash")
        if (splashDir == null || !splashDir.exists() || !splashDir.isDirectory) {
            toSaveSplashImg(splashDir!!)
            return
        }
        val splashImg = splashDir.listFiles()
        if (splashImg.isEmpty()) {
            toSaveSplashImg(splashDir)
            return
        }
        val tempSplashUrlList = mutableListOf<String>()
        tempSplashUrlList.addAll(splashUrlList)
        val deleteSplashImgList = mutableListOf<File>()

        //是否需要删除这个本地封面
        var isNeedDeleteSplashImg: Boolean
        //遍历本地存储的封面
        for (localSplashImg in splashImg) {
            //先行假设是需要删除然后下载新的
            isNeedDeleteSplashImg = true
            //本地存储封面的文件名
            val localSplashImgName =
                localSplashImg.path.substring(localSplashImg.path.lastIndexOf("/") + 1)
            //遍历服务器上现有的封面
            for (serviceSplashImg in splashUrlList) {
                //现行服务器上封面文件名
                val serviceSplashImgName =
                    serviceSplashImg.substring(serviceSplashImg.lastIndexOf("/") + 1)
                if (serviceSplashImgName == localSplashImgName) {
                    //如果服务器封面图片等于本地存储封面图片,说明该封面没有任何问题,需要沿用,不再下载了
                    isNeedDeleteSplashImg = false
                    tempSplashUrlList.remove(serviceSplashImg)
                }
            }
            if (isNeedDeleteSplashImg) {
                //如果需要删除本地封面图
                deleteSplashImgList.add(localSplashImg)
            }
        }
        toDeleteSplashImg(deleteSplashImgList)
        splashUrlList.clear()
        splashUrlList.addAll(tempSplashUrlList)
        if (splashUrlList.isNotEmpty()) {
            toSaveSplashImg(splashDir)
        }
    }

    /**
     * 下载保存图片到本地
     */
    private fun toSaveSplashImg(splashDir: File) {
        val fileNameList = mutableListOf<String>()
        for (splashUrl in splashUrlList) {
            val fileName = splashUrl.substring(splashUrl.lastIndexOf("/") + 1)
            fileNameList.add(fileName)
        }
        Aria.download(this)
            .loadGroup(splashUrlList)
            .setDirPath(splashDir.path)
            .setSubFileName(fileNameList)
            .unknownSize()
            .ignoreFilePathOccupy()
            .ignoreCheckPermissions()
            .create()
    }

    /**
     * 删除已经替换了的封面图
     */
    private fun toDeleteSplashImg(deleteSplashImg: MutableList<File>) {
        if (deleteSplashImg.isNullOrEmpty()) {
            return
        }
        for (deleteSplash in deleteSplashImg) {
            try {
                deleteSplash.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 删除全部的封面图
     */
    private fun toDeleteAllSplashImg() {
        val splashDir = getExternalFilesDir("splash")
        if (splashDir == null || !splashDir.exists() || !splashDir.isDirectory) {
            return
        }
        val deleteSplashImg = splashDir.listFiles()
        if (deleteSplashImg.isEmpty()) {
            return
        }
        for (deleteSplash in deleteSplashImg) {
            try {
                deleteSplash.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 检查是否需要更新游戏在线时间
     */
    private fun toCheckIsNeedUpdateGameInfo() {
        val gameTimeInfo = SPUtils.getString(SPArgument.GAME_TIME_INFO)
        if (null != gameTimeInfo && gameTimeInfo.split("-").size == 3) {
            val split = gameTimeInfo.split("-")
            val gameId = split[0].toInt()
            val startTime = split[1].toLong()
            val endTime = split[2].toLong()
            if (endTime - startTime >= 1 * 60 * 1000) {
                val updateGameTimeInfo = RetrofitUtils.builder().updateGameTimeInfo(
                    gameId,
                    startTime.toString(),
                    endTime.toString()
                )
                updateGameTimeInfoObservable = updateGameTimeInfo.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                    }, {})
            }
        }
        SPUtils.putValue(SPArgument.GAME_TIME_INFO, null)
    }

    /**
     * 登录状态的监听
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        if (loginStatusChange == null) {
            return
        }
        LogUtils.d("loginStatusChange.isLogin:${loginStatusChange.isLogin}")
        if (loginStatusChange.isLogin) {
            if (loginStatusChange.isHaveRewardInteger == null || !loginStatusChange.isHaveRewardInteger) {
                toCheckCanGetIntegral()
            }
            when (mainPage) {
                MainPage.MAIN -> {
                    tab_main.setCurrentItem(0)
                    toChangeFragment(0)
                }
                MainPage.PLAYING -> {
                    tab_main.setCurrentItem(1)
                    toChangeFragment(1)
                }
                MainPage.LIKE -> {
                    tab_main.setCurrentItem(2)
                    toChangeFragment(2)
                }
                MainPage.ME -> {
                    tab_main.setCurrentItem(3)
                    toChangeFragment(3)
                }
            }
        } else {
            EventBus.getDefault().post(LikeDataChange(""))
            EventBus.getDefault().post(PlayingDataChange(""))
            tab_main.setCurrentItem(0)
            toChangeFragment(0)
        }
    }

    /**
     * 是否显示小红点
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun isShowRedPoint(redPointChange: RedPointChange) {
        if (redPointChange == null) {
            return
        }
        if (redPointChange.isShow) {
            tab_main.showRedPoint(true)
        } else {
            tab_main.showRedPoint(false)
        }
    }

    /**
     * 更新fragment
     */
    private fun toChangeFragment(index: Int) {
        when (index) {
            0 -> {
                mainPage = MainPage.MAIN
                hideAll()
                currentFragment = mainFragment
                supportFragmentManager.beginTransaction()
                    .show(currentFragment!!)
                    .commitAllowingStateLoss()
            }
            1 -> {
                mainPage = MainPage.PLAYING
                hideAll()
                if (null == playingFragment) {
                    playingFragment = GameFragment.newInstance(1)
                    currentFragment = playingFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_main, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = playingFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
            2 -> {
                mainPage = MainPage.LIKE
                hideAll()
                if (null == likeFragment) {
                    likeFragment = GameFragment.newInstance(2)
                    currentFragment = likeFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_main, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = likeFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
            3 -> {
                mainPage = MainPage.ME
                hideAll()
                if (null == mineFragment) {
                    mineFragment = MineFragment.newInstance()
                    currentFragment = mineFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_main, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = mineFragment
                    supportFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    /**
     * 隐藏掉所有东西
     */
    private fun hideAll() {
        supportFragmentManager.beginTransaction()
            .hide(mainFragment!!)
            .hide(playingFragment ?: mainFragment!!)
            .hide(likeFragment ?: mainFragment!!)
            .hide(mineFragment ?: mainFragment!!)
            .commitAllowingStateLoss()
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)
        downloadPath = ""
        SPUtils.putValue(SPArgument.APP_DOWNLOAD_PATH, null)
        isDownloadApp = false
        Aria.download(this).stopAllTask()
        Aria.download(this).resumeAllTask()
        unregisterReceiver(timeChangeReceiver)

        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null
        VersionDialog.dismiss()
    }

    override fun onTaskResume(task: DownloadTask?) {
        if (isDownloadApp) {
            if (!ApkDownloadDialog.isShowing() && !isFinishing) {
                ApkDownloadDialog.showDialog(this)
            }
            ApkDownloadDialog.setProgress(task?.percent ?: 0)
        }
    }

    override fun onTaskStart(task: DownloadTask?) {
        if (isDownloadApp) {
            if (!ApkDownloadDialog.isShowing()) {
                ApkDownloadDialog.showDialog(this)
            }
            ApkDownloadDialog.setProgress(task?.percent ?: 0)
        }
    }

    override fun onTaskStop(task: DownloadTask?) {
    }

    override fun onTaskCancel(task: DownloadTask?) {
        if (isDownloadApp) {
            ApkDownloadDialog.dismissLoading()
            isDownloadApp = false
        }
    }

    override fun onTaskFail(task: DownloadTask?, e: Exception?) {
        if (isDownloadApp) {
            ApkDownloadDialog.dismissLoading()
            isDownloadApp = false
        }
    }

    @SuppressLint("CheckResult")
    override fun onTaskComplete(task: DownloadTask?) {
        if (isDownloadApp) {
            isDownloadApp = false
            ApkDownloadDialog.setProgress(100)
            ApkDownloadDialog.dismissLoading()
            Aria.download(this).stopAllTask()
            Aria.download(this).removeAllTask(false)
            if (isApkDownload(File(downloadPath)) && !MyApp.isBackground) {
                installAPK(File(downloadPath))
            }
        }
    }

    override fun onTaskRunning(task: DownloadTask?) {
        if (isDownloadApp) {
            if (!ApkDownloadDialog.isShowing()) {
                ApkDownloadDialog.showDialog(this)
            }
            ApkDownloadDialog.setProgress(task?.percent!!)
        }
    }

    /**
     * 判断文件是否完全下载下来
     */
    private fun isApkDownload(file: File) = file.exists() && file.isFile

    /**
     * 去删除安装包
     */
    private fun toDeleteApk() {
        Thread {
            DeleteApkUtils.deleteApk(File(downloadPath))
        }.start()
    }

    /**
     *下载到本地后执行安装
     */
    private fun installAPK(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canRequestPackageInstalls = packageManager.canRequestPackageInstalls()
            if (canRequestPackageInstalls) {
                toInstallApp(file)
            } else {
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
                startActivityForResult(intent, 100)
                return
            }
        } else {
            toInstallApp(file)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && packageManager.canRequestPackageInstalls()) {
            downloadPath = SPUtils.getString(SPArgument.APP_DOWNLOAD_PATH, "")!!
            if (isApkDownload(File(downloadPath))) {
                toInstallApp(File(downloadPath))
            }
        } else if (requestCode == 100 && !packageManager.canRequestPackageInstalls()) {
            ToastUtils.show(getString(R.string.author_fail))
        }
    }

    /**
     * 开始安装
     */
    private fun toInstallApp(file: File) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } else {
            Uri.fromFile(file)
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }


    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}