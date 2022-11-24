package com.fortune.tejiebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter基类(带有position)--专门为客服聊天界面准备
 * 实现可局部更新数据
 */

class BaseAdapterWithPosition4CustomerService<T> private constructor() :
    RecyclerView.Adapter<BaseAdapterWithPosition4CustomerService<T>.BaseViewHolder>() {
    private var mDataList: List<T>? = null
    private var mLayoutId: Int? = null
    private var addBindView: ((itemView: View, itemData: T, position: Int, payloads: MutableList<Any>) -> Unit)? =
        null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(mLayoutId!!, parent, false)
        return BaseViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mDataList!!.size
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.setIsRecyclable(false)
        addBindView!!.invoke(holder.itemView, mDataList!![position], position,payloads)
    }

    inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class Builder<B> {
        private var baseAdapter: BaseAdapterWithPosition4CustomerService<B> =
            BaseAdapterWithPosition4CustomerService()

        fun setData(lists: List<B>): Builder<B> {
            baseAdapter.mDataList = lists
            return this
        }

        fun setLayoutId(layoutId: Int): Builder<B> {
            baseAdapter.mLayoutId = layoutId
            return this
        }

        fun addBindView(itemBind: ((itemView: View, itemData: B, position: Int, payloads: MutableList<Any>) -> Unit)): Builder<B> {
            baseAdapter.addBindView = itemBind
            return this
        }

        fun create(): BaseAdapterWithPosition4CustomerService<B> {
            return baseAdapter
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
    }
}