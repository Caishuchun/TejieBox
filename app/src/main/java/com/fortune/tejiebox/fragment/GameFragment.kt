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
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.GameDetailActivity
import com.fortune.tejiebox.activity.SearchGameActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.BaseGameListInfoBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.http.RetrofitUtils
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
import java.util.concurrent.TimeUnit

class GameFragment : Fragment() {

    private var gameListObservable: Disposable? = null
    private var mData = mutableListOf<BaseGameListInfoBean>()
    private var mAdapter: BaseAdapterWithPosition<BaseGameListInfoBean>? = null
    private var mView: View? = null

    private var isFirstCreate = true
    private var currentPage = 1
    private var countPage = 1
    private var currentRandom = 0

    companion object {
        @JvmStatic
        fun newInstance() = GameFragment()

        private var mGameId = -1

        /**
         * 传GameId
         */
        fun setGameId(gameId: Int) {
            mGameId = gameId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_game, container, false)
        isFirstCreate = true
        initView()
        getGameList(true)
        return mView
    }

    /**
     * 初始化布局
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initView() {
        mView?.ll_gameFragment_search?.visibility = View.VISIBLE
        mView?.rl_gameFragment_title?.visibility = View.GONE

        mView?.ll_gameFragment_search?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    requireActivity().startActivity(
                        Intent(requireContext(), SearchGameActivity::class.java)
                    )
                }
        }

        mAdapter = BaseAdapterWithPosition.Builder<BaseGameListInfoBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_game_fragment_game)
            .addBindView { itemView, itemData, position ->

                itemView.runView_item_gameFragment.visibility = View.GONE
                itemView.iv_item_gameFragment_type.visibility = View.GONE

                //Tag是为了防止图片重复
                itemView.iv_item_gameFragment_icon.setTag(R.id.image, position)
                itemView.iv_item_gameFragment_icon.setImageResource(R.mipmap.bg_gray_6)
                Glide.with(this)
                    .load(itemData.game_cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .skipMemoryCache(false)
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

                itemView.tv_item_gameFragment_name.text = itemData.game_name
                itemView.tv_item_gameFragment_des.text = itemData.game_desc

                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(requireActivity(), GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        intent.putExtra(GameDetailActivity.GAME_IS_INTEGRAL, itemData.is_integral)
                        requireActivity().startActivity(intent)
                    }
            }
            .create(true)

        mView?.rv_gameFragment_game?.let {
            //取消默认动画
            (it.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

            it.setHasFixedSize(true)
            it.setItemViewCacheSize(20)
            it.isDrawingCacheEnabled = true
            it.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
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
                if (currentPage >= 2 && position[0] >= (currentPage - 2) * 32) {
//                if (mData.size < position[0] + 10) {
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
            it.setEnableLoadMore(true)
            it.setEnableLoadMoreWhenContentNotFull(false)
            it.setRefreshHeader(MaterialHeader(requireActivity()))
            it.setRefreshFooter(ClassicsFooter(requireActivity()))
            it.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
                override fun onRefresh(refreshLayout: RefreshLayout) {
                    currentPage = 1
                    getGameList(false)
                }

                override fun onLoadMore(refreshLayout: RefreshLayout) {
                    if (currentPage < countPage) {
                        currentPage++
                        getGameList(false)
                    } else {
                        mView?.refresh_gameFragment_game?.finishLoadMoreWithNoMoreData()
                    }
                }
            })
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!isFirstCreate && !hidden) {
            LogUtils.d("++++++++++++++首页出现-onHiddenChanged")
            val lastGetGameListTime = SPUtils.getLong(SPArgument.GET_GAME_LIST_TIME)
            if (System.currentTimeMillis() - lastGetGameListTime > 1000 * 60 * 10) {
                //时间过了十分钟
                currentPage = 1
                getGameList(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstCreate) {
            LogUtils.d("++++++++++++++首页出现-onResume")
            val lastGetGameListTime = SPUtils.getLong(SPArgument.GET_GAME_LIST_TIME)
            if (System.currentTimeMillis() - lastGetGameListTime > 1000 * 60 * 10) {
                //时间过了十分钟
                currentPage = 1
                getGameList(false)
            }
        }
    }

    /**
     * 获取游戏列表
     */
    private fun getGameList(needLoading: Boolean) {
        if (needLoading && isAdded) {
            DialogUtils.showBeautifulDialog(requireContext())
        }
        if (currentPage == 1) {
            val lastGetGameListTime = SPUtils.getLong(SPArgument.GET_GAME_LIST_TIME)
            if (System.currentTimeMillis() - lastGetGameListTime > 1000 * 60 * 10) {
                //时间过了十分钟
                SPUtils.putValue(SPArgument.GET_GAME_LIST_TIME, System.currentTimeMillis())
                currentRandom = (0..10000).random()
            }
        }
        val gameList = RetrofitUtils.builder().gameList(currentPage, currentRandom)
        gameListObservable = gameList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (needLoading) isFirstCreate = false
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
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
                                    val oldSize = mData.size
                                    for (list in it.data.list) {
                                        if (!mData.contains(list)) {
                                            mData.add(list)
                                        }
                                    }
                                    if (currentPage == 1) {
                                        mAdapter?.notifyItemRangeChanged(0, mData.size)
                                        mView?.rv_gameFragment_game?.postDelayed(
                                            {
                                                if (mGameId != -1) {
                                                    val intent = Intent(
                                                        requireActivity(),
                                                        GameDetailActivity::class.java
                                                    )
                                                    intent.putExtra(
                                                        GameDetailActivity.GAME_ID,
                                                        mGameId
                                                    )
                                                    requireActivity().startActivity(intent)
                                                    mGameId = -1
                                                }
                                            }, 1000
                                        )
                                    } else {
                                        mAdapter?.notifyItemRangeInserted(
                                            if (oldSize > 1) oldSize - 1 else 0,
                                            it.data.list.size
                                        )
                                    }
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
                    if (needLoading) isFirstCreate = false
                    mView?.refresh_gameFragment_game?.finishRefresh()
                    mView?.refresh_gameFragment_game?.finishLoadMore()
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                }
            )
    }

    /**
     * 打开游戏详情页
     */
    fun openGameDetailActivity(gameId: Int) {
        val intent = Intent(requireActivity(), GameDetailActivity::class.java)
        intent.putExtra(GameDetailActivity.GAME_ID, gameId)
        requireActivity().startActivity(intent)
    }

    override fun onDestroy() {
        gameListObservable?.dispose()
        gameListObservable = null
        super.onDestroy()
    }
}