package com.fortune.tejiebox.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.bumptech.glide.Glide
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.ShelfDataBean
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.android.synthetic.main.fragment_account_login.view.*
import me.weyye.hipermission.HiPermission
import me.weyye.hipermission.PermissionCallback
import me.weyye.hipermission.PermissionItem
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@SuppressLint("SetTextI18n")
class NewSplashActivity : BaseActivity() {

    private var checkVersionObservable: Disposable? = null
    private var countDownTimeObservable: Disposable? = null
    private var countDownTime = 3
    private var isFirst = true

    private var downloadPath = "" //下载的安装路径
    private var isDownloadApp = false //是否在下载app

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: NewSplashActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_splash

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        Aria.download(this).register()
        setTheme(R.style.NormalTheme)
        instance = this
        toSetSplashBg()
        SPUtils.putValue(SPArgument.GET_GAME_LIST_TIME, 0L)

        Thread {
            Thread.sleep(1000)
            runOnUiThread {
                toAgreeAgreement()
            }
        }.start()

    }

    /**
     * 先去同意协议再说
     */
    private fun toAgreeAgreement() {
        val isAgree = SPUtils.getBoolean(SPArgument.IS_CHECK_AGREEMENT_SPLASH, false)
        if (!isAgree
//            && BaseAppUpdateSetting.isToAuditVersion
        ) {
            DialogUtils.showAgreementDialog(
                this,
                true,
                object : DialogUtils.OnDialogListener {
                    override fun next() {
                        SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT_SPLASH, true)
                        LoginUtils.init(this@NewSplashActivity)
                        getPermission(0)
                    }
                }
            )
        } else {
            LoginUtils.init(this)
            getPermission(0)
        }
    }

    /**
     * 设置封面背景图片
     */
    private fun toSetSplashBg() {
        val splashDir = getExternalFilesDir("splash")
        if (splashDir == null || !splashDir.exists() || !splashDir.isDirectory) {
            Glide.with(this)
                .load(
                    if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.bg_splash_tejie
                    else R.mipmap.bg_splash_tejiebox
                )
                .into(iv_splash_bg)
            return
        }
        val splashImages = splashDir.listFiles()
        if (splashImages.isEmpty()) {
            Glide.with(this)
                .load(
                    if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.bg_splash_tejie
                    else R.mipmap.bg_splash_tejiebox
                )
                .into(iv_splash_bg)
            return
        }
        val imgIndex = (splashImages.indices).random()
        Glide.with(this)
            .load(splashImages[imgIndex].path)
            .error(
                if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.bg_splash_tejie
                else R.mipmap.bg_splash_tejiebox
            )
            .into(iv_splash_bg)
    }

    /**
     * 资源文件转为bitmap
     */
    private fun resource2Bitmap() =
        BitmapFactory.decodeResource(
            resources,
            if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.bg_splash_tejie
            else R.mipmap.bg_splash_tejiebox
        )

    /**
     * 获取权限
     */
    private fun getPermission(count: Int) {
        val permissions = arrayListOf(
            PermissionItem(
                Manifest.permission.READ_PHONE_STATE,
                "手机状态",
                R.drawable.permission_ic_phone
            ),
            PermissionItem(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "文件管理",
                R.drawable.permission_ic_storage
            )
        )
        HiPermission.create(this)
            .permissions(permissions)
            .msg("为了您正常使用${resources.getString(R.string.app_name)},需要以下权限")
            .filterColor(Color.parseColor("#5F60FF"))
            .style(R.style.PermissionStyle)
            .checkMutiPermission(object : PermissionCallback {
                override fun onClose() {
                    LogUtils.d("HiPermission=>onClose()")
//                    if (count == 0) {
//                        getPermission(1)
//                    } else {
//                        finish()
//                    }

                    IsMultipleOpenAppUtils.initMultipleOpenApps()
                    toGetShelfData()
                }

                override fun onFinish() {
                    LogUtils.d("HiPermission=>onFinish()")
                    IsMultipleOpenAppUtils.initMultipleOpenApps()
                    toGetShelfData()
                }

                override fun onDeny(permission: String?, position: Int) {
                    LogUtils.d("HiPermission=>onDeny(permission:$permission,position:$position)")
                }

                override fun onGuarantee(permission: String?, position: Int) {
                    LogUtils.d("HiPermission=>onGuarantee(permission:$permission,position:$position)")
                }
            })
    }

    /**
     * 获取服务器上的.JSON文件
     */
    private fun toGetShelfData() {
        val shelfDataRequest = Request.Builder()
            .url("https://cdn.tjbox.lelehuyu.com/apk/setting/shelf_setting.json")
            .build()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
        val shelfDataCall = okHttpClient.newCall(shelfDataRequest)
        shelfDataCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogUtils.d("getShelfData=>onFailure(e:$e)")
                toMain4CheckVersion()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseStr = response.body()?.string()
                if (responseStr == null) {
                    LogUtils.d("getShelfData=>onResponse: responseStr is null")
                    toMain4CheckVersion()
                    return
                }
                try {
                    val shelfDataBean = Gson().fromJson(responseStr, ShelfDataBean::class.java)
                    if (shelfDataBean != null) {
                        LogUtils.d("getShelfData=>onResponse: $shelfDataBean")
                        getDateFromShelfDate(shelfDataBean)
                    } else {
                        LogUtils.d("getShelfData=>onResponse: shelfDataBean is null")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun getDateFromShelfDate(shelfDataBean: ShelfDataBean?) {
        if (shelfDataBean == null) {
            toMain4CheckVersion()
            return
        }

        ShelfDataBean.setData(shelfDataBean)

        //marketChannel 0:默认 1:应用宝 2:华为 3:小米 4:vivo 5:oppo 6:360 7:百度 8:91
        when (BaseAppUpdateSetting.marketChannel) {
            1 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.tencet_market == 0
                )
            }

            2 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.huawei_market == 0
                )
            }

            3 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.xiaomi_market == 0
                )
            }

            4 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.vivo_market == 0
                )
            }

            5 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.oppo_market == 0
                )
            }

            6 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.s360_market == 0
                )
            }

            7 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.baidu_market == 0
                )
            }

            8 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.s91_market == 0
                )
            }

            9 -> {
                toMain4CheckVersion(
                    isShowStartGameBtn = 0,
                    isCanUseShare = shelfDataBean.isCanUseShare,
                    isDirectToJump = shelfDataBean.samsung_market == 0
                )
            }

            else -> {
                toMain4CheckVersion()
            }
        }
    }


    /**
     * 倒计时5s进入下一个页面
     */
    @SuppressLint("CheckResult")
    fun toCountDown() {
        tv_splash_countDown.visibility = View.VISIBLE
        countDownTimeObservable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (isFirst) {
                    isFirst = false
                }
                if (countDownTime >= 0) {
                    tv_splash_countDown.text = countDownTime.toString()
                    countDownTime--
                } else {
                    //不管不顾,直接进主界面
                    toMain()
                    checkVersionObservable?.dispose()
                    countDownTimeObservable?.dispose()

                    checkVersionObservable = null
                    countDownTimeObservable = null
                }
            }
    }

    /**
     * 跳转到主界面前,先检查版本更新状态
     * @param isShowStartGameBtn 是否显示开始游戏按钮 1:显示 0:不显示
     * @param isCanUseShare 是否可以使用分享 1:可以 0:不可以
     * @param isDirectToJump 是否直接跳转到主界面
     */
    @SuppressLint("CheckResult")
    private fun toMain4CheckVersion(
        isShowStartGameBtn: Int = 1,
        isCanUseShare: Int = 1,
        isDirectToJump: Boolean = false
    ) {
        val deviceId = GetDeviceId.getDeviceId(this)
        LogUtils.d("===============$deviceId")
        SPUtils.putValue(SPArgument.ONLY_DEVICE_ID_NEW, GetDeviceId.getDeviceId(this))
        val checkVersion = RetrofitUtils.builder().checkVersion(deviceId)
        checkVersionObservable = checkVersion
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    if (it.getCode() == 1) {
                        val data = it.getData()!!
                        data.isShowStartGameBtn = isShowStartGameBtn
                        data.isCanUseShare = isCanUseShare
                        VersionBean.setData(data)
                        if (isDirectToJump) {
                            toCountDown()
                        } else {
                            toCheckVersion(it.getData()!!)
                        }
                    } else {
                        toCountDown()
                    }
                }
            }, {
                toCountDown()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
            })
    }

    /**
     * 检查版本
     */
    private fun toCheckVersion(data: VersionBean.DataBean) {
        val newVersion = data.version_name!!.replace(".", "").toInt()
        val currentVersion = MyApp.getInstance().getVersion().replace(".", "").toInt()
//        val newVersion = data.version_number ?: MyApp.getInstance().getVersionCode()
//        val currentVersion = MyApp.getInstance().getVersionCode()
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
                tv_splash_updateMsg.text = data.update_msg
                toDownloadApk(updateUrl)
            }
        } else {
            toDeleteApk()
            toCountDown()
            val channel = data.channel ?: return
            //获取本地存储的渠道号,如果没有存储过,说明是第一次进入,则保存并初始化;如果有值,说明已经存在,不在这里再进行初始化了
            val uMChannelId = SPUtils.getString(SPArgument.UM_CHANNEL_ID, null)
            if (null == uMChannelId) {
                SPUtils.putValue(SPArgument.UM_CHANNEL_ID, channel.toString())
                UMUtils.init(this, false)
            }
        }
    }

    /**
     * apk下载
     */
    private fun toDownloadApk(updateUrl: String) {
        ll_splash_update.visibility = View.VISIBLE
        isDownloadApp = true
        Aria.download(this)
            .load(updateUrl) //读取下载地址
            .setFilePath(downloadPath, true) //设置文件保存的完整路径
            .ignoreFilePathOccupy()
            .ignoreCheckPermissions()
            .create()
    }

    override fun onTaskResume(task: DownloadTask?) {
        if (isDownloadApp) {
            pb_splash_download.setProgress(task?.percent ?: 0)
            tv_splash_download.text = "${task?.percent ?: 0}%"
        }
    }

    override fun onTaskStart(task: DownloadTask?) {
        if (isDownloadApp) {
            pb_splash_download.setProgress(task?.percent ?: 0)
            tv_splash_download.text = "${task?.percent ?: 0}%"
        }
    }

    override fun onTaskStop(task: DownloadTask?) {
    }

    override fun onTaskCancel(task: DownloadTask?) {
        if (isDownloadApp) {
            pb_splash_download.visibility = View.GONE
            tv_splash_download.visibility = View.GONE
            isDownloadApp = false
        }
    }

    override fun onTaskFail(task: DownloadTask?, e: Exception?) {
        if (isDownloadApp) {
            pb_splash_download.visibility = View.GONE
            tv_splash_download.visibility = View.GONE
            isDownloadApp = false
        }
    }

    @SuppressLint("CheckResult")
    override fun onTaskComplete(task: DownloadTask?) {
        if (isDownloadApp) {
            isDownloadApp = false
            pb_splash_download.setProgress(100)
            tv_splash_download.text = "100%"
            pb_splash_download.visibility = View.GONE
            tv_splash_download.visibility = View.GONE
            Aria.download(this).stopAllTask()
            Aria.download(this).removeAllTask(false)
            if (isApkDownload(File(downloadPath)) && !MyApp.isBackground) {
                installAPK(File(downloadPath))
            }
        }
    }

    override fun onTaskRunning(task: DownloadTask?) {
        if (isDownloadApp) {
            pb_splash_download.setProgress(task?.percent ?: 0)
            tv_splash_download.text = "${task?.percent ?: 0}%"
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
        DialogUtils.showInstallTipsDialog(
            this@NewSplashActivity,
            object : DialogUtils.OnDialogListener {
                override fun next() {
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
            }
        )
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

    /**
     * 现在可以跳转到主界面
     */
    @SuppressLint("CheckResult")
    private fun toMain() {
        SPUtils.putValue(SPArgument.IS_LOGIN, true)
        startActivity(Intent(this, MainActivity::class.java))
//        startActivity(Intent(this, com.fortune.tejiebox.shangjia.activity.MainActivity::class.java))
        finish()
    }

    override fun destroy() {
        checkVersionObservable?.dispose()
        countDownTimeObservable?.dispose()

        checkVersionObservable = null
        countDownTimeObservable = null

//        wakeUpAdapter = null
    }

    override fun onResume() {
        super.onResume()
        val file = File(downloadPath)
        if (file.exists()) {
            installAPK(file)
        }
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}