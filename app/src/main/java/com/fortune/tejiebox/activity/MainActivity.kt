package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*
import android.view.View
import android.view.animation.*
import androidx.fragment.app.Fragment
import com.arialyy.aria.core.Aria
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.listener.AppInstallAdapter
import com.fm.openinstall.model.AppData
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.GameInfo4ClipboardBean
import com.fortune.tejiebox.bean.OpenInstallBean
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.*
import com.fortune.tejiebox.fragment.*
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.listener.OnBottomBarItemSelectListener
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.room.CustomerServiceInfo
import com.fortune.tejiebox.room.CustomerServiceInfoDataBase
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.utils.ActivityManager
import com.google.gson.Gson
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
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private var mainFragment: GameFragment? = null
    private var moreGameFragment: MoreGameFragment? = null
    private var playingFragment: PlayingAndCollectionFatherFragment? = null
    private var mineFragment: MineFragment? = null

    private var canQuit = false
    var currentFragment: Fragment? = null
    private var updateGameTimeInfoObservable: Disposable? = null
    private var canGetIntegralObservable: Disposable? = null

    private var intentFilter: IntentFilter? = null
    private var timeChangeReceiver: TimeChangeReceiver? = null

    private var splashUrlList = mutableListOf<String>()
    private var getSplashUrlObservable: Disposable? = null
    private var getReCustomerInfoObservable: Disposable? = null
    private var textFontChangeObservable: Disposable? = null
    private var getGameIdObservable: Disposable? = null

    private var openInstallType = -1

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
        MAIN, PLAYING, ALL, ME
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
//        LogUtils.d("+++++++++++++++++++++++${SPUtils.getString(SPArgument.LOGIN_TOKEN)}")
        instance = this
        EventBus.getDefault().register(this)
        StatusBarUtils.setTextDark(this, true)
        Aria.download(this).register()

        mainFragment = GameFragment.newInstance()

        initView()
//        isHaveNewPlayingGame(IsHaveNewPlayingGame(true))

        intentFilter = IntentFilter()
        intentFilter?.addAction(Intent.ACTION_TIME_TICK)
        if (timeChangeReceiver == null) {
            timeChangeReceiver = TimeChangeReceiver()
        }
        registerReceiver(timeChangeReceiver, intentFilter)

        ll_shade_root.postDelayed({
            if (MyApp.getInstance().isHaveToken()) {
                //检查是否能够领取奖励
                toCheckCanGetIntegral()
                //如果登录了获取一下客服回复消息
                toGetCustomerServiceInfo()
            }
        }, 1000)

//        val gameId = SPUtils.getInt(SPArgument.NEED_JUMP_GAME_ID_JUMP, -1)
//        if (gameId != -1) {
//            //需要跳转到游戏详情页
//            GameFragment.setGameId(gameId)
//            SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_JUMP, -1)
//        }
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

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        if (mainFragment == null && fragment is GameFragment && mainPage == MainPage.MAIN) {
            mainFragment = fragment
        } else if (playingFragment == null && fragment is PlayingAndCollectionFatherFragment && mainPage == MainPage.PLAYING) {
            playingFragment = fragment
        } else if (moreGameFragment == null && fragment is MoreGameFragment && mainPage == MainPage.ALL) {
            moreGameFragment = fragment
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
            val isNeedUpdateDialog =
                SPUtils.getBoolean(SPArgument.IS_NEED_UPDATE_DIALOG, false)
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
            } else {
                if (data.notice != null && data.notice?.contains("@") == true) {
                    val noticeId = data.notice!!.split("@")[0].toInt()
                    val noticeStr = data.notice!!.substring(data.notice!!.indexOf("@") + 1)
                    val currentNoticeId = SPUtils.getInt(SPArgument.NOTICE_ID, -1)
                    if (noticeId > currentNoticeId) {
                        SPUtils.putValue(SPArgument.NOTICE_ID, noticeId)
                        VersionDialog.show(
                            this@MainActivity,
                            noticeStr,
                            object : VersionDialog.OnUpdateAPP {
                                override fun onUpdate() {
                                }
                            }
                        )
                    }
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
                if (MyApp.getInstance().isHaveToken()) {
                    toGetCustomerServiceInfo()
                }
            }
        })

        //页面加载结束1s之后
        window.decorView.postDelayed({
            toCheckIsNeedOpenGame(MyApp.getInstance().isHaveToken())
        }, 1000)

        toCheckIsNeedUpdateGameInfo()
        toGetSplashImgUrl()
    }

    /**
     * 跳转到首页
     */
    fun toMainFragment() {
        tab_main.setCurrentItem(0)
        toChangeFragment(0)
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
//            if (endTime - startTime >= 1 * 60 * 1000) {
            val updateGameTimeInfo = RetrofitUtils.builder().updateGameTimeInfo(
                gameId,
                startTime.toString(),
                endTime.toString()
            )
            updateGameTimeInfoObservable = updateGameTimeInfo.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {})
//            }
        }
        SPUtils.putValue(SPArgument.GAME_TIME_INFO, null)
    }

    /**
     * 登录状态的监听
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        tab_main.showMsgNum(0)
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
                MainPage.ALL -> {
                    tab_main.setCurrentItem(2)
                    toChangeFragment(2)
                }
                MainPage.ME -> {
                    tab_main.setCurrentItem(3)
                    toChangeFragment(3)
                }
            }
            toGetCustomerServiceInfo()
            toCheckIsNeedOpenGame(true)
        } else {
            tab_main.showMsgNum(0)
            EventBus.getDefault().post(LikeDataChange(""))
            EventBus.getDefault().post(PlayingDataChange(""))
            tab_main.setCurrentItem(0)
            toChangeFragment(0)
        }
    }

    /**
     * 检查是否需要打开游戏
     * @param isLogined 是否一登录
     */
    private fun toCheckIsNeedOpenGame(isLogined: Boolean = false) {
//        val data = GameInfo4ClipboardBean.getData() ?: return
//        LogUtils.d("剪切板拿到的数据:$data")
//        if (isLogined) {
//            //登录先判断是否能获取到游戏id
//            toGetGameInfo(data.version)
//        } else {
//            //没登录就跳转登录
//            LoginUtils.toQuickLogin(this)
//        }
        //本地取数据
        val openInstallUsed = SPUtils.getBoolean(SPArgument.OPEN_INSTALL_USED, false)
        if (openInstallUsed && openInstallType != 2) {
            // 使用了
            SPUtils.putValue(SPArgument.OPEN_INSTALL_INFO, null)
        } else {
            // 未使用
            val openInstallInfo = SPUtils.getString(SPArgument.OPEN_INSTALL_INFO)
//            dealOpenInstallInfo(openInstallInfo, isLogined)
            if (openInstallInfo == null) {
                // 从openInstall取数据
                LogUtils.d("==========================获取参数")
                OpenInstall.getInstall(object : AppInstallAdapter() {
                    override fun onInstall(appData: AppData) {
                        LogUtils.d(
                            "==========================${Gson().toJson(appData)}"
                        )
                        if (appData.data != "") {
                            //data不为空
                            val dataInfo =
                                Gson().fromJson(appData.data, OpenInstallBean::class.java)
                            dealOpenInstallInfo(dataInfo.i, isLogined)
                        } else {
                            LogUtils.d("==========================没有数据")
                        }
                    }
                })
            } else {
                dealOpenInstallInfo(openInstallInfo, isLogined)
            }
        }
    }

    /**
     * 处理openInstall 传来的值
     */
    private fun dealOpenInstallInfo(data: String?, isLogined: Boolean) {
        if (data.isNullOrEmpty()) {
            return
        }
        var info = data
        try {
            info = Uri.decode(info)
            LogUtils.d("==========================info:$info")
            SPUtils.putValue(SPArgument.OPEN_INSTALL_INFO, info)
            val needInfo = AESUtils.decrypt(info)
            if (needInfo != null) {
                LogUtils.d("==========================needInfo:$needInfo")
                val jsonObject = JSONObject(needInfo)
                val keys = jsonObject.keys()
                val map = HashMap<String, Any>()
                for (key in keys) {
                    val value = jsonObject[key]
                    map[key] = value
                }
                LogUtils.d("==========================map:$map")
                when {
                    map["type"] == 1 -> {
                        //1.用户分享下载
                        LogUtils.d("==========================map[\"type\"] == 1")
                        //需要使用加密串在注册登录时使用
                        openInstallType = 1
                        if (!isLogined) {
                            LoginUtils.toQuickLogin(this)
                        }
                    }
                    map["type"] == 2 -> {
                        //2.游戏内跳转下载
                        LogUtils.d("==========================map[\"type\"] == 2")
                        openInstallType = 2
                        //需要在首页的时候进行判断, 跳转
                        if (!isLogined) {
                            LoginUtils.toQuickLogin(this)
                        } else {
                            val gameInfo4ClipboardBean = GameInfo4ClipboardBean(
                                map["game_channel"] as String,
                                map["account"] as String,
                                map["password"] as String,
                                map["game_version"] as String,
                                map["server_id"] as String,
                                map["role_id"] as String,
                            )
                            if (gameInfo4ClipboardBean.channelId.length > 6) {
                                val channelIdHead = gameInfo4ClipboardBean.channelId.substring(0, 6)
                                if (channelIdHead.toLowerCase().contains("box")) {
                                    return
                                }
                            }
                            GameInfo4ClipboardBean.setData(gameInfo4ClipboardBean)
                            SPUtils.putValue(SPArgument.OPEN_INSTALL_USED, true)
                            toGetGameInfo(gameInfo4ClipboardBean.version)
                        }
                    }
                    map["type"] == 3 -> {
                        //3.GM分享
                        LogUtils.d("==========================map[\"type\"] == 3")
                        openInstallType = 3
                        //跳转到游戏详情页面
                        var gameId = SPUtils.getInt(SPArgument.NEED_JUMP_GAME_ID_JUMP, -1)
                        if (gameId == -1) {
                            //需要跳转到游戏详情页
                            gameId = map["game_id"] as Int
                            SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_JUMP, gameId)
                            SPUtils.putValue(SPArgument.NEED_JUMP_GAME_ID_UPDATE, gameId)
                            mainFragment?.openGameDetailActivity(gameId)
                        }
                    }
                    else -> {
                        LogUtils.d("==========================type异常, 不是123")
                    }
                }
            } else {
                LogUtils.d("==========================数据为空")
            }
        } catch (e: Exception) {
            LogUtils.d("==========================解密失败")
            LogUtils.d("==========================${e.message}")
        }
    }

    /**
     * 通过游戏渠道号获取游戏数据
     */
    private fun toGetGameInfo(channelId: String) {
        val getGameId = RetrofitUtils.builder().getGameId(channelId)
        getGameIdObservable = getGameId.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (it.code == 1 && it.data != null && it.data.game_id != null) {
                        if (it.data.game_id < 10000) {
                            //上架游戏
                        } else {
                            //全部游戏
                            if (it.data.game_channelId != null) {
                                tab_main.setCurrentItem(2)
                                toChangeFragment(2)
                                MoreGameFragment.setGameInfo(
                                    it.data.game_id,
                                    it.data.game_name ?: "特戒盒子游戏",
                                    it.data.game_channelId
                                )
                            }
                        }
                    }
                }, {
                    GameInfo4ClipboardBean.setData(null)
//                    ClipboardUtils.clearClipboardContent(this)
                })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun changeShowNum(showNumChange: ShowNumChange) {
        tab_main.showMsgNum(showNumChange.num)
    }

    /**
     * 获取客服回复信息
     */
    private fun toGetCustomerServiceInfo() {
        val getMsg = RetrofitUtils.builder().getMsg()
        getReCustomerInfoObservable = getMsg.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it.code) {
                    1 -> {
                        val dataBase = CustomerServiceInfoDataBase.getDataBase(this)
                        val customerServiceInfoDao = dataBase.customerServiceInfoDao()
                        if (it.data.isNotEmpty()) {
                            for (data in it.data) {
                                val customerServiceInfo = CustomerServiceInfo(
                                    data.id, 0, data.type,
                                    if (data.type == 1) data.content else null,
                                    if (data.type == 2) data.content else null,
                                    data.img_width, data.img_height,
                                    data.create_stamp,
                                    0
                                )
                                customerServiceInfoDao.addInfo(customerServiceInfo)
                            }
                        }
                        val all = customerServiceInfoDao.all
                        var notRead = 0
                        for (info in all) {
                            if (info.is_read == 0) {
                                notRead++
                            }
                        }
                        tab_main.showMsgNum(notRead)
                        EventBus.getDefault().postSticky(ShowNumChange(notRead))
                    }
                    -1 -> {
                        ToastUtils.show(it.msg)
                        ActivityManager.toSplashActivity(this)
                    }
                }
            }, {})
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
                    playingFragment = PlayingAndCollectionFatherFragment.newInstance()
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
                mainPage = MainPage.ALL
                hideAll()
                if (null == moreGameFragment) {
                    moreGameFragment = MoreGameFragment.newInstance()
                    currentFragment = moreGameFragment
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fl_main, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = moreGameFragment
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
            .hide(moreGameFragment ?: mainFragment!!)
            .hide(mineFragment ?: mainFragment!!)
            .commitAllowingStateLoss()
    }

    override fun destroy() {
        EventBus.getDefault().unregister(this)
        SPUtils.putValue(SPArgument.APP_DOWNLOAD_PATH, null)
        unregisterReceiver(timeChangeReceiver)

        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null
        VersionDialog.dismiss()

        canGetIntegralObservable?.dispose()
        canGetIntegralObservable = null

        getSplashUrlObservable?.dispose()
        getSplashUrlObservable = null

        getReCustomerInfoObservable?.dispose()
        getReCustomerInfoObservable = null

        textFontChangeObservable?.dispose()
        textFontChangeObservable = null

        getGameIdObservable?.dispose()
        getGameIdObservable = null
    }

    /**
     * 是否有新的在玩出现
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun isHaveNewPlayingGame(isHaveNewPlayingGame: IsHaveNewPlayingGame) =
        if (isHaveNewPlayingGame.isHaveNewPlayingGame) {
            tv_main_tip.visibility = View.VISIBLE
            //组合动画
            val animationSet = AnimationSet(true)
            val alphaAnimation = AlphaAnimation(0f, 1f)
            val scaleAnimation = ScaleAnimation(
                0f, 1f, 0.1f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1f
            )
            animationSet.addAnimation(alphaAnimation)
            animationSet.addAnimation(scaleAnimation)
            animationSet.duration = 2000
            animationSet.interpolator = AccelerateDecelerateInterpolator()
            animationSet.fillAfter = true
            animationSet.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                @SuppressLint("CheckResult")
                override fun onAnimationEnd(animation: Animation?) {
                    toSetText(animationSet)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            tv_main_tip.startAnimation(animationSet)
        } else {
            tv_main_tip.visibility = View.GONE
        }

    /**
     * 逐一显示文字, 突出明显性
     */
    @SuppressLint("CheckResult")
    private fun toSetText(animationSet: AnimationSet) {
        textFontChangeObservable?.dispose()
        textFontChangeObservable = null
        var currentIndex = -1
        val textFont = "刚玩的游戏在这里"
        textFontChangeObservable = Observable.interval(200, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                currentIndex += 1
                if (currentIndex > 0 && currentIndex <= textFont.length) {
                    tv_main_tip.text = textFont.substring(0, currentIndex)
                } else if (currentIndex == textFont.length * 3) {
                    textFontChangeObservable?.dispose()
                    textFontChangeObservable = null
                    tv_main_tip.text = "点击这里查看在玩"
                    tv_main_tip.startAnimation(animationSet)
                }
            }
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