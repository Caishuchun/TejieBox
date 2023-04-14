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
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.listener.AppWakeUpAdapter
import com.fm.openinstall.model.AppData
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.GameInfo4ClipboardBean
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.luck.picture.lib.utils.SpUtils
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
import java.io.File
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
        wakeUpAdapter?.let { OpenInstall.getWakeUp(intent, it) }
        toSetSplashBg()
        SPUtils.putValue(SPArgument.GET_GAME_LIST_TIME, 0L)
        toAgreeAgreement()
//        val encrypt = AESUtils.encrypt("k202301|csc0913|csc0913|k2hz001|5|168513")
//        val encrypt = AESUtils.encrypt("678910|csc0913|csc0913|ltdjp|5|168513")
//        val encrypt = AESUtils.encrypt("vm1000|w123|w123|vm1000|1|1")
//        val encrypt = AESUtils.encrypt("{\"type\":1,\"share_id\":\"QY\"}")
//        val encrypt = AESUtils.encrypt("{\"type\":3,\"game_id\":1344}")
//        val encrypt = AESUtils.encrypt("{\"type\":2,\"game_channel\":\"vm1000\",\"account\":\"w123\",\"password\":\"w123\",\"game_version\":\"vm1000\",\"server_id\":\"1\",\"role_id\":\"1\"}")
//        LogUtils.d("++++++++++++++$encrypt")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        wakeUpAdapter?.let { OpenInstall.getWakeUp(intent, it) }
    }

    /**
     * OpenInstall 一键拉起功能
     */
    private var wakeUpAdapter: AppWakeUpAdapter? = object : AppWakeUpAdapter() {
        override fun onWakeUp(appData: AppData) {
            LogUtils.d("==========================一键拉起")
            LogUtils.d("==========================channel:${appData.channel},bindData:${appData.data}")
        }
    }

    /**
     * 获取剪切板数据
     */
    private fun toGetClipboardContent() {
        val clipboardContent = ClipboardUtils.getClipboardContent(this) ?: return
        if (clipboardContent.startsWith("TJBOX|")) {
            //需要跳转到详情页
            val replace = clipboardContent.replace("TJBOX|", "")
            var gameId = -1
            if (replace != "") {
                gameId = try {
                    replace.toInt()
                } catch (e: NumberFormatException) {
                    -1
                }
            }
            SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_JUMP, gameId)
            SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_UPDATE, gameId)
//            ClipboardUtils.clearClipboardContent(this)
        } else {
            //判断是不是要去全部游戏
            val decrypt = try {
                AESUtils.decrypt(clipboardContent) ?: return
            } catch (e: Exception) {
                return
            }
            LogUtils.d("=======$decrypt")
            val gameInfo = decrypt.split("|")
            if (gameInfo.size < 6) return
            val gameInfo4ClipboardBean = GameInfo4ClipboardBean(
                gameInfo[0],
                gameInfo[1],
                gameInfo[2],
                gameInfo[3],
                gameInfo[4],
                gameInfo[5],
            )
            if (gameInfo4ClipboardBean.channelId.length > 6) {
                val channelIdHead = gameInfo4ClipboardBean.channelId.substring(0, 6)
                if (channelIdHead.toLowerCase().contains("box")) {
                    return
                }
            }
            GameInfo4ClipboardBean.setData(gameInfo4ClipboardBean)
        }
    }

    /**
     * 先去同意协议再说
     */
    private fun toAgreeAgreement() {
        val isAgree = SPUtils.getBoolean(SPArgument.IS_CHECK_AGREEMENT, false)
        if (!isAgree && BaseAppUpdateSetting.isToAuditVersion) {
            DialogUtils.showAgreementDialog(
                this,
                object : DialogUtils.OnDialogListener {
                    override fun next() {
                        SPUtils.putValue(SPArgument.IS_CHECK_AGREEMENT, true)
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
                    if (count == 0) {
                        getPermission(1)
                    } else {
                        finish()
                    }
                }

                override fun onFinish() {
                    LogUtils.d("HiPermission=>onFinish()")
                    val isCheckAgreement = SPUtils.getBoolean(SPArgument.IS_CHECK_AGREEMENT, false)
                    if (BaseAppUpdateSetting.isToPromoteVersion && !isCheckAgreement) {
                        return
                    }
                    toMain4CheckVersion()
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
     * 倒计时5s进入下一个页面
     */
    @SuppressLint("CheckResult")
    fun toCountDown() {
        //初始化openinstall
        LogUtils.d("==========================初始化")
        OpenInstall.init(this)
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
     */
    @SuppressLint("CheckResult")
    private fun toMain4CheckVersion() {
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
                        VersionBean.setData(it.getData()!!)
                        toCheckVersion(it.getData()!!)
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
//                toCountDown()
            } else {
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
        pb_splash_download.visibility = View.VISIBLE
        tv_splash_download.visibility = View.VISIBLE
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
            pb_splash_download.progress = task?.percent ?: 0
            tv_splash_download.text = "检查更新 ${task?.percent ?: 0}%"
        }
    }

    override fun onTaskStart(task: DownloadTask?) {
        if (isDownloadApp) {
            pb_splash_download.progress = task?.percent ?: 0
            tv_splash_download.text = "检查更新 ${task?.percent ?: 0}%"
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
            pb_splash_download.progress = 100
            tv_splash_download.text = "检查更新 100%"
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
            pb_splash_download.progress = task?.percent ?: 0
            tv_splash_download.text = "检查更新 ${task?.percent ?: 0}%"
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
        //版本更新的时候,删除所有的启动图
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

    /**
     * 现在可以跳转到主界面
     */
    @SuppressLint("CheckResult")
    private fun toMain() {
        //检查邀请码是否填写过了
//        val inviteCodeUsed = SPUtils.getBoolean(SPArgument.INVITE_CODE_USED, true)
//        if (inviteCodeUsed) {
        SPUtils.putValue(SPArgument.IS_LOGIN, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
//        } else {
//            startActivity(Intent(this, InviteCodeActivity::class.java))
//            finish()
//        }
    }

    override fun destroy() {
        checkVersionObservable?.dispose()
        countDownTimeObservable?.dispose()

        checkVersionObservable = null
        countDownTimeObservable = null

        wakeUpAdapter = null
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