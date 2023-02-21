package com.fortune.tejiebox.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.tejiebox.R
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_playing_and_collection_father.view.*
import java.util.concurrent.TimeUnit

class PlayingAndCollectionFatherFragment : Fragment() {

    private var mView: View? = null
    private var currentPage = 0
    private var playingFragment: PlayingAndCollectionFragment? = null
    private var collectionFragment: PlayingAndCollectionFragment? = null

    companion object {
        @JvmStatic
        fun newInstance() = PlayingAndCollectionFatherFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_playing_and_collection_father, container, false)
        initView()
        return mView
    }

    private fun initView() {
        changeTitle()
        changeFragment()

        mView?.tv_fragment_playingAndCollection_playing?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (currentPage != 0) {
                        currentPage = 0
                        changeTitle()
                        changeFragment()
                    }
                }
        }
        mView?.tv_fragment_playingAndCollection_collection?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (currentPage != 1) {
                        currentPage = 1
                        changeTitle()
                        changeFragment()
                    }
                }
        }
    }

    /**
     * 修改Fragment
     */
    private fun changeFragment() {
        when (currentPage) {
            0 -> {
                if (playingFragment == null) {
                    playingFragment = PlayingAndCollectionFragment.newInstance(1)
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_fragment_playingAndCollection, playingFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    hideAll()
                    childFragmentManager.beginTransaction()
                        .show(playingFragment!!)
                        .commitAllowingStateLoss()
                }
            }
            1 -> {
                hideAll()
                if (collectionFragment == null) {
                    collectionFragment = PlayingAndCollectionFragment.newInstance(2)
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_fragment_playingAndCollection, collectionFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    childFragmentManager.beginTransaction()
                        .show(collectionFragment!!)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    /**
     * 隐藏所有Fragment
     */
    private fun hideAll() {
        childFragmentManager.beginTransaction()
            .hide(playingFragment!!)
            .hide(collectionFragment ?: playingFragment!!)
            .commitAllowingStateLoss()
    }

    /**
     * 修改抬头
     */
    private fun changeTitle() {
        mView?.tv_fragment_playingAndCollection_playing?.setTextColor(Color.parseColor("#C4C4C4"))
        mView?.tv_fragment_playingAndCollection_collection?.setTextColor(Color.parseColor("#C4C4C4"))
        when (currentPage) {
            0 -> {
                mView?.tv_fragment_playingAndCollection_playing?.setTextColor(Color.parseColor("#6E6FFF"))
            }
            1 -> {
                mView?.tv_fragment_playingAndCollection_collection?.setTextColor(Color.parseColor("#6E6FFF"))
            }
        }
    }
}