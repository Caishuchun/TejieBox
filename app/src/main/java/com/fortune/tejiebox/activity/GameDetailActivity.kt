package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseActivity
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
        getInfo()
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
                    toStartGame()
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
                                    gameChannel + Box2GameUtils.getPhoneAndToken()
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
        DialogUtils.showBeautifulDialog(this)
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
                        .subscribe({
                        }, {})
                }
            }
            SPUtils.putValue(SPArgument.GAME_TIME_INFO, null)
        }
    }

    override fun onPause() {
        mHandler.removeMessages(0)
        super.onPause()
        MobclickAgent.onPause(this)
    }
}