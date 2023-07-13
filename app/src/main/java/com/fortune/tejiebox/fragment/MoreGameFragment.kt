package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.IdCardActivity
import com.fortune.tejiebox.activity.ProcessActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.AllAccountBean
import com.fortune.tejiebox.bean.BaseGameListInfoBean
import com.fortune.tejiebox.bean.GameInfo4ClipboardBean
import com.fortune.tejiebox.bean.VersionBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.ActivityManager
import com.fortune.tejiebox.utils.Box2GameUtils
import com.fortune.tejiebox.utils.DialogUtils
import com.fortune.tejiebox.utils.HttpExceptionUtils
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.LoginUtils
import com.fortune.tejiebox.utils.SPUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.fortune.tejiebox.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.unity3d.player.JumpUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_game.view.ll_gameFragment_search
import kotlinx.android.synthetic.main.fragment_game.view.refresh_gameFragment_game
import kotlinx.android.synthetic.main.fragment_game.view.rl_gameFragment_title
import kotlinx.android.synthetic.main.fragment_game.view.rv_gameFragment_game
import kotlinx.android.synthetic.main.fragment_game.view.tv_gameFragment_cancel
import kotlinx.android.synthetic.main.fragment_game.view.tv_gameFragment_nothing
import kotlinx.android.synthetic.main.fragment_game.view.tv_gameFragment_title
import kotlinx.android.synthetic.main.item_game_fragment_more_game.view.tv_item_gameFragment_name
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class MoreGameFragment : Fragment() {

    private var gameListObservable: Disposable? = null
    private var mData = mutableListOf<BaseGameListInfoBean>()
    private var mAdapter: BaseAdapterWithPosition<BaseGameListInfoBean>? = null
    private var mView: View? = null

    private var addPlayingGameObservable: Disposable? = null
    private var allAccountObservable: Disposable? = null
    private var saveAccountObservable: Disposable? = null

    private var currentPage = 1
    private var countPage = 1

    companion object {
        @JvmStatic
        fun newInstance() = MoreGameFragment()

        private var gameIdFromClipboardContent = -1
        private var gameNameFromClipboardContent = ""
        private var gameChannelIdFromClipboardContent = ""
        fun setGameInfo(gameId: Int, gameName: String, gameChannelId: String) {
            gameIdFromClipboardContent = gameId
            gameNameFromClipboardContent = gameName
            gameChannelIdFromClipboardContent = gameChannelId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_game, container, false)
        initView()
        getGameList(true)
        return mView
    }
    /**
     * 这是一个时间工具累
     *
     */

    /**
     * 初始化布局
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initView() {
        mView?.ll_gameFragment_search?.visibility = View.GONE
        mView?.rl_gameFragment_title?.visibility = View.VISIBLE
        mView?.tv_gameFragment_cancel?.visibility = View.GONE
        mView?.tv_gameFragment_title?.text = "全部游戏"

        mAdapter = BaseAdapterWithPosition.Builder<BaseGameListInfoBean>()
            .setData(mData)
            .setLayoutId(R.layout.item_game_fragment_more_game)
            .addBindView { itemView, itemData, position ->
                itemView.tv_item_gameFragment_name.text = itemData.game_name

                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (VersionBean.getData()?.isShowStartGameBtn == 1) {
                            if (MyApp.getInstance().isHaveToken()) {
                                toGetAllAccount(
                                    itemData.game_id,
                                    itemData.game_name,
                                    itemData.game_channelId
                                )
                            } else {
                                LoginUtils.toQuickLogin(requireActivity())
                            }
                        }
                    }
            }
            .create(true)

        mView?.rv_gameFragment_game?.let {
            it.setHasFixedSize(true)
            it.setItemViewCacheSize(20)
            it.isDrawingCacheEnabled = true
            it.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
            (it.itemAnimator as SimpleItemAnimator).supportsChangeAnimations =
                false //取消默认动画
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

                                    override fun accountStart(
                                        account: String,
                                        password: String
                                    ) {
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
    private fun toStartGame(
        gameId: Int,
        gameChannel: String,
        account: String?,
        password: String?
    ) {
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
                                startActivity(
                                    Intent(
                                        requireContext(),
                                        ProcessActivity::class.java
                                    )
                                )

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
     * 获取游戏列表
     */
    private fun getGameList(needLoading: Boolean) {
        if (needLoading) {
            DialogUtils.showBeautifulDialog(requireContext())
        }
        val gameList = RetrofitUtils.builder().gameListNew(currentPage)
        gameListObservable = gameList.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
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
                                    //第一次跳转过来并加载了数据之后,需要检查剪切板数据
                                    Observable.timer(1, TimeUnit.SECONDS)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            toCheckIsNeedOpenGame()
                                        }
                                }
                                if (it.data.list.isNotEmpty()) {
                                    val count = it.data.paging.count
                                    val limit = it.data.paging.limit
                                    countPage = count / limit + if (count % limit == 0) 0 else 1
                                    val oldSize = mData.size
                                    mData.addAll(it.data.list)
                                    if (currentPage == 1) {
                                        mAdapter?.notifyDataSetChanged()
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
                    mView?.refresh_gameFragment_game?.finishRefresh()
                    mView?.refresh_gameFragment_game?.finishLoadMore()
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
                }
            )
    }

    /**
     * 检查是否需要开启游戏
     */
    private fun toCheckIsNeedOpenGame() {
        if (gameNameFromClipboardContent == "") {
            return
        }
        val data = GameInfo4ClipboardBean.getData() ?: return
        LogUtils.d("剪切板拿到的数据:$data")
        GameInfo4ClipboardBean.setData(null)
//        ClipboardUtils.clearClipboardContent(requireActivity())
        val list = arrayListOf<AllAccountBean.Data>()
        list.add(AllAccountBean.Data(data.account, data.password))
        DialogUtils.showStartGameDialog(
            requireActivity(),
            gameNameFromClipboardContent,
            list,
            object : DialogUtils.OnDialogListener4StartGame {
                override fun tejieStart() {
                    toStartGame(
                        gameIdFromClipboardContent,
                        gameChannelIdFromClipboardContent,
                        null,
                        null
                    )
                }

                override fun accountStart(account: String, password: String) {
                    toSaveAccount(
                        gameIdFromClipboardContent,
                        gameChannelIdFromClipboardContent,
                        account,
                        password
                    )
                }
            }, 1
        )
    }

    override fun onDestroy() {
        gameListObservable?.dispose()
        gameListObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null

        allAccountObservable?.dispose()
        allAccountObservable = null

        saveAccountObservable?.dispose()
        saveAccountObservable = null
        super.onDestroy()
    }
}