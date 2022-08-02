package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.GameDetailActivity
import com.fortune.tejiebox.activity.SearchGameActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.BannerListBean
import com.fortune.tejiebox.bean.GameListBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.RoundImageView
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.unity3d.player.JumpUtils
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.config.IndicatorConfig
import com.youth.banner.indicator.CircleIndicator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.banner_pic.view.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import kotlinx.android.synthetic.main.item_main_frament_game.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import net.center.blurview.ShapeBlurView
import net.center.blurview.enu.BlurMode
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit


class MainFragment : Fragment() {

    private var mView: View? = null
    private var mAdapter4Game: BaseAdapterWithPosition<GameListBean.Data.Game>? = null
    private var mAdapter4Banner: ImageAdapter? = null

    private var bannerObservable: Disposable? = null
    private var gameListObservable: Disposable? = null
    private var addPlayingGameObservable: Disposable? = null

    private var mData4GameList = mutableListOf<GameListBean.Data.Game>()
    private var mData4Banner = mutableListOf<BannerListBean.Data>()

    private var bannerUrlList = mutableListOf("")

    private var currentPage = 1
    private var countPage = 0
    private var isPlayingGame = false

    private var updateGameTimeInfoObservable: Disposable? = null

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_main, container, false)
        initView()
        getBanner()
        getGameList(needLoading = true, isRefresh = false)
        return mView
    }

    /**
     * 获取游戏列表
     */
    private fun getGameList(needLoading: Boolean, isRefresh: Boolean) {
        if (needLoading) {
            DialogUtils.showBeautifulDialog(requireContext())
        }
        val gameList = RetrofitUtils.builder().gameList(currentPage)
        gameListObservable = gameList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    DialogUtils.dismissLoading()
                    if (isRefresh) {
                        mView?.refresh_mainFragment_game?.finishRefresh()
                    } else {
                        mView?.refresh_mainFragment_game?.finishLoadMore()
                    }
                    if (it != null) {
                        when (it.code) {
                            1 -> {
                                if (currentPage == 1) {
                                    mData4GameList.clear()
                                    mAdapter4Game?.notifyDataSetChanged()
                                }
                                if (it.data.list.isNotEmpty()) {
                                    val count = it.data.paging.count
                                    val limit = it.data.paging.limit
                                    countPage = count / limit + if (count % limit == 0) 0 else 1
                                    mData4GameList.addAll(it.data.list)
                                    mAdapter4Game?.notifyItemChanged(mData4GameList.size - 1)
                                }
                                if (currentPage == 1 && countPage > 1) {
                                    currentPage++
                                    getGameList(needLoading = false, isRefresh = false)
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
                    if (isRefresh) {
                        mView?.refresh_mainFragment_game?.finishRefresh()
                    } else {
                        mView?.refresh_mainFragment_game?.finishLoadMore()
                    }
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                }
            )
    }

    /**
     * 获取轮播图信息
     */
    private fun getBanner() {
        val bannerList = RetrofitUtils.builder().bannerList()
        bannerObservable = bannerList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    DialogUtils.dismissLoading()
                    if (it != null) {
                        when (it.code) {
                            1 -> {
                                mData4Banner.clear()
                                bannerUrlList.clear()
                                if (it.data.isNotEmpty()) {
                                    mData4Banner.addAll(it.data)
                                    for (index in it.data.indices) {
                                        bannerUrlList.add(it.data[index].game_cover)
                                    }
                                } else {
                                    bannerUrlList.add("")
                                }
                                mAdapter4Banner?.notifyDataSetChanged()
                                mView?.banner_mainFragment?.let { banner ->
                                    banner.setCurrentItem(1)
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
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                }
            )
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        mView?.ll_mainFragment_search?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    requireActivity().startActivity(
                        Intent(
                            requireActivity(),
                            SearchGameActivity::class.java
                        )
                    )
                }
        }

        mAdapter4Banner = ImageAdapter(requireContext(), mData4Banner, bannerUrlList)
        mView?.banner_mainFragment?.let {
            val screenWidth = PhoneInfoUtils.getWidth(requireActivity())
            it.addBannerLifecycleObserver(this)
                .setAdapter(mAdapter4Banner)
                .setIndicator(CircleIndicator(requireContext()))
                .setIndicatorMargins(
                    IndicatorConfig.Margins(
                        0,
                        0,
                        0,
                        (screenWidth.toFloat() / 360 * 25).toInt()
                    )
                )
        }

        mAdapter4Game = BaseAdapterWithPosition.Builder<GameListBean.Data.Game>()
            .setLayoutId(R.layout.item_main_frament_game)
            .setData(mData4GameList)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData.game_icon)
                    .into(itemView.iv_item_mainFragment_icon)

                itemView.tv_item_mainFragment_gameName.text = itemData.game_name
                itemView.tv_item_mainFragment_gameDes.text = itemData.game_desc

                val typeView =
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.layout_item_tag, null)
                typeView.tv_tag.text = itemData.game_type
                typeView.tv_tag.setTextColor(Color.parseColor("#5F60FF"))
                typeView.tv_tag.setBackgroundResource(R.drawable.bg_tag1)
                itemView.flowLayout_item_mainFragment.addView(typeView)
                val size = itemData.game_tag.size
                for (index in 0 until size) {
                    val tagView =
                        LayoutInflater.from(requireContext())
                            .inflate(R.layout.layout_item_tag, null)
                    tagView.tv_tag.text = itemData.game_tag[index]
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
                    itemView.flowLayout_item_mainFragment.addView(tagView)
                }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(requireActivity(), GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        requireContext().startActivity(intent)
                    }

                RxView.clicks(itemView.tv_item_mainFragment_start)
                    .throttleFirst(
                        200,
                        TimeUnit.MILLISECONDS
                    )
                    .subscribe {
                        if (MyApp.getInstance().isHaveToken()) {
                            toStartGame(itemData.game_id, itemData.game_channelId)
                        } else {
                            LoginUtils.toQuickLogin(requireActivity())
                        }
                    }
            }
            .create()
        mView?.rv_mainFragment_game?.let {
            it.adapter = mAdapter4Game
            it.layoutManager = SafeLinearLayoutManager(requireActivity())
        }

        mView?.rv_mainFragment_game?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val position =
                    (mView?.rv_mainFragment_game?.layoutManager as LinearLayoutManager)
                        .findFirstVisibleItemPosition()
//                LogUtils.d("onScrolled->position:$position")
                if (currentPage >= 2 && position >= (currentPage - 2) * 15) {
                    if (currentPage < countPage) {
                        currentPage++
                        getGameList(needLoading = false, isRefresh = false)
                    } else {
                        mView?.refresh_mainFragment_game?.finishLoadMoreWithNoMoreData()
                    }
                }
            }
        })

        mView?.refresh_mainFragment_game?.let {
            it.setEnableRefresh(true)
            it.setEnableLoadMore(true)
            it.setEnableLoadMoreWhenContentNotFull(false)
            it.setRefreshHeader(MaterialHeader(requireActivity()))
            it.setRefreshFooter(ClassicsFooter(requireActivity()))
            it.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
                override fun onRefresh(refreshLayout: RefreshLayout) {
                    currentPage = 1
                    getGameList(needLoading = false, isRefresh = true)
                    getBanner()
                }

                override fun onLoadMore(refreshLayout: RefreshLayout) {
                    if (currentPage < countPage) {
                        currentPage++
                        getGameList(needLoading = false, isRefresh = false)
                    } else {
                        mView?.refresh_mainFragment_game?.finishLoadMoreWithNoMoreData()
                    }
                }
            })
        }

        val screenWidth = PhoneInfoUtils.getWidth(requireActivity())
        val float = screenWidth.toFloat() / 360

        mView?.blur_mainFragment?.let {
            it.refreshView(
                ShapeBlurView.build()
                    .setBlurMode(BlurMode.MODE_RECTANGLE)
                    .setBlurRadius(5 * float)
                    .setDownSampleFactor(0.1f * float)
                    .setCornerRadius(10 * float)
                    .setBorderColor(R.color.white_FFFFFF)
                    .setBorderWidth(0.5f * float)
                    .setOverlayColor(Color.parseColor("#69FFFFFF"))
            )
        }
    }

    /**
     * 启动游戏
     */
    private fun toStartGame(gameId: Int, gameChannelId: String) {
        val addPlayingGame = RetrofitUtils.builder().addPlayingGame(gameId, 1)
        addPlayingGameObservable = addPlayingGame.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            EventBus.getDefault().post(PlayingDataChange(""))
                            isPlayingGame = true
                            SPUtils.putValue(
                                SPArgument.GAME_TIME_INFO,
                                "$gameId-${System.currentTimeMillis()}"
                            )
                            JumpUtils.jump2Game(requireActivity(), gameChannelId)
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

    override fun onResume() {
        super.onResume()
        if (isPlayingGame) {
            isPlayingGame = false
            LogUtils.d("====退出游戏")
            val gameTimeInfo = SPUtils.getString(SPArgument.GAME_TIME_INFO)
            if (null != gameTimeInfo && gameTimeInfo.split("-").size >= 2) {
                val split = gameTimeInfo.split("-")
                val gameId = split[0].toInt()
                val startTime = split[1].toLong()
                val endTime = System.currentTimeMillis()
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
    }

    /**
     * 自定义布局，下面是常见的图片样式，更多实现可以看demo，可以自己随意发挥
     */
    class ImageAdapter(
        context: Context,
        bannerData: MutableList<BannerListBean.Data>,
        urlList: List<String>
    ) :
        BannerAdapter<String, ImageAdapter.BannerViewHolder>(urlList) {
        private var mContext = context
        private var mBannerData = bannerData
        override fun onCreateHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.banner_pic, parent, false)
            return BannerViewHolder(root)
        }

        @SuppressLint("CheckResult")
        override fun onBindView(
            holder: BannerViewHolder,
            url: String,
            position: Int,
            size: Int
        ) {
            Glide.with(mContext)
                .load(url)
                .placeholder(R.drawable.bg_search)
                .into(holder.imageView)

            RxView.clicks(holder.imageView)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    var gameId = -1
                    for (data in mBannerData) {
                        if (data.game_cover == url) {
                            gameId = data.game_id
                            break
                        }
                    }
                    val intent = Intent(mContext, GameDetailActivity::class.java)
                    intent.putExtra(GameDetailActivity.GAME_ID, gameId)
                    mContext.startActivity(intent)
                }
        }

        inner class BannerViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {
            var imageView: RoundImageView = view.riv_banner
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerObservable?.dispose()
        bannerObservable = null

        gameListObservable?.dispose()
        gameListObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null

        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null
    }

}