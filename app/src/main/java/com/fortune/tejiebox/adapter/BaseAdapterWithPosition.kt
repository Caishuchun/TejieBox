package com.fortune.tejiebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter基类(带有position)
 */

class BaseAdapterWithPosition<T> private constructor() :
    RecyclerView.Adapter<BaseAdapterWithPosition<T>.BaseViewHolder>() {
    private var mDataList: List<T>? = null
    private var mLayoutId: Int? = null
    private var addBindView: ((itemView: View, itemData: T, position: Int) -> Unit)? = null

    private var isHsaStableIds = false

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

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
//        是否允许回收
//        holder.setIsRecyclable(false)
        addBindView!!.invoke(holder.itemView, mDataList!![position], position)
    }

    inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class Builder<B> {
        private var baseAdapter: BaseAdapterWithPosition<B> = BaseAdapterWithPosition()

        fun setData(lists: List<B>): Builder<B> {
            baseAdapter.mDataList = lists
            return this
        }

        fun setLayoutId(layoutId: Int): Builder<B> {
            baseAdapter.mLayoutId = layoutId
            return this
        }

        fun addBindView(itemBind: ((itemView: View, itemData: B, position: Int) -> Unit)): Builder<B> {
            baseAdapter.addBindView = itemBind
            return this
        }

        /**
         * @param isHsaStableIds 直译, 是否拥有稳定的id
         * RecyclerView的item中包含ImageView, 当调用notifyDataSetChanged进行列表刷新时，
         * 为了使url没变的ImageView不重新加载（图片会一闪），可以使用isHsaStableIds=true,
         * 其作用相当于给imageView增加了tag, tag不变的话, 不重复加载, 但这相应会导致数据项重复.
         * 所以, 需要再重写一次getItemId()方法
         */
        fun create(isHsaStableIds: Boolean = false): BaseAdapterWithPosition<B> {
            baseAdapter.isHsaStableIds = isHsaStableIds
            baseAdapter.setHasStableIds(isHsaStableIds)
            return baseAdapter
        }
    }
}