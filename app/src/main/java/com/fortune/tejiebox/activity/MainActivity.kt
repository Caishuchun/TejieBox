package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.*
import com.fortune.tejiebox.fragment.*
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
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
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {

    private var mainFragment: MainFragment? = null
    private var playingFragment: LikePlayFragment? = null
    private var likeFragment: LikePlayFragment? = null
    private var mineFragment: MineFragment? = null

    private var canQuit = false
    var currentFragment: Fragment? = null
    private var downloadPath = "" //下载的安装路径
    private var isDownloadApp = false //是否在下载app
    private var updateGameTimeInfoObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: MainActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        var mainPage: MainPage = MainPage.MAIN
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

        mainFragment = MainFragment.newInstance()

        initView()
        toDeleteGameApk()
    }

    /**
     * 删除游戏安装包
     */
    private fun toDeleteGameApk() {
        //没有安装目录,就是么有安装包,啥也不做
        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        if (downloadDir.isFile) {
            //检查到安装目录变成文件,也就是么有安装包,啥也不做
            return
        }
        if (downloadDir.isDirectory) {
            //安装目录是正经的文件夹了
            val files = downloadDir.listFiles()
            if (files.isEmpty()) {
                //可是它为空,也就是么有安装包,啥也不做
                return
            }
            for (file in files) {
                if (file.isFile && file.name.endsWith(".apk") && !file.name.contains(packageName)) {
                    //文件夹下有文件,并且就是文件,最重要的是apk文件
                    val split = file.name.split("_")
                    val version = split[split.size - 1]
                    val packageName = file.name.replace(version, "").replace(".apk", "")
                    if (InstallApkUtils.isInstallApk(this, packageName)) {
                        Thread {
                            DeleteApkUtils.deleteApk(File("${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()}/${file.name}"))
                        }.start()
                    }
                }
            }
        }
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
        if (mainFragment == null && fragment is MainFragment) {
            mainFragment = fragment
        } else if (playingFragment == null && fragment is LikePlayFragment && mainPage == MainPage.PLAYING) {
            playingFragment = fragment
        } else if (likeFragment == null && fragment is LikePlayFragment && mainPage == MainPage.LIKE) {
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
            val versionName = data.update_url!!.substring(data.update_url!!.lastIndexOf("/") + 1)
            downloadPath =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + versionName
            SPUtils.putValue(SPArgument.APP_DOWNLOAD_PATH, downloadPath)
            if (newVersion > currentVersion) {
                //需要更新的话,直接更新
                if (isApkDownload(File(downloadPath))) {
                    installAPK(File(downloadPath))
                } else {
                    toDownloadApk(data.update_url!!)
                }
            } else {
                if (isApkDownload(File(downloadPath))) {
                    toDeleteApk()
                }
                //已经更新过
                val isNeedUpdateDialog = SPUtils.getBoolean(SPArgument.IS_NEED_UPDATE_DIALOG, false)
                if (isNeedUpdateDialog) {
                    SPUtils.putValue(SPArgument.IS_NEED_UPDATE_DIALOG, false)
                    VersionDialog.show(
                        this@MainActivity,
                        data.update_msg.toString(),
                        object : VersionDialog.OnUpdateAPP {
                            override fun onUpdate() {
//                                ll_shade_root.visibility = View.VISIBLE
                            }
                        }
                    )
                } else {
//                    val isNeedShade = SPUtils.getBoolean(SPArgument.IS_NEED_SHADE_NEW, true)
//                    if (isNeedShade) {
//                        ll_shade_root.visibility = View.VISIBLE
//                    }
                }
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
         * 当进入游戏后,检查剪贴板的数据,如果数据对上的话,跳转到相应的游戏详情页
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
        }, 200)

        toCheckIsNeedUpdateGameInfo()
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
            if (endTime - startTime >= 2 * 60 * 1000) {
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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        LogUtils.d("loginStatusChange.isLogin:${loginStatusChange.isLogin}")
        if (loginStatusChange.isLogin) {
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
                    playingFragment = LikePlayFragment.newInstance(0)
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
                    likeFragment = LikePlayFragment.newInstance(1)
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

        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null
        VersionDialog.dismiss()
    }

    override fun onTaskResume(task: DownloadTask?) {
        if (!ApkDownloadDialog.isShowing()) {
            ApkDownloadDialog.showDialog(this)
        }
        ApkDownloadDialog.setProgress(task?.percent ?: 0)
    }

    override fun onTaskStart(task: DownloadTask?) {
        if (!ApkDownloadDialog.isShowing()) {
            ApkDownloadDialog.showDialog(this)
        }
        ApkDownloadDialog.setProgress(task?.percent ?: 0)
    }

    override fun onTaskStop(task: DownloadTask?) {
    }

    override fun onTaskCancel(task: DownloadTask?) {
        ApkDownloadDialog.dismissLoading()
        isDownloadApp = false
    }

    override fun onTaskFail(task: DownloadTask?, e: Exception?) {
        ApkDownloadDialog.dismissLoading()
        isDownloadApp = false
    }

    @SuppressLint("CheckResult")
    override fun onTaskComplete(task: DownloadTask?) {
        isDownloadApp = false
        ApkDownloadDialog.setProgress(100)
        ApkDownloadDialog.dismissLoading()
        Aria.download(this).stopAllTask()
        Aria.download(this).removeAllTask(false)
        if (isApkDownload(File(downloadPath)) && !MyApp.isBackground) {
            installAPK(File(downloadPath))
        }
    }

    override fun onTaskRunning(task: DownloadTask?) {
        if (!ApkDownloadDialog.isShowing()) {
            ApkDownloadDialog.showDialog(this)
        }
        ApkDownloadDialog.setProgress(task?.percent!!)
    }

    /**
     * 跳转到home界面
     */
    fun toHomeFragment() {
        tab_main.setCurrentItem(0)
        toChangeFragment(0)
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
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && packageManager.canRequestPackageInstalls()) {
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
        SPUtils.putValue(SPArgument.IS_NEED_UPDATE_DIALOG, true)
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