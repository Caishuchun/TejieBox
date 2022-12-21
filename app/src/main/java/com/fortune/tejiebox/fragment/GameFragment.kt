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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.GameDetailActivity
import com.fortune.tejiebox.activity.SearchGameActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.BaseGameListInfoBean
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

class GameFragment : Fragment() {
    // 0 首页, 1 在玩, 2收藏
    var type: Int? = null
    private var mView: View? = null

    private var isShake = false

    private var gameListObservable: Disposable? = null
    private var getPlayingListObservable: Disposable? = null
    private var getLikeListObservable: Disposable? = null
    private var deleteLikeGameObservable: Disposable? = null
    private var addPlayingGameObservable: Disposable? = null

    private var mData = mutableListOf<BaseGameListInfoBean>()
    private var mAdapter: BaseAdapterWithPosition<BaseGameListInfoBean>? = null

    private var currentPage = 1
    private var countPage = 1

    companion object {
        @JvmStatic
        fun newInstance(type: Int) =
            GameFragment().apply {
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
            //如果该fragment隐藏了
            if (type == 1 || type == 2) {
                //如果是收藏界面和在玩界面
                isShake = false
                mAdapter?.notifyDataSetChanged()
            }
        } else {
            if (type == 1) {
//                EventBus.getDefault().postSticky(IsHaveNewPlayingGame(false))
            }
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("CheckResult")
    private fun initView() {
        when (type) {
            0 -> {
                mView?.ll_gameFragment_search?.visibility = View.VISIBLE
                mView?.rl_gameFragment_title?.visibility = View.GONE
            }
            1 -> {
                mView?.ll_gameFragment_search?.visibility = View.GONE
                mView?.rl_gameFragment_title?.visibility = View.VISIBLE
                mView?.tv_gameFragment_title?.text = "在玩列表"
            }
            2 -> {
                mView?.ll_gameFragment_search?.visibility = View.GONE
                mView?.rl_gameFragment_title?.visibility = View.VISIBLE
                mView?.tv_gameFragment_title?.text = "收藏列表"
            }
        }

        mView?.ll_gameFragment_search?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    requireActivity().startActivity(
                        Intent(requireContext(), SearchGameActivity::class.java)
                    )
                }
        }

        mView?.tv_gameFragment_cancel?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    isShake = false
                    mAdapter?.notifyDataSetChanged()
                }
        }

        mView?.view_gameFragment_space?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isShake && (type == 1 || type == 2)) {
                        //如果是在玩和收藏界面抖动
                        isShake = false
                        mAdapter?.notifyDataSetChanged()
                    }
                }
        }

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

                Glide.with(this)
                    .load(itemData.game_cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(object : ImageViewTarget<Drawable>(itemView.iv_item_gameFragment_icon) {
                        override fun setResource(resource: Drawable?) {
                            itemView.iv_item_gameFragment_icon.setImageDrawable(resource)
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            super.onResourceReady(resource, transition)
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
                        }
                    })
                itemView.tv_item_gameFragment_name.text = itemData.game_name

                //在玩和收藏,描述替换成在线时长
                if (type == 0) {
                    itemView.tv_item_gameFragment_des.text = itemData.game_desc
                } else {
                    itemView.tv_item_gameFragment_des.text =
                        timeFormat4Hours(itemData.duration_sum.toLong())
                }

                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(requireActivity(), GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        requireActivity().startActivity(intent)
                    }

                RxView.longClicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .takeWhile { type != 0 }
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
                                    mAdapter?.notifyDataSetChanged()
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
            .create()

        mView?.rv_gameFragment_game?.let {
            it.adapter = mAdapter
            it.layoutManager = SafeStaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
        }

        mView?.rv_gameFragment_game?.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val mFirstVisibleItems: IntArray? = null
                val position =
                    (mView?.rv_gameFragment_game?.layoutManager as StaggeredGridLayoutManager)
                        .findFirstVisibleItemPositions(mFirstVisibleItems)
                if (currentPage >= 2 && position[0] >= (currentPage - 2) * 32 + 16) {
                    if (currentPage < countPage) {
                        currentPage++
                        getGameList(needLoading = false)
                    } else {
                        mView?.refresh_gameFragment_game?.finishLoadMoreWithNoMoreData()
                    }
                }
            }
        })

        mView?.refresh_gameFragment_game?.let {
            it.setEnableRefresh(true)
            it.setEnableLoadMore(type == 0)
            it.setEnableLoadMoreWhenContentNotFull(false)
            it.setRefreshHeader(MaterialHeader(requireActivity()))
            it.setRefreshFooter(ClassicsFooter(requireActivity()))
            it.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
                override fun onRefresh(refreshLayout: RefreshLayout) {
                    if (type == 0) {
                        currentPage = 1
                    }
                    getInfo(false)
                }

                override fun onLoadMore(refreshLayout: RefreshLayout) {
                    if (type == 0) {
                        if (currentPage < countPage) {
                            currentPage++
                            getInfo(false)
                        } else {
                            mView?.refresh_gameFragment_game?.finishLoadMoreWithNoMoreData()
                        }
                    }
                }
            })
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
            0 -> {
                getGameList(isNeedDialog)
            }
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
     * 获取游戏列表
     */
    private fun getGameList(needLoading: Boolean) {
        if (needLoading) {
            DialogUtils.showBeautifulDialog(requireContext())
        }
        val gameList = RetrofitUtils.builder().gameList(currentPage)
        gameListObservable = gameList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    var count = 0
                    for (info in it.data.list) {
                        if (info.game_top == 1) {
                            count++
                        }
                    }
                    DialogUtils.dismissLoading()
                    mView?.refresh_gameFragment_game?.finishRefresh()
                    mView?.refresh_gameFragment_game?.finishLoadMore()
                    if (it != null) {
                        when (it.code) {
                            1 -> {
                                if (currentPage == 1) {
                                    mData.clear()
                                    mAdapter?.notifyDataSetChanged()
                                }
                                if (it.data.list.isNotEmpty()) {
                                    val count = it.data.paging.count
                                    val limit = it.data.paging.limit
                                    countPage = count / limit + if (count % limit == 0) 0 else 1
                                    mData.addAll(it.data.list)
                                    mAdapter?.notifyItemChanged(mData.size - 1)
                                    mView?.tv_gameFragment_nothing?.let { tv ->
                                        tv.visibility = View.GONE
                                    }
                                } else {
                                    mView?.tv_gameFragment_nothing?.let { tv ->
                                        tv.visibility = View.VISIBLE
                                    }
                                }
                                if (currentPage == 1 && countPage > 1) {
                                    currentPage++
                                    getGameList(needLoading = false)
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
                    mView?.refresh_gameFragment_game?.finishRefresh()
                    mView?.refresh_gameFragment_game?.finishLoadMore()
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                }
            )
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
//            EventBus.getDefault().postSticky(IsHaveNewPlayingGame(true))
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun login(loginStatusChange: LoginStatusChange) {
        if (loginStatusChange == null) {
            return
        }
        if (type != 0) {
            getInfo(false)
        }
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
        gameListObservable?.dispose()
        gameListObservable = null

        getLikeListObservable?.dispose()
        getLikeListObservable = null

        getPlayingListObservable?.dispose()
        getPlayingListObservable = null

        deleteLikeGameObservable?.dispose()
        deleteLikeGameObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null
        super.onDestroy()
    }
}