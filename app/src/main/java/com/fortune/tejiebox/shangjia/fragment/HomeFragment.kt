package com.fortune.tejiebox.shangjia.fragment

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.item_home_big_title.view.*
import kotlinx.android.synthetic.main.item_home_small_title.view.*
import kotlinx.android.synthetic.main.item_pager.view.*
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var mView: View? = null
    private var mData4BigTitle = mutableListOf<String>()
    private var mData4SmallTitle = mutableListOf<String>()
    private var mData4Game4Game = mutableListOf<String>()
    private var mPosition4BigTitle = 0
    private var mPosition4SmallTitle = 0
    private var mAdapter4BigTitle: BaseAdapterWithPosition<String>? = null
    private var mAdapter4SmallTitle: BaseAdapterWithPosition<String>? = null
    private var mAdapter4Game: BaseAdapterWithPosition<String>? = null
    private var mAdapter4Pager: BaseAdapterWithPosition<String>? = null

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_home, container, false)
        initView()

        changeViewPagerSensitivity()
        return mView
    }

    /**
     * 动态设置ViewPager2 灵敏度
     */
    private fun changeViewPagerSensitivity() {
        try {
            val recyclerViewField: Field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(mView?.pager_home_game) as RecyclerView
            val touchSlopField: Field = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * 3) //6 is empirical value
        } catch (ignore: Exception) {
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("CheckResult")
    private fun initView() {
        LogUtils.d("++++++++++1")
        mData4BigTitle.add("类型")
        mData4BigTitle.add("厂商")
        mData4BigTitle.add("其他")
        //大标题
        mAdapter4BigTitle = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.item_home_big_title)
            .setData(mData4BigTitle)
            .addBindView { itemView, itemData, position ->
                itemView.tv_home_big_title.text = itemData
                if (mPosition4BigTitle == position) {
                    itemView.tv_home_big_title.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    itemView.line_home_big_title.visibility = View.VISIBLE
                } else {
                    itemView.tv_home_big_title.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
                    itemView.line_home_big_title.visibility = View.INVISIBLE
                }
                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val oldPosition = mPosition4BigTitle
                        mPosition4BigTitle = position
                        mAdapter4BigTitle?.notifyItemChanged(oldPosition)
                        mAdapter4BigTitle?.notifyItemChanged(mPosition4BigTitle)
                        val max = (5..10).random()
                        mData4SmallTitle.clear()
                        for (index in 0..max) {
                            mData4SmallTitle.add("${mData4BigTitle[mPosition4BigTitle]}_$index")
                        }
                        mPosition4SmallTitle = 0
                        mView?.rv_home_small_title?.scrollToPosition(0)
                        mAdapter4SmallTitle?.notifyDataSetChanged()

                        mView?.pager_home_game?.currentItem = 0
                        mAdapter4Pager?.notifyDataSetChanged()
                    }
            }
            .create()
        mView?.rv_home_big_title?.let {
            it.adapter = mAdapter4BigTitle
            it.layoutManager = SafeLinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL)
        }

        val max = (5..10).random()
        for (index in 0..max) {
            mData4SmallTitle.add("${mData4BigTitle[mPosition4BigTitle]}_$index")
        }

        //小标题
        mAdapter4SmallTitle = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.item_home_small_title)
            .setData(mData4SmallTitle)
            .addBindView { itemView, itemData, position ->
                itemView.tv_home_small_title.text = itemData
                if (mPosition4SmallTitle == position) {
                    itemView.tv_home_small_title.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                } else {
                    itemView.tv_home_small_title.typeface =
                        Typeface.defaultFromStyle(Typeface.NORMAL)
                }
                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val oldPosition = mPosition4SmallTitle
                        mPosition4SmallTitle = position
                        mAdapter4SmallTitle?.notifyItemChanged(oldPosition)
                        mAdapter4SmallTitle?.notifyItemChanged(mPosition4SmallTitle)

                        mView?.pager_home_game?.currentItem = mPosition4SmallTitle
                    }
            }
            .create()
        mView?.rv_home_small_title?.let {
            it.adapter = mAdapter4SmallTitle
            it.layoutManager = SafeLinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL)
        }

        //ViewPager的Adapter
        mAdapter4Pager = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.item_pager)
            .setData(mData4SmallTitle)
            .addBindView { itemView, itemData, position ->
                itemView.tv_pager_title.text = itemData
            }
            .create()
        mView?.pager_home_game?.let {
            it.adapter = mAdapter4Pager
            it.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val oldPosition = mPosition4SmallTitle
                    mPosition4SmallTitle = position
                    mAdapter4SmallTitle?.notifyItemChanged(oldPosition)
                    mAdapter4SmallTitle?.notifyItemChanged(mPosition4SmallTitle)
                    mView?.rv_home_small_title?.scrollToPosition(mPosition4SmallTitle)
                }
            })
        }
    }
}