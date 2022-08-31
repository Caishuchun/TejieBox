package com.fortune.tejiebox.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.fortune.tejiebox.R
import kotlinx.android.synthetic.main.layout_item_show_pic.view.*

class ShowPicAdapter(context: Context, picLists: List<String>) : PagerAdapter() {

    private var mContext: Context = context
    private var mLists: List<String> = picLists

    override fun getCount(): Int {
        return mLists.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view =
            LayoutInflater.from(mContext).inflate(R.layout.layout_item_show_pic, container, false)
        Glide.with(mContext)
            .load(mLists[position])
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(view.iv_pic)

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}