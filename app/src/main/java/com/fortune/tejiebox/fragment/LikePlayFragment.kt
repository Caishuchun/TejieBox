package com.fortune.tejiebox.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.fortune.tejiebox.R
import com.fortune.tejiebox.activity.GameDetailActivity
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.bean.LikeAndPlayingBean
import com.fortune.tejiebox.event.LikeDataChange
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_like_play.view.*
import kotlinx.android.synthetic.main.item_likeplay_fragment_game.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

private const val TYPE = "type"

class LikePlayFragment() : Fragment() {
    //type 0在玩 1收藏
    var type: Int = 0
    private var mView: View? = null
    private var isShake = false

    private var getPlayingListObservable: Disposable? = null
    private var getLikeListObservable: Disposable? = null
    private var deletePlayingGameObservable: Disposable? = null
    private var deleteLikeGameObservable: Disposable? = null
    private var addPlayingGameObservable: Disposable? = null

    private var mData = mutableListOf<LikeAndPlayingBean.Data>()
    private var mAdapter: BaseAdapterWithPosition<LikeAndPlayingBean.Data>? = null

    companion object {
        @JvmStatic
        fun newInstance(type: Int) =
            LikePlayFragment().apply {
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
        mView = inflater.inflate(R.layout.fragment_like_play, container, false)
        initView()
        getInfo()
        return mView
    }

    /**
     * 获取在玩/收藏列表
     */
    private fun getInfo(isNeedDialog: Boolean = true) {
        if (MyApp.getInstance().isHaveToken()) {
            when (type) {
                0 -> {
                    getPlayingList(isNeedDialog)
                }
                1 -> {
                    getLikeList(isNeedDialog)
                }
            }
        } else {
            mData.clear()
            mAdapter?.notifyDataSetChanged()
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
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
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
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
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
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(requireContext(), it))
            })
    }


    @SuppressLint("CheckResult")
    private fun initView() {
        when (type) {
            0 -> mView?.tv_likePlayFragment_title?.let { it.text = "在玩游戏" }
            1 -> mView?.tv_likePlayFragment_title?.let { it.text = "收藏列表" }
        }

        mView?.tv_likePlayFragment_cancel?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    isShake = false
                    mAdapter?.notifyDataSetChanged()
                }
        }

        mView?.tv_likePlayFragment_cancel?.let { it ->
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isShake) {
                        mView?.tv_likePlayFragment_cancel?.let { it.text = "删除" }
                    } else {
                        mView?.tv_likePlayFragment_cancel?.let { it.text = "取消" }
                    }
                    isShake = !isShake
                    mAdapter?.notifyDataSetChanged()
                }
        }

        mAdapter = BaseAdapterWithPosition.Builder<LikeAndPlayingBean.Data>()
            .setData(mData)
            .setLayoutId(R.layout.item_likeplay_fragment_game)
            .addBindView { itemView, itemData, position ->
                if (isShake) {
                    FlipAnimUtils.startShakeByPropertyAnim(
                        itemView,
                        1f, 1f,
                        2f,
                        1000
                    )
                    itemView.iv_item_likePlayFragment_delete.visibility = View.VISIBLE
                    mView?.tv_likePlayFragment_cancel?.let { it.text = "取消" }
                } else {
                    FlipAnimUtils.stopShakeByPropertyAnim(itemView)
                    itemView.iv_item_likePlayFragment_delete.visibility = View.GONE
                    mView?.tv_likePlayFragment_cancel?.let { it.text = "删除" }
                }

                Glide.with(this)
                    .load(itemData.game_icon)
                    .into(itemView.iv_item_likePlayFragment_icon)
                itemView.tv_item_likePlayFragment_name.text = itemData.game_name

                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(requireActivity(), GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        requireContext().startActivity(intent)
                    }

                RxView.longClicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        isShake = !isShake
                        mAdapter?.notifyDataSetChanged()
                    }
                RxView.clicks(itemView.iv_item_likePlayFragment_delete)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        DialogUtils.showDefaultDialog(requireContext(),
                            "删除游戏",
                            "确定从${if (type == 0) "在玩列表" else "收藏列表"}中删除\"${itemData.game_name}\"吗?",
                            "取消",
                            "确认",
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    mData.removeAt(position)
                                    toDeleteCurrentGame(itemData.game_id)
                                    mAdapter?.notifyDataSetChanged()
                                    if (mData.size == 0) {
                                        isShake = false
                                        mView?.tv_likePlayFragment_cancel?.let {
                                            it.text = "删除"
                                        }
                                    }
                                }
                            })
                    }
            }
            .create()

        mView?.rv_likePlayFragment_game?.let {
            it.adapter = mAdapter
            it.layoutManager = SafeStaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
        }


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun likeDataChange(data: LikeDataChange) {
        getInfo(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun playingDataChange(data: PlayingDataChange) {
        getInfo(false)
    }

    /**
     * 删除当前游戏
     */
    private fun toDeleteCurrentGame(gameId: Int) {
        when (type) {
            0 -> {
                toDeletePlayingGame(gameId)
            }
            1 -> {
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
            .subscribe({}, {})
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        getLikeListObservable?.dispose()
        getLikeListObservable = null

        getPlayingListObservable?.dispose()
        getPlayingListObservable = null

        deleteLikeGameObservable?.dispose()
        deleteLikeGameObservable = null

        deletePlayingGameObservable?.dispose()
        deletePlayingGameObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null
        super.onDestroy()
    }
}