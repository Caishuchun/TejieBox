package com.fortune.tejiebox.shangjia.fragment

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_community.view.*
import kotlinx.android.synthetic.main.item_home_big_title.view.*
import java.util.concurrent.TimeUnit

class CommunityFragment() : Fragment() {

    private var mView: View? = null
    private var mData4Title = mutableListOf<String>()
    private var mData4Comment = mutableListOf<String>()

    private var mPosition4Title = 0

    private var mAdapter4Title: BaseAdapterWithPosition<String>? = null
    private var mAdapter4Comment: BaseAdapterWithPosition<String>? = null

    companion object {
        @JvmStatic
        fun newInstance() = CommunityFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_community, container, false)
        initView()
        return mView
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        LogUtils.d("++++++++++2")
        mData4Title.add("推荐")
        mData4Title.add("关注")
        mData4Title.add("视频")
        mData4Title.add("图文")

        //标题的Adapter
        mAdapter4Title = BaseAdapterWithPosition.Builder<String>()
            .setData(mData4Title)
            .setLayoutId(R.layout.item_home_big_title)
            .addBindView { itemView, itemData, position ->
                itemView.tv_home_big_title.text = itemData
                if (mPosition4Title == position) {
                    itemView.tv_home_big_title.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    itemView.line_home_big_title.visibility = View.VISIBLE
                } else {
                    itemView.tv_home_big_title.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
                    itemView.line_home_big_title.visibility = View.INVISIBLE
                }
                RxView.clicks(itemView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val oldPosition = mPosition4Title
                        mPosition4Title = position
                        mAdapter4Title?.notifyItemChanged(oldPosition)
                        mAdapter4Title?.notifyItemChanged(mPosition4Title)
                    }
            }
            .create()
        mView?.rv_community_big_title?.let {
            it.adapter = mAdapter4Title
            it.layoutManager = SafeLinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL)
        }

        for (index in 0..20) {
            mData4Comment.add("$index")
        }
        mAdapter4Comment = BaseAdapterWithPosition.Builder<String>()
            .setData(mData4Comment)
            .setLayoutId(R.layout.item_comment_father)
            .addBindView { itemView, itemData, position ->

            }.create()
        mView?.rv_community_comment?.let {
            it.adapter = mAdapter4Comment
            it.layoutManager = SafeLinearLayoutManager(requireContext())
        }
    }
}