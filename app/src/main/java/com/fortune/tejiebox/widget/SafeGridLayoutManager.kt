package com.fortune.tejiebox.widget

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

/**
 * 安全第一的StaggeredGridLayoutManager 流式布局的layoutManager
 */
open class SafeGridLayoutManager(context: Context, orientation: Int) :
    GridLayoutManager(context, orientation) {

//    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
//        try {
//            super.onLayoutChildren(recycler, state)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
}