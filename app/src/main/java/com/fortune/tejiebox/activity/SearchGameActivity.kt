package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.bean.GameListBean
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.PlayingDataChange
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.room.*
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.fortune.tejiebox.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import com.unity3d.player.JumpUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search_game.*
import kotlinx.android.synthetic.main.item_main_frament_game.view.*
import kotlinx.android.synthetic.main.layout_item_hot_search.view.*
import kotlinx.android.synthetic.main.layout_item_search_his.view.*
import kotlinx.android.synthetic.main.layout_item_search_sugrec.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class SearchGameActivity : BaseActivity() {
    private var getHotSearchHisObservable: Disposable? = null
    private var getSearchSugrecObservable: Disposable? = null
    private var searchObservable: Disposable? = null
    private var addToHotSearchObservable: Disposable? = null
    private var hotSearchList = arrayListOf<String>()
    private var searchSugrecList = arrayListOf<String>()
    private var searchHisList = mutableListOf<SearchHis>()
    private var searchList = mutableListOf<GameListBean.Data.Game>()
    private lateinit var hotSearchAdapter: BaseAdapterWithPosition<String>
    private lateinit var searchHisAdapter: BaseAdapterWithPosition<SearchHis>
    private lateinit var searchSugrecAdapter: BaseAdapterWithPosition<String>
    private lateinit var searchAdapter: BaseAdapterWithPosition<GameListBean.Data.Game>

    private var addPlayingGameObservable: Disposable? = null
    private var updateGameTimeInfoObservable: Disposable? = null
    private var isPlayingGame = false

    private var getGiftCodeObservable: Disposable? = null

    private var isNeedSugrec = true //是不是需要建议和历史记录的请求

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: SearchGameActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        lateinit var searchHisDao: SearchHisDao
    }

    override fun getLayoutId() = R.layout.activity_search_game

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)

        et_search_str.isFocusable = true
        et_search_str.isFocusableInTouchMode = true
        et_search_str.requestFocus()

        initSearchHis()
        initView()
        getHotSearch()
    }

    @SuppressLint("CheckResult")
    private fun initSearchHis() {
        val dataBase = SearchHisDataBase.getDataBase(this.applicationContext)
        searchHisDao = dataBase.searchHisDao()

        val stepAll = searchHisDao.all
        val all = mutableListOf<SearchHis>()
        if (stepAll.isNotEmpty()) {
            for (index in stepAll.indices) {
                all.add(stepAll[stepAll.size - 1 - index])
            }
        }
        if (all.isEmpty()) {
            //没有缓存的搜索记录
            ll_search_his_only2.visibility = View.GONE
            view_search_line.visibility = View.GONE
        } else {
            when (all.size) {
                1 -> {
                    //仅有一条搜索记录
                    //1.仅显示第一条搜索记录
                    //2.其他都不显示
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.GONE
                    ll_search_his_3.visibility = View.GONE
                    ll_search_his_4.visibility = View.GONE
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteAll()
                            ll_search_his_1.visibility = View.GONE
                            view_search_line.visibility = View.GONE
                        }
                }
                2 -> {
                    //仅有两条搜索记录
                    //1.显示两条搜索记录
                    //2.不显示加载更多
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.GONE
                    ll_search_his_4.visibility = View.GONE
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            ll_search_his_1.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            ll_search_his_2.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                }
                3 -> {
                    //仅有三条条搜索记录
                    //1.显示三条条搜索记录
                    //2.不显示加载更多
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.VISIBLE
                    tv_search_his_3.text = all[2].str
                    ll_search_his_4.visibility = View.GONE
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            ll_search_his_1.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            ll_search_his_2.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[2].str)
                        }
                    RxView.clicks(iv_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[2])
                            ll_search_his_3.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                }
                4 -> {
                    //仅有四条条搜索记录
                    //1.显示四条条搜索记录
                    //2.不显示加载更多
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.VISIBLE
                    tv_search_his_3.text = all[2].str
                    ll_search_his_4.visibility = View.VISIBLE
                    tv_search_his_4.text = all[3].str
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            ll_search_his_1.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            ll_search_his_2.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[2].str)
                        }
                    RxView.clicks(iv_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[2])
                            ll_search_his_3.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[3].str)
                        }
                    RxView.clicks(iv_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[3])
                            ll_search_his_4.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                }
                else -> {
                    //搜索记录大于四条
                    //1.显示前四条记录
                    //2.显示加载更多
                    //3.点击加载更多显示recycleview来加载更多搜索记录
                    //4.前四条可以单独删除
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.VISIBLE
                    tv_search_his_3.text = all[2].str
                    ll_search_his_4.visibility = View.VISIBLE
                    tv_search_his_4.text = all[3].str
                    tv_search_showAll.visibility = View.VISIBLE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            initSearchHis()
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            initSearchHis()
                        }
                    RxView.clicks(ll_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[2].str)
                        }
                    RxView.clicks(iv_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[2])
                            initSearchHis()
                        }
                    RxView.clicks(ll_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[3].str)
                        }
                    RxView.clicks(iv_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[3])
                            initSearchHis()
                        }
                    RxView.clicks(tv_search_showAll)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            ll_search_his_only2.visibility = View.GONE
                            ll_search_his_all.visibility = View.VISIBLE
                            val newAll = searchHisDao.all
                            searchHisList.clear()
                            searchHisAdapter.notifyDataSetChanged()
                            if (newAll.isNotEmpty()) {
                                for (index in newAll.indices) {
                                    searchHisList.add(newAll[newAll.size - 1 - index])
                                }
                                searchHisAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        //取消即退出
        RxView.clicks(iv_search_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        //进行搜索
        RxView.clicks(tv_search_search)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val searchStr = et_search_str.text.toString().trim()
                if (searchStr.isNotEmpty()) {
                    toSearch(et_search_str.text.toString().trim())
                }
            }

        //热门搜索的adapter
        hotSearchAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.layout_item_hot_search)
            .setData(hotSearchList)
            .addBindView { itemView, itemData, position ->
                when (position) {
                    0 -> {
                        itemView.tv_hot_search_item_num.setTextColor(resources.getColor(R.color.red_F03D3D))
                        itemView.tv_hot_search_item_hot.setTextColor(resources.getColor(R.color.red_F03D3D))
                    }
                    1 -> {
                        itemView.tv_hot_search_item_num.setTextColor(resources.getColor(R.color.orange_FF774E))
                        itemView.tv_hot_search_item_hot.setTextColor(resources.getColor(R.color.orange_FF774E))
                    }
                    2 -> {
                        itemView.tv_hot_search_item_num.setTextColor(resources.getColor(R.color.orange_FFA855))
                        itemView.tv_hot_search_item_hot.setTextColor(resources.getColor(R.color.orange_FFA855))
                    }
                }
                itemView.tv_hot_search_item_hot.text = itemData
                itemView.tv_hot_search_item_num.text = "${position + 1}"
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toSearch(itemData)
                    }
            }
            .create()
        //热门搜索的recycle
        rv_search_hot.layoutManager =
            SafeStaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rv_search_hot.adapter = hotSearchAdapter

        //搜索历史的adapter
        searchHisAdapter = BaseAdapterWithPosition.Builder<SearchHis>()
            .setLayoutId(R.layout.layout_item_search_his)
            .setData(searchHisList)
            .addBindView { itemView, itemData, position ->
                if (searchHisDao.all.isNotEmpty()) {
                    tv_search_clearAll.visibility = View.VISIBLE
                }
                itemView.tv_item_search_his.text = itemData.str
                RxView.clicks(itemView.iv_item_search_his)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        searchHisList.remove(itemData)
                        searchHisDao.deleteHis(itemData)
                        searchHisAdapter.notifyDataSetChanged()
                        if (searchHisDao.all.isEmpty()) {
                            view_search_line.visibility = View.GONE
                            tv_search_clearAll.visibility = View.GONE
                        }
                    }
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toSearch(itemData.str)
                    }
            }
            .create()
        //搜索历史的recycle
        rv_search_all.layoutManager = SafeLinearLayoutManager(this)
        rv_search_all.adapter = searchHisAdapter
        //点击清空搜索记录
        RxView.clicks(tv_search_clearAll)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                searchHisList.clear()
                searchHisAdapter.notifyDataSetChanged()
                searchHisDao.deleteAll()
                ll_search_his_all.visibility = View.GONE
                view_search_line.visibility = View.GONE
            }

        //监听输入框的输入
        var oldStr = ""
        RxTextView.textChanges(et_search_str)
            .skipInitialValue()
            .subscribe {
                if (it.isEmpty()) {
                    iv_search_delete.visibility = View.GONE
                    ll_search_noInput.visibility = View.VISIBLE
                    rv_search_input.visibility = View.GONE
                    rv_search_result.visibility = View.GONE
                } else {
                    iv_search_delete.visibility = View.VISIBLE
                    ll_search_noInput.visibility = View.GONE
                    rv_search_input.visibility = View.VISIBLE
                    if (oldStr.length > it.length) {
                        rv_search_result.visibility = View.GONE
                    }
                    if (it.toString().trim() == "888888") {
                        // 输入的是六个8
                        searchSugrecList.add("免费礼包")
                        searchSugrecAdapter.notifyDataSetChanged()
                    } else {
                        if (oldStr != it.toString() && isNeedSugrec) {
                            toShowSugrec(it.toString())
                        } else {
                            isNeedSugrec = true
                        }
                    }
                }
                oldStr = it.toString()
            }
        //软键盘回车的监听
        et_search_str.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchStr = et_search_str.text.toString().trim()
                if (searchStr.isNotEmpty()) {
                    toSearch(et_search_str.text.toString().trim())
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }


        //清除输入的文字
        RxView.clicks(iv_search_delete)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                tv_search_nothing.visibility = View.GONE
                et_search_str.setText("")
                iv_search_delete.visibility = View.GONE
                if (ll_search_his_only2.visibility == View.VISIBLE) {
                    initSearchHis()
                }
                if (ll_search_his_all.visibility == View.VISIBLE) {
                    val all = searchHisDao.all
                    if (all.isNotEmpty()) {
                        searchHisList.clear()
                        for (index in all.indices) {
                            searchHisList.add(all[all.size - 1 - index])
                        }
                    }
                    searchHisAdapter.notifyDataSetChanged()
                }
            }

        //搜索建议的adapter
        searchSugrecAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.layout_item_search_sugrec)
            .setData(searchSugrecList)
            .addBindView { itemView, itemData, position ->
                var gameName = ""
                var gameID = ""
                if (itemData.contains("|")) {
                    val lastIndexOf = itemData.lastIndexOf("|")
                    gameName = itemData.substring(0, lastIndexOf)
                    gameID = itemData.substring(lastIndexOf + 1, itemData.length)
                    itemView.view_item_search_sugrec.visibility = View.VISIBLE

                    //上色
                    itemView.tv_item_search_sugrec_gameName.text =
                        redText(gameName, et_search_str.text.toString().trim())
                    itemView.tv_item_search_sugrec_gameId.text =
                        redText(
                            gameID,
                            et_search_str.text.toString().trim()
                        )
                } else {
                    itemView.view_item_search_sugrec.visibility = View.GONE
                    itemView.tv_item_search_sugrec_gameId.text = ""
                    gameName = itemData

                    //上色
                    itemView.tv_item_search_sugrec_gameName.text =
                        redText(gameName, et_search_str.text.toString().trim())
                }
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (gameID.contains(et_search_str.text.toString().trim())) {
                            toSearch(gameID.substring(gameID.indexOf(":") + 1, gameID.length))
                        } else {
                            toSearch(gameName)
                        }
                    }
            }.create()
        rv_search_input.adapter = searchSugrecAdapter
        rv_search_input.layoutManager = SafeLinearLayoutManager(this)

        //搜索的adapter
        searchAdapter = BaseAdapterWithPosition.Builder<GameListBean.Data.Game>()
            .setLayoutId(R.layout.item_main_frament_game)
            .setData(searchList)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData.game_cover)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(itemView.iv_item_mainFragment_icon)

                itemView.tv_item_mainFragment_gameName.text = itemData.game_name
                itemView.tv_item_mainFragment_gameDes.text = itemData.game_desc

                val typeView =
                    LayoutInflater.from(this)
                        .inflate(R.layout.layout_item_tag, null)
                typeView.tv_tag.text = itemData.game_type
                typeView.tv_tag.setTextColor(Color.parseColor("#5F60FF"))
                typeView.tv_tag.setBackgroundResource(R.drawable.bg_tag1)
                itemView.flowLayout_item_mainFragment.addView(typeView)
                val size = itemData.game_tag.size
                for (index in 0 until size) {
                    val tagView =
                        LayoutInflater.from(this)
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
                        toAddToHotSearch(itemData.game_name)
                        val intent = Intent(this, GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                        startActivity(intent)
                    }

                RxView.clicks(itemView.tv_item_mainFragment_start)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (MyApp.getInstance().isHaveToken()) {
                            toAddToHotSearch(itemData.game_name)
                            toStartGame(itemData.game_id, itemData.game_channelId)
                        } else {
                            LoginUtils.toQuickLogin(this)
                        }
                    }
            }
            .create()
        rv_search_result.adapter = searchAdapter
        rv_search_result.layoutManager = SafeLinearLayoutManager(this)
    }

    /**
     * 获取游戏礼包
     */
    private fun toGetGiftCode() {
        DialogActivity.showGiftCode(this)
    }

    /**
     * 启动游戏
     */
    private fun toStartGame(gameId: Int, gameChannelid: String) {
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
                                EventBus.getDefault().post(PlayingDataChange(""))
                                isPlayingGame = true
                                SPUtils.putValue(
                                    SPArgument.GAME_TIME_INFO,
                                    "$gameId-${System.currentTimeMillis()}"
                                )
                                JumpUtils.jump2Game(
                                    this,
                                    gameChannelid + Box2GameUtils.getPhoneAndToken()
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
            intent.putExtra(IdCardActivity.GAME_CHANNEL, gameChannelid)
            startActivity(intent)
        }
    }

    /**
     * 添加到热门搜索
     */
    private fun toAddToHotSearch(gameName: String) {
        val addToHotSearch = RetrofitUtils.builder().addToHotSearch(gameName)
        addToHotSearchObservable = addToHotSearch.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, {})
    }

    /**
     * 获取搜索建议
     */
    private fun toShowSugrec(str: String) {
        searchSugrecList.clear()
        searchSugrecAdapter.notifyDataSetChanged()
        val searchSugrec = RetrofitUtils.builder().searchSugrec(str)
        getSearchSugrecObservable = searchSugrec.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null && it.getCode() == 1 && it.getData() != null && it.getData()!!.size >= 0) {
                    for (sugrec in it.getData()!!) {
                        searchSugrecList.add(sugrec)
                    }
                    searchSugrecAdapter.notifyDataSetChanged()
                } else {
                    ll_search_noInput.visibility = View.VISIBLE
                    rv_search_input.visibility = View.GONE
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
//                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 正式搜索
     */
    private fun toSearch(str: String) {
        LogUtils.d("============search===$str")
        searchList.clear()
        searchAdapter.notifyDataSetChanged()
        isNeedSugrec = false
        searchHisDao.addHis(SearchHis((str)))
        val all = searchHisDao.all
        if (all.size > 30) {
            searchHisDao.deleteHis(all[0])
        }
        et_search_str.setText(str)
        et_search_str.setSelection(str.length)
        ll_search_noInput.visibility = View.GONE
        rv_search_input.visibility = View.GONE
        if (str == "888888" || str == "免费礼包") {
            toGetGiftCode()
        } else {
            rv_search_result.visibility = View.VISIBLE
            DialogUtils.showBeautifulDialog(this)
            val search = RetrofitUtils.builder().search(str, 1)
            searchObservable = search.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                    if (it != null) {
                        when (it.code) {
                            1 -> {
                                if (it.data != null) {
                                    tv_search_nothing.visibility = View.GONE
                                    val list = it.data.list
                                    if (!list.isNullOrEmpty()) {
                                        for (info in list) {
                                            searchList.add(info)
                                        }
                                        searchAdapter.notifyDataSetChanged()
                                    } else {
                                        tv_search_nothing.visibility = View.VISIBLE
                                    }
                                }
                            }
                            -1 -> {
                                it.msg.let { it1 -> ToastUtils.show(it1) }
                                ActivityManager.toSplashActivity(this)
                            }
                            else -> {
                                it.msg.let { it1 -> ToastUtils.show(it1) }
                            }
                        }
                    } else {
                        ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                    }
                }, {
                    DialogUtils.dismissLoading()
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                })
        }
    }

    /**
     * 获取热门搜索
     */
    private fun getHotSearch() {
        hotSearchList.clear()
        hotSearchAdapter.notifyDataSetChanged()
        DialogUtils.showBeautifulDialog(this)
        val hotSearch = RetrofitUtils.builder().hotSearch()
        getHotSearchHisObservable = hotSearch.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            ll_search_hot_search.visibility = View.VISIBLE
                            for (hot in it.getData()!!) {
                                hotSearchList.add(hot)
                            }
                            hotSearchAdapter.notifyDataSetChanged()
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
//                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun destroy() {
        getHotSearchHisObservable?.dispose()
        getSearchSugrecObservable?.dispose()
        searchObservable?.dispose()
        addToHotSearchObservable?.dispose()

        getHotSearchHisObservable = null
        getSearchSugrecObservable = null
        searchObservable = null
        addToHotSearchObservable = null

        addPlayingGameObservable?.dispose()
        addPlayingGameObservable = null
        updateGameTimeInfoObservable?.dispose()
        updateGameTimeInfoObservable = null

        getGiftCodeObservable?.dispose()
        getGiftCodeObservable = null
    }

    /**
     * 关键字标红
     */
    @SuppressLint("SetTextI18n")
    private fun redText(str: String, key: String): Spanned? {
        return if (str.contains(key)) {
            val start = str.indexOf(key)
            val end = start + key.length
            Html.fromHtml(
                "${str.substring(0, start)}<font color='#FF0000'>$key</font>${
                    str.substring(
                        end,
                        str.length
                    )
                }"
            )
        } else {
            Html.fromHtml(str)
        }
    }

    override fun onResume() {
        super.onResume()
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
        super.onPause()
        MobclickAgent.onPause(this)
    }
}