package com.fortune.tejiebox.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.utils.*
import com.google.gson.Gson
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_splash.*
import me.weyye.hipermission.HiPermission
import me.weyye.hipermission.PermissionCallback
import me.weyye.hipermission.PermissionItem
import java.io.File
import java.util.concurrent.TimeUnit

class SplashActivity : BaseActivity() {

    private var checkVersionObservable: Disposable? = null
    private var countDownTimeObservable: Disposable? = null
    private var countDownTime = 3
    private var isFirst = true

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: SplashActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_splash

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        setTheme(R.style.NormalTheme)
        instance = this
        LoginUtils.init(this)

        toSetSplashBg()

        toDeleteGameApk()
        getPermission(0)
    }

    /**
     * 设置封面背景图片
     */
    private fun toSetSplashBg() {
        val splashDir = getExternalFilesDir("splash")
        if (splashDir == null || !splashDir.exists() || !splashDir.isDirectory) {
            return
        }
        val splashImg = splashDir.listFiles()
        if (splashImg.isEmpty()) {
            return
        }
        val imgIndex = (splashImg.indices).random()
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeFile(splashImg[imgIndex].path)
        } catch (e: Exception) {
            return
        }
        if (bitmap == null) {
            return
        }
        iv_splash_bg.setImageBitmap(bitmap)
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
            .msg("为了您正常使用特戒盒子,需要以下权限")
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
                    toMain4CheckVersion()
                    toCountDown()
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
        countDownTimeObservable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (isFirst) {
                    isFirst = false
                }
                if (countDownTime > 0) {
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
        val checkVersion = RetrofitUtils.builder().checkVersion(
            GetDeviceId.getDeviceId(this)
        )
        checkVersionObservable = checkVersion
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    if (it.getCode() == 1) {
                        VersionBean.setData(it.getData()!!)
                    } else if (it.getCode() == -1) {
                        ToastUtils.show(it.getMsg()!!)
                        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
                        SPUtils.putValue(SPArgument.PHONE_NUMBER, null)
                        SPUtils.putValue(SPArgument.USER_ID, null)
                        SPUtils.putValue(SPArgument.IS_HAVE_ID, 0)
                        SPUtils.putValue(SPArgument.ID_NAME, null)
                        SPUtils.putValue(SPArgument.ID_NUM, null)
                        ActivityManager.toSplashActivity(this)
                    }
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
            })
    }

    /**
     * 现在可以跳转到主界面
     */
    @SuppressLint("CheckResult")
    private fun toMain() {
        SPUtils.putValue(SPArgument.TIME_4_PHONE_CHARGE, System.currentTimeMillis())
        SPUtils.putValue(SPArgument.IS_LOGIN, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun destroy() {
        checkVersionObservable?.dispose()
        countDownTimeObservable?.dispose()

        checkVersionObservable = null
        countDownTimeObservable = null
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