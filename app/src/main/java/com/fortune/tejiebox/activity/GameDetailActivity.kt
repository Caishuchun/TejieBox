package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.GameInfoBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LikeDataChange
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.CenterLayoutManager
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.snail.antifake.jni.EmulatorDetectUtil
import com.umeng.analytics.MobclickAgent
import com.unity3d.player.JumpUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_game_detail_v2.*
import kotlinx.android.synthetic.main.item_game_pic.view.*
import kotlinx.android.synthetic.main.item_game_pic_small.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import net.center.blurview.ShapeBlurView
import net.center.blurview.enu.BlurMode
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GameDetailActivity : BaseActivity() {
    private var gameInfoObservable: Disposable? = null
    private lateinit var picAdapter: BaseAdapterWithPosition<String>
    private lateinit var picAdapter4Small: BaseAdapterWithPosition<String>
    private var picLists = mutableListOf<String>()

    private var collectGameObservable: Disposable? = null
    private var addPlayingGameObservable: Disposable? = null

    private var isCollect = false
    private var gameChannel = ""

    private var gameId = -1
    private var gameChannelId: String? = null
    private var isPlayingGame = false
    private var gameStyle: String? = null //游戏登录器UI

    private var updateGameTimeInfoObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: GameDetailActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val GAME_ID = "game_id"
        const val GAME_CHANNEL_ID = "game_channel_id"
    }

    override fun getLayoutId() = R.layout.activity_game_detail_v2

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        instance = this
        gameId = intent.getIntExtra(GAME_ID, -1)
        gameChannelId = intent.getStringExtra(GAME_CHANNEL_ID)
        initView()
        initWebView()
        getInfo()
    }

    /**
     * 设置WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val settings: WebSettings = web_detail.settings
        // 设置WebView支持JavaScript
        settings.javaScriptEnabled = true
        //支持自动适配
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false) //支持放大缩小
        settings.builtInZoomControls = false //显示缩放按钮
        settings.blockNetworkImage = true // 把图片加载放在最后来加载渲染
        settings.allowFileAccess = true // 允许访问文件
        settings.saveFormData = true
        settings.setGeolocationEnabled(true)
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true /// 支持通过JS打开新窗口
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        //设置不让其跳转浏览器
        web_detail.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }
        // 添加客户端支持
        web_detail.webChromeClient = WebChromeClient()
        //不加这个图片显示不出来
        // mWebView.loadUrl(TEXTURL);
        //不加这个图片显示不出来
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            web_detail.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        web_detail.settings.blockNetworkImage = false
        //允许cookie 不然有的网站无法登陆
        val mCookieManager: CookieManager = CookieManager.getInstance()
        mCookieManager.setAcceptCookie(true)
        mCookieManager.setAcceptThirdPartyCookies(web_detail, true)
//        web_detail.loadUrl(URL)
        web_detail.isHorizontalScrollBarEnabled = false
        web_detail.isVerticalScrollBarEnabled = false

        val data = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title> 
    
    <link rel="stylesheet" href="css/base.css">

    <style type="text/css">
		body,p,h1,h2,h3,h4,h5,h6,ul,ol,dl,dt,dd,li {
			margin: 0;
			padding: 0;
			list-style: none;
			font-size: 14px;
			font-family: 宋体;
			color: #000000;
		}

		body {
			background-color: #241E18;
			text-align: center;
		}

		a {
			color: #000000; 
			text-decoration: none;
		}

		/*a:hover {
			color: red;
			text-decoration: underline;
		}*/

		i,s,em {
			font-style: normal;
			text-decoration: none;
		}

		input,img {
			vertical-align: middle;
			border: 0 none;
			outline-style: none;
			padding: 0;
			margin: 0;
		}

		/*清除浮动*/
		.clearfix:after {
			content: "";
			height: 0;
			line-height: 0;
			display: block;
			clear: both;
			visibility: hidden;
		}

		.clearfix {
			zoom: 1;
		}

		/*版心*/
		.w {
			width: 1190px;
			margin: 0 auto;
		}
        .header {
            width: 100%; 
            height: 146px; 
            background-color: #010101; 
            position: relative;
        }
        .game_info {
            height: 100px; 
            margin-left: 30px; 
            margin-top: 23px; 
            margin-bottom: 23px; 
            display: flex; 
            flex-direction: row; 
            float: left
        }
        .game_avart {
            height: 100%; 
            width: 100px; 
            background-color: #6E6568;
        }
        .game_details {
            margin-left: 30px; 
            text-align: left; 
            display: flex; 
            flex-direction: column; 
            justify-content: space-between;
        }
        .game_details_item {
            color: gold;
        }
        .name {
            font-size: 28px; 
        }
        .subname {
            font-size: 16px; 
        }
        .introduce {
            font-size: 16px; 
        }
        .download_btn_header {
            width:192px;
            height:74px;
            background-image: url(resource/download_btn_top.png);
            position: absolute;
            top: 50%;
            right: 30px;
            margin-top: -37px;
        }
        /* width: 192px; height: 74px; background-image: url(resource/download_btn_top.png); */
        .banner {
            height: 937px; 
            background: url("https://t7.baidu.com/it/u=1595072465,3644073269&fm=193&f=GIF") no-repeat;
        }

        .login_gift {
            height: 450px; 
            background: url(resource/login_gift_bg.png) no-repeat;
        }
        .login_gift_title {
            margin-top: 87px;
        }
        .login_gift_list {
            height: 174px; 
            margin-left: 82px; 
            margin-right: 82px; 
            margin-top: 8px; 
            display: flex; 
            flex-direction: row; 
            justify-content: space-between;
        }
        .login_gift_btn {
            width: 220px; 
            height: 55px; 
            background-image: 
            url(resource/login_gift_btn.png); 
            background-color: rgba(0, 0, 0, 0); 
            border-style: none; 
            margin-top: 22px; 
            margin-bottom: 85px;
        }

        .luck_draw {
            text-align: center; 
            margin-bottom: 54px;
        }
        .jackpot {
            height: 438px; 
            background: url(resource/luck_draw_bg.png) no-repeat center; 
            margin-top: 40px;
        }
        .jackpot_list {
            height: 250px; 
            display: grid; 
            grid-gap: 52px 30px; 
            grid-template: repeat(2, 1fr)/repeat(4, 1fr); 
            padding-top: 47px; 
            padding-left: 60px; 
            padding-right: 60px;
        }
        .jackpot_item {
            display: grid;
            place-items: center;
            background: url(resource/jackpot_item.png);
            background-size: cover;
        }
        .luck_draw_btn {
            width: 220px; 
            height: 55px; 
            background-image: url(resource/login_gift_btn.png); 
            background-color: rgba(0, 0, 0, 0); 
            border-style: none; 
            margin-top: 44px;
        }

        .strategy {
            text-align: center;
        }
        .strategy_content {
            height: 444px; 
            background: url(resource/strategy_bg.png) no-repeat center; 
            margin-top: 40px;
            font-size: 25px;
            text-align: left;
            padding-top: 85px;
            padding-left: 55px;
            padding-right: 55px;
            padding-bottom: 85px;
        }
        .load_btn_bottom {
            width: 254px; 
            height: 94px; 
            background-image: url(resource/load_btn_bottom.png); 
            background-color: rgba(0, 0, 0, 0); 
            border-style: none; 
            margin-top: 30px; 
            margin-bottom: 53px;
        }
    </style>
</head>
<body>
    <!-- 顶部 -->
    <div class="header">
       <!-- 游戏介绍 -->
       <div class="game_info">
            <!-- 游戏图标  -->
            <img class="game_avart" src="">
            <div class="game_details">
                <span class="game_details_item name">传奇手游</span>
                <span class="game_details_item subname">(爆率全开刀刀暴击)</span>
                <span class="game_details_item introduce">装备永久回收小怪爆终极</span>
            </div>
            <!-- 游戏信息 -->
       </div>
       <button class="download_btn_header">

    </button>
    </div>
    
    <!-- 游戏banner -->
    <div class="banner">
        
       
    </div>

    <!-- 领取登录福利 -->
    <div class="login_gift">
         <!-- 标题 -->
        <img class="login_gift_title" src="resource/login_gift_title.png">
        <!-- 登录福利列表 -->
        <div class="login_gift_list">
            <div class="login_gift_item">
                <img src="resource/gift_4.png">
                <div>5000积分</div>
            </div>
            <div class="login_gift_item">
                <img src="resource/gift_2.png">
                <div>狂暴神技</div>
            </div>
            <div class="login_gift_item">
                <img src="resource/gift_3.png">
                <div>琥珀套装</div>
            </div>
            <div class="login_gift_item">
                <img src="resource/gift_1.png">
                <div>四格神器</div>
            </div>
        </div>
        <!-- 领取按钮 -->
        <button class="login_gift_btn">

        </button>
    </div>

    <!-- 抽奖 -->
    <div class="luck_draw">
        <!-- 标题 -->
        <img src="resource/luck_draw_title.png">
        <!-- 奖池 -->
        <div class="jackpot">
            <!-- 奖池列表 -->
            <div class="jackpot_list">
                <div class="jackpot_item">
                    <img src="resource/jackpot_1.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_2.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_3.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_4.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_5.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_6.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_7.png">
                </div>
                <div class="jackpot_item">
                    <img src="resource/jackpot_8.png">
                </div>
            </div>
            <!-- 抽奖按钮 -->
            <button class="luck_draw_btn">

            </button>
        </div>
        
        
        
    </div>

    <!-- 攻略 -->
    <div class="strategy">
        <!-- 标题 -->
        <img src="resource/strategy_title.png">
        <!-- 攻略内容 -->
        <div class="strategy_content">
            吃饭睡觉打豆豆
        </div>
    </div>

    <!-- 底部下载按钮 -->
    <button class="load_btn_bottom">

    </button>
</body>
</html>
        """.trimIndent()

        web_detail.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
    }

    private var currentPicPosition = 0

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initView() {
        val emulator = EmulatorDetectUtil.isEmulator(this)
        if (emulator) {
            tv_detail_tips.visibility = View.VISIBLE
        } else {
            tv_detail_tips.visibility = View.GONE
        }

        RxView.clicks(iv_detail_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(ll_detail_start)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    val phone = SPUtils.getString(SPArgument.PHONE_NUMBER, null)
                    if (phone.isNullOrBlank()) {
                        DialogUtils.showDefaultDialog(
                            this,
                            "未绑定手机号",
                            "需要绑定手机号才能开始游戏",
                            "暂不绑定",
                            "立即绑定",
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    startActivity(
                                        Intent(
                                            this@GameDetailActivity,
                                            AccountSafeActivity::class.java
                                        )
                                    )
                                }
                            }
                        )
                    } else {
                        toStartGame()
                    }
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }

        picAdapter4Small = BaseAdapterWithPosition.Builder<String>()
            .setData(picLists)
            .setLayoutId(R.layout.item_game_pic_small)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData)
                    .into(itemView.iv_item_gamePicSmall)

                if (currentPicPosition == position) {
                    itemView.view_item_gamePicSmall.visibility = View.GONE
                    itemView.rl_item_gamePicSmall.setBackgroundResource(R.drawable.bg_pic_selected)
                } else {
                    itemView.view_item_gamePicSmall.visibility = View.VISIBLE
                    itemView.rl_item_gamePicSmall.setBackgroundResource(R.drawable.bg_pic_unselected)
                    RxView.clicks(itemView.rootView)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            currentPicPosition = position
                            rv_detail_pic.scrollToPosition(currentPicPosition)
                            picAdapter4Small.notifyDataSetChanged()
                        }
                }
            }
            .create()
        rv_detail_pic_small.adapter = picAdapter4Small

        val centerLayoutManager = CenterLayoutManager(this, OrientationHelper.HORIZONTAL)
        rv_detail_pic_small.layoutManager = centerLayoutManager

        picAdapter = BaseAdapterWithPosition.Builder<String>()
            .setData(picLists)
            .setLayoutId(R.layout.item_game_pic)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData)
                    .into(itemView.iv_item_gamePic)

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        mHandler.removeMessages(0)
                        val intent = Intent(this, ShowPicActivity::class.java)
                        intent.putExtra(ShowPicActivity.POSITION, position)
                        intent.putStringArrayListExtra(
                            ShowPicActivity.LIST,
                            picLists as ArrayList<String>
                        )
                        startActivity(intent)
                    }
            }
            .create()
        rv_detail_pic.adapter = picAdapter
        rv_detail_pic.layoutManager = SafeLinearLayoutManager(this, OrientationHelper.HORIZONTAL)
        PagerSnapHelper().attachToRecyclerView(rv_detail_pic)
        rv_detail_pic.setOnTouchListener(DisInterceptTouchListener())
        rv_detail_pic.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = rv_detail_pic.layoutManager!! as LinearLayoutManager
                currentPicPosition = layoutManager.findFirstVisibleItemPosition()
                centerLayoutManager.smoothScrollToPosition(
                    rv_detail_pic_small,
                    RecyclerView.State(),
                    currentPicPosition
                )
                picAdapter4Small.notifyDataSetChanged()
            }
        })

        val screenWidth = PhoneInfoUtils.getWidth(this)
        val float = screenWidth.toFloat() / 360
        blur_detail_start.refreshView(
            ShapeBlurView.build()
                .setBlurMode(BlurMode.MODE_RECTANGLE)
                .setBlurRadius(5 * float)
                .setDownSampleFactor(0.1f * float)
                .setOverlayColor(Color.parseColor("#FFFFFF"))
        )
    }

    /**
     * 启动游戏
     */
    private fun toStartGame() {
        val isHaveId = SPUtils.getInt(SPArgument.IS_HAVE_ID)
        if (isHaveId == 1) {
            DialogUtils.showBeautifulDialog(this)
            val addPlayingGame = RetrofitUtils.builder().addPlayingGame(gameId, 1)
            addPlayingGameObservable = addPlayingGame.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    DialogUtils.dismissLoading()
                    LogUtils.d("success=>${Gson().toJson(it)}")
                    if (it != null) {
                        when (it.code) {
                            1 -> {
                                //起个子线程的页面
                                startActivity(Intent(this, ProcessActivity::class.java))

                                EventBus.getDefault().post(PlayingDataChange(""))
                                isPlayingGame = true
                                SPUtils.putValue(
                                    SPArgument.GAME_TIME_INFO,
                                    "$gameId-${System.currentTimeMillis()}"
                                )

                                JumpUtils.jump2Game(
                                    this,
                                    gameChannel + Box2GameUtils.getPhoneAndToken(),
                                    gameStyle
                                )
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
                    DialogUtils.dismissLoading()
                    LogUtils.d("fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                })
        } else {
            isPlayingGame = true
            val intent = Intent(this, IdCardActivity::class.java)
            intent.putExtra(IdCardActivity.FROM, 2)
            intent.putExtra(IdCardActivity.GAME_ID, gameId)
            intent.putExtra(IdCardActivity.GAME_CHANNEL, gameChannel)
            intent.putExtra(IdCardActivity.GAME_STYLE, gameStyle)
            startActivity(intent)
        }
    }

    /**
     * 收藏/取消收藏
     */
    private fun toCollectOrUncollectGame() {
        val collectGame = RetrofitUtils.builder().addLikeGame(
            gameId,
            if (isCollect) 0 else 1
        )
        collectGameObservable = collectGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            EventBus.getDefault().post(LikeDataChange(""))
                            if (isCollect) {
                                ToastUtils.show(getString(R.string.string_048))
                            } else {
                                ToastUtils.show(getString(R.string.string_049))
                            }
                            isCollect = !isCollect
                            iv_detail_like.setImageResource(if (isCollect) R.mipmap.icon_like_selected4game else R.mipmap.icon_like_unselect4game)
                            tv_detail_like.text = if (isCollect) "已收藏" else "收藏"
                            tv_detail_like.setTextColor(
                                if (isCollect) Color.parseColor("#ff3159")
                                else Color.parseColor("#534F64")
                            )
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

    class DisInterceptTouchListener : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (v !is ViewGroup) {
                return false
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    v.onTouchEvent(event)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            return false
        }
    }

    /**
     * 获取游戏数据
     */
    private fun getInfo() {
//        DialogUtils.showBeautifulDialog(this)
        val gameInfo = if (null == gameChannelId) {
            RetrofitUtils.builder().gameInfo(game_id = gameId)
        } else {
            RetrofitUtils.builder().gameInfo(game_channel_id = gameChannelId)
        }
        gameInfoObservable = gameInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            DialogUtils.dismissLoading()
                            val data = it.data
                            gameStyle = it.data.game_style
                            toInitView(data)
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                DialogUtils.dismissLoading()
            })
    }

    /**
     * 图片轮播效果
     */
    private var mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            currentPicPosition += 1
            if (currentPicPosition >= picLists.size) {
                currentPicPosition = 0
                rv_detail_pic.scrollToPosition(currentPicPosition)
            } else {
                rv_detail_pic.smoothScrollToPosition(currentPicPosition)
            }
            sendEmptyMessageDelayed(0, 3000)
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
    private fun toInitView(info: GameInfoBean.Data) {
        //修改顶部图片高度
        val width = PhoneInfoUtils.getWidth(this)
        val layoutParams4Rl = rl_detail_pic.layoutParams
        layoutParams4Rl.height = (249.5 * (width / 360f)).toInt()
        val layoutParams4RV = rv_detail_pic.layoutParams
        layoutParams4RV.height = (202.5 * (width / 360f)).toInt()

        //处理头部图片信息
        picLists.clear()
        picLists.addAll(info.game_pic)
        picAdapter.notifyDataSetChanged()
        picAdapter4Small.notifyDataSetChanged()
        mHandler.sendEmptyMessageDelayed(0, 3000)

        gameId = info.game_id
        gameChannel = info.game_channelId
        //处理游戏信息
        Glide.with(this)
            .load(info.game_cover)
            .placeholder(R.mipmap.bg_gray_6)
            .into(iv_detail_icon)

        tv_detail_name.text = info.game_name
        tv_detail_des.text = info.game_desc
        fl_detail.removeAllViews()

        val typeView =
            LayoutInflater.from(this).inflate(R.layout.layout_item_tag, null)
        typeView.tv_tag.text = info.game_type
        typeView.tv_tag.setTextColor(Color.parseColor("#5F60FF"))
        typeView.tv_tag.setBackgroundResource(R.drawable.bg_tag1)
        fl_detail.addView(typeView)
        for (index in info.game_tag.indices) {
            val tagView =
                LayoutInflater.from(this).inflate(R.layout.layout_item_tag, null)
            tagView.tv_tag.text = info.game_tag[index]
            when (index % 2 == 0) {
                true -> {
                    tagView.tv_tag.setTextColor(Color.parseColor("#5CE6FF"))
                    tagView.tv_tag.setBackgroundResource(R.drawable.bg_tag2)
                }
                false -> {
                    tagView.tv_tag.setTextColor(Color.parseColor("#FF5FEB"))
                    tagView.tv_tag.setBackgroundResource(R.drawable.bg_tag3)
                }
            }
            fl_detail.addView(tagView)
        }

        //收藏
        isCollect = info.is_fav == 1
        iv_detail_like.setImageResource(if (isCollect) R.mipmap.icon_like_selected4game else R.mipmap.icon_like_unselect4game)
        tv_detail_like.text = if (isCollect) "已收藏" else "收藏"
        tv_detail_like.setTextColor(
            if (isCollect) Color.parseColor("#ff3159")
            else Color.parseColor("#534F64")
        )
        RxView.clicks(ll_detail_like)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    toCollectOrUncollectGame()
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }

        //免费充值
        RxView.clicks(tv_detail_integral)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    val intent = Intent(this, IntegralActivity::class.java)
                    intent.putExtra(IntegralActivity.GAME_ID, info.game_id)
                    intent.putExtra(IntegralActivity.GAME_ICON, info.game_cover)
                    intent.putExtra(IntegralActivity.GAME_NAME, info.game_name)
                    intent.putExtra(IntegralActivity.GAME_CHANNEL_ID, info.game_channelId)
                    startActivity(intent)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }

        //今日开服
        if (info.game_open_times.size <= 1) {
            ll_detail_service2.visibility = View.GONE
            view_detail_service2.visibility = View.GONE
            view_detail_service2_bg.visibility = View.GONE
            view_detail_service2_bg2.visibility = View.GONE
            val gameOpenTimes = info.game_open_times[0]
            tv_detail_service1.text = getServerNameAndTime(gameOpenTimes)[0]
            tv_detail_time1.text = getServerNameAndTime(gameOpenTimes)[1]
        } else {
            ll_detail_service2.visibility = View.VISIBLE
            view_detail_service2.visibility = View.VISIBLE
            view_detail_service2_bg.visibility = View.VISIBLE
            view_detail_service2_bg2.visibility = View.VISIBLE
            val gameOpenTimes = info.game_open_times[0]
            tv_detail_service1.text = getServerNameAndTime(gameOpenTimes)[0]
            tv_detail_time1.text = getServerNameAndTime(gameOpenTimes)[1]
            val gameOpenTimes1 = info.game_open_times[1]
            tv_detail_service2.text = getServerNameAndTime(gameOpenTimes1)[0]
            tv_detail_time2.text = getServerNameAndTime(gameOpenTimes1)[1]
        }

        //特戒盒子专属礼包
        if (null == info.cdkey || info.cdkey.isEmpty()) {
            ll_detail_code.visibility = View.GONE
        } else {
            ll_detail_code.visibility = View.VISIBLE
            tv_detail_code.text = info.cdkey
            iv_detail_code_title.setImageResource(
                if (BaseAppUpdateSetting.isToPromoteVersion) R.mipmap.gift_title2
                else R.mipmap.gift_title
            )
            tv_detail_codeMsg.text = "礼包奖励: ${info.desc}"
        }

        //游戏详情相关
        tv_detail_days.text = getDays(info.game_update_time)
        tv_detail_strategy.text = info.game_intro.trim()
    }

    /**
     * 获取游戏的开服区名和时间
     */
    private fun getServerNameAndTime(gameOpenService: String): List<String> {
        val index = gameOpenService.lastIndexOf(",")
        return arrayListOf(
            "${getData()}${gameOpenService.substring(0, index)}区",
            "今日 ${gameOpenService.substring(index + 1)}"
        )
    }

    /**
     * 获取今日日期
     */
    @SuppressLint("SimpleDateFormat")
    private fun getData(): String {
        val simpleDateFormat = SimpleDateFormat("MM月dd日")
        return simpleDateFormat.format(System.currentTimeMillis())
    }

    /**
     * 计算更新时间
     */
    private fun getDays(updateTimeMillis: Int): String {
        val currentTimeMillis = System.currentTimeMillis() / 1000
        val l = (currentTimeMillis - updateTimeMillis) / 60 / 60 / 24
        return if (l <= 0) getString(R.string.update_today) else "$l${getString(R.string.days)}更新"
    }

    override fun destroy() {
        mHandler.removeMessages(0)
        EventBus.getDefault().unregister(this)

        gameInfoObservable?.dispose()
        gameInfoObservable = null
        collectGameObservable?.dispose()
        collectGameObservable = null
        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null
        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null
    }

    override fun onResume() {
        super.onResume()
        if (picLists.size > 0) {
            mHandler.sendEmptyMessageDelayed(0, 3000)
        }
        MobclickAgent.onResume(this)
        if (isPlayingGame) {
            isPlayingGame = false
            LogUtils.d("=====退出游戏")
            val gameTimeInfo = SPUtils.getString(SPArgument.GAME_TIME_INFO)
            if (null != gameTimeInfo && gameTimeInfo.split("-").size >= 2) {
                val split = gameTimeInfo.split("-")
                val gameId = split[0].toInt()
                val startTime = split[1].toLong()
                val endTime = System.currentTimeMillis()
                if (endTime - startTime >= 1 * 60 * 1000) {
                    val updateGameTimeInfo = RetrofitUtils.builder().updateGameTimeInfo(
                        gameId,
                        startTime.toString(),
                        endTime.toString()
                    )
                    updateGameTimeInfoObservable = updateGameTimeInfo.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({}, {})
                }
            }
        }
        SPUtils.putValue(SPArgument.GAME_TIME_INFO, null)
    }

    override fun onPause() {
        mHandler.removeMessages(0)
        super.onPause()
        MobclickAgent.onPause(this)
    }
}