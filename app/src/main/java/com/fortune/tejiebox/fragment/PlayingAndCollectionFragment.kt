package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.GameDetailActivity
import com.fortune.tejiebox.activity.IdCardActivity
import com.fortune.tejiebox.activity.ProcessActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.BaseGameListInfoBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.LikeDataChange
import com.fortune.tejiebox.event.LoginStatusChange
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.unity3d.player.JumpUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_game.view.*
import kotlinx.android.synthetic.main.item_game_fragment_game.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

private const val TYPE = "type"

/**
 * 游戏列表页面, 在玩和收藏
 */
class PlayingAndCollectionFragment : Fragment() {
    //1 在玩, 2收藏
    private var type: Int? = null
    private var mView: View? = null

    private var isShake = false

    private var getPlayingListObservable: Disposable? = null
    private var getLikeListObservable: Disposable? = null
    private var deleteLikeGameObservable: Disposable? = null
    private var addPlayingGameObservable: Disposable? = null

    private var allAccountObservable: Disposable? = null
    private var saveAccountObservable: Disposable? = null

    private var mData = mutableListOf<BaseGameListInfoBean>()
    private var mAdapter: BaseAdapterWithPosition<BaseGameListInfoBean>? = null

    companion object {
        @JvmStatic
        fun newInstance(type: Int) =
            PlayingAndCollectionFragment().apply {
                arguments = Bundle().apply {
                    putInt(TYPE, type)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        arguments?.let {
            type = it.getInt(TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_game, container, false)

        initView()
        getInfo()

        return mView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            //如果是收藏界面和在玩界面隐藏了
            if (isShake) {
                isShake = false
                mAdapter?.notifyDataSetChanged()
            }
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initView() {
//        when (type) {
//            1 -> {
//                mView?.ll_gameFragment_search?.visibility = View.GONE
//                mView?.rl_gameFragment_title?.visibility = View.VISIBLE
//                mView?.tv_gameFragment_title?.text = "在玩列表"
//            }
//            2 -> {
//                mView?.ll_gameFragment_search?.visibility = View.GONE
//                mView?.rl_gameFragment_title?.visibility = View.VISIBLE
//                mView?.tv_gameFragment_title?.text = "收藏列表"
//            }
//        }
        mView?.ll_gameFragment_search?.visibility = View.GONE
        mView?.rl_gameFragment_title?.visibility = View.GONE

        mView?.tv_gameFragment_cancel?.let { it ->
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isShake) {
                        mView?.tv_gameFragment_cancel?.let { it.text = "删除" }
                    } else {
                        mView?.tv_gameFragment_cancel?.let { it.text = "取消" }
                    }
                    isShake = !isShake
                    mAdapter?.notifyDataSetChanged()
                }
        }

        mAdapter = BaseAdapterWithPosition.Builder<BaseGameListInfoBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_game_fragment_game)
            .addBindView { itemView, itemData, position ->
                if (itemData.game_id < 10000) {
                    if (isShake) {
                        FlipAnimUtils.startShakeByPropertyAnim(
                            itemView,
                            1f, 1f,
                            2f,
                            1000
                        )
                        itemView.iv_item_gameFragment_delete.visibility = View.VISIBLE
                        mView?.tv_gameFragment_cancel?.let { it.text = "取消" }
                    } else {
                        FlipAnimUtils.stopShakeByPropertyAnim(itemView)
                        itemView.iv_item_gameFragment_delete.visibility = View.GONE
                        mView?.tv_gameFragment_cancel?.let { it.text = "删除" }
                    }

                    itemView.runView_item_gameFragment.visibility = View.GONE
                    itemView.iv_item_gameFragment_type.visibility = View.GONE

                    //Tag是为了防止图片重复
                    itemView.iv_item_gameFragment_icon.setTag(R.id.image, position)
                    Glide.with(this)
                        .load(itemData.game_cover)
                        .placeholder(R.mipmap.bg_gray_6)
                        .skipMemoryCache(true)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                itemView.runView_item_gameFragment.visibility =
                                    if (itemData.game_top == 1) View.VISIBLE else View.GONE
                                when (itemData.icon_type) {
                                    1 -> {
                                        itemView.iv_item_gameFragment_type.visibility = View.VISIBLE
                                        itemView.iv_item_gameFragment_type.setImageResource(R.mipmap.icon_new)
                                    }
                                    2 -> {
                                        itemView.iv_item_gameFragment_type.visibility = View.VISIBLE
                                        itemView.iv_item_gameFragment_type.setImageResource(R.mipmap.icon_hot)
                                    }
                                    else -> {
                                        itemView.iv_item_gameFragment_type.visibility = View.GONE
                                    }
                                }
                                return false
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(itemView.iv_item_gameFragment_icon)

                    //在玩和收藏,描述替换成在线时长
                    itemView.tv_item_gameFragment_des.visibility = View.VISIBLE
                    itemView.tv_item_gameFragment_des.text =
                        timeFormat4Hours(itemData.duration_sum.toLong())
                } else {
                    itemView.runView_item_gameFragment.visibility = View.GONE
                    itemView.iv_item_gameFragment_type.visibility = View.GONE
                    itemView.iv_item_gameFragment_icon.setImageResource(R.mipmap.game_icon)
                    itemView.tv_item_gameFragment_des.visibility = View.INVISIBLE
                }
                itemView.tv_item_gameFragment_name.text = itemData.game_name
                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.game_id < 10000) {
                            val intent = Intent(requireActivity(), GameDetailActivity::class.java)
                            intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                            requireActivity().startActivity(intent)
                        } else {
                            //启动弹框啥的
                            toGetAllAccount(
                                itemData.game_id,
                                itemData.game_name,
                                itemData.game_channelId
                            )
                        }
                    }

                RxView.longClicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        isShake = !isShake
                        mAdapter?.notifyDataSetChanged()
                    }

                RxView.clicks(itemView.iv_item_gameFragment_delete)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        DialogUtils.showDefaultDialog(requireContext(),
                            "删除游戏",
                            "确定从${if (type == 1) "在玩列表" else "收藏列表"}中删除\"${itemData.game_name}\"吗?",
                            "取消",
                            "确认",
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    mData.removeAt(position)
                                    toDeleteCurrentGame(itemData.game_id)
                                    mAdapter?.notifyItemRemoved(position)
                                    if (mData.size == 0) {
                                        isShake = false
                                        mView?.tv_gameFragment_cancel?.let {
                                            it.text = "删除"
                                        }
                                        mView?.tv_gameFragment_nothing?.let {
                                            it.visibility = View.VISIBLE
                                        }
                                    }
                                }
                            })
                    }
            }
            .create(true)

        mView?.rv_gameFragment_game?.let {
            it.setHasFixedSize(true)
            it.setItemViewCacheSize(20)
            it.isDrawingCacheEnabled = true
            it.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
            (it.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false //取消默认动画
            it.adapter = mAdapter
            it.layoutManager = SafeStaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
        }

        mView?.refresh_gameFragment_game?.let {
            it.setEnableRefresh(true)
            it.setEnableLoadMore(false)
            it.setEnableLoadMoreWhenContentNotFull(false)
            it.setRefreshHeader(MaterialHeader(requireActivity()))
            it.setRefreshFooter(ClassicsFooter(requireActivity()))
            it.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
                override fun onRefresh(refreshLayout: RefreshLayout) {
                    getInfo(false)
                }

                override fun onLoadMore(refreshLayout: RefreshLayout) {

                }
            })
        }
    }

    /**
     * 获取全部账号
     */
    private fun toGetAllAccount(gameId: Int, gameName: String, gameChannelId: String) {
        DialogUtils.showBeautifulDialog(requireContext())
        val allAccount = RetrofitUtils.builder().allAccount()
        allAccountObservable = allAccount.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            DialogUtils.showStartGameDialog(
                                requireActivity(),
                                gameName,
                                it.data,
                                object : DialogUtils.OnDialogListener4StartGame {
                                    override fun tejieStart() {
                                        toStartGame(gameId, gameChannelId, null, null)
                                    }

                                    override fun accountStart(account: String, password: String) {
                                        toSaveAccount(gameId, gameChannelId, account, password)
                                    }
                                })
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
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
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    /**
     * 校验保存账号
     */
    private fun toSaveAccount(
        gameId: Int,
        gameChannelId: String,
        account: String,
        password: String
    ) {
        val saveAccount = RetrofitUtils.builder().saveAccount(account, password)
        saveAccountObservable = saveAccount.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            DialogUtils.dismissLoading()
                            toStartGame(gameId, gameChannelId, account, password)
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
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
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    /**
     * 启动游戏
     */
    private fun toStartGame(gameId: Int, gameChannel: String, account: String?, password: String?) {
        val isHaveId = SPUtils.getInt(SPArgument.IS_HAVE_ID)
        if (isHaveId == 1) {
            DialogUtils.showBeautifulDialog(requireContext())
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
                                startActivity(Intent(requireContext(), ProcessActivity::class.java))

                                EventBus.getDefault().post(PlayingDataChange(""))
                                if (account == null) {
                                    JumpUtils.jump2Game(
                                        requireActivity(),
                                        "$gameChannel${Box2GameUtils.getPhoneAndToken()}",
                                        null
                                    )
                                } else {
                                    JumpUtils.jump2Game(
                                        requireActivity(),
                                        "$gameChannel|phone=$account|token=$password|logintype=2",
                                        null
                                    )
                                }
                            }
                            -1 -> {
                                ToastUtils.show(it.msg)
                                ActivityManager.toSplashActivity(requireActivity())
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
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                })
        } else {
            val intent = Intent(requireContext(), IdCardActivity::class.java)
            intent.putExtra(IdCardActivity.FROM, 2)
            intent.putExtra(IdCardActivity.GAME_ID, gameId)
            intent.putExtra(IdCardActivity.GAME_CHANNEL, gameChannel)
            intent.putExtra(IdCardActivity.ACCOUNT, account)
            intent.putExtra(IdCardActivity.PASSWORD, password)
            startActivity(intent)
        }
    }

    /**
     * 格式化在线时长 "在线XX.X小时"
     */
    private fun timeFormat4Hours(time: Long): String {
        var hours = if (time <= 0L) {
            0.1
        } else {
            val hour = time.toFloat() / (60L * 60L)
            val decimalFormat = DecimalFormat("##0.0")
            val format = decimalFormat.format(hour)
            format.toDouble()
        }
        if (hours < 0.1) {
            hours = 0.1
        }
        return "时长${hours}小时"
    }

    /**
     * 获取游戏数据
     */
    private fun getInfo(isNeedDialog: Boolean = true) {
        when (type) {
            1 -> {
                if (MyApp.getInstance().isHaveToken()) {
                    getPlayingList(isNeedDialog)
                } else {
                    mData.clear()
                    mAdapter?.notifyDataSetChanged()
                }
            }
            2 -> {
                if (MyApp.getInstance().isHaveToken()) {
                    getLikeList(isNeedDialog)
                } else {
                    mData.clear()
                    mAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * 获取收藏列表
     */
    private fun getLikeList(isNeedDialog: Boolean) {
        if (isNeedDialog) {
            DialogUtils.showBeautifulDialog(requireContext())
        }
        val likeGame = RetrofitUtils.builder().likeGame()
        getLikeListObservable = likeGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mView?.refresh_gameFragment_game?.finishRefresh()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data.isNotEmpty()) {
                                mView?.tv_gameFragment_nothing?.let { tv ->
                                    tv.visibility = View.GONE
                                }
                            } else {
                                mView?.tv_gameFragment_nothing?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                }
                            }

                            mData.clear()
                            mData.addAll(it.data)
                            mAdapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                mView?.refresh_gameFragment_game?.finishRefresh()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    /**
     * 获取在玩列表
     */
    private fun getPlayingList(isNeedDialog: Boolean) {
        if (isNeedDialog) {
            DialogUtils.showBeautifulDialog(requireContext())
        }
        val playingGame = RetrofitUtils.builder().playingGame()
        getPlayingListObservable = playingGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mView?.refresh_gameFragment_game?.finishRefresh()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data.isNotEmpty()) {
                                mView?.tv_gameFragment_nothing?.let { tv ->
                                    tv.visibility = View.GONE
                                }
                            } else {
                                mView?.tv_gameFragment_nothing?.let { tv ->
                                    tv.visibility = View.VISIBLE
                                }
                            }

                            mData.clear()
                            mData.addAll(it.data)
                            mAdapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(requireActivity())
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                mView?.refresh_gameFragment_game?.finishRefresh()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun likeDataChange(data: LikeDataChange) {
        if (data == null) {
            return
        }
        if (type == 2) {
            getInfo(false)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun playingDataChange(data: PlayingDataChange) {
        if (data == null) {
            return
        }
        if (type == 1) {
            getInfo(false)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        if (loginStatusChange == null) {
            return
        }
        getInfo(false)
    }

    /**
     * 删除当前游戏
     */
    private fun toDeleteCurrentGame(gameId: Int) {
        when (type) {
            1 -> {
                toDeletePlayingGame(gameId)
            }
            2 -> {
                toDeleteLikeGame(gameId)
            }
        }
    }

    /**
     * 删除收藏游戏__取消收藏
     */
    private fun toDeleteLikeGame(gameId: Int) {
        val collectGame = RetrofitUtils.builder().addLikeGame(gameId, 0)
        deleteLikeGameObservable = collectGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, {
            })
    }

    /**
     * 删除在玩游戏
     */
    private fun toDeletePlayingGame(gameId: Int) {
        val addPlayingGame = RetrofitUtils.builder().addPlayingGame(gameId, 0)
        addPlayingGameObservable = addPlayingGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, {})
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)

        getLikeListObservable?.dispose()
        getLikeListObservable = null

        getPlayingListObservable?.dispose()
        getPlayingListObservable = null

        deleteLikeGameObservable?.dispose()
        deleteLikeGameObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null

        allAccountObservable?.dispose()
        allAccountObservable = null

        saveAccountObservable?.dispose()
        saveAccountObservable = null
        super.onDestroy()
    }
}