package com.fortune.tejiebox.utils

import android.app.Activity
import android.content.Context
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseDialog
import com.fortune.tejiebox.widget.EasyGuideLayer
import com.fortune.tejiebox.widget.GuideItem
import com.fortune.tejiebox.widget.OnDrawHighLightCallback
import com.fortune.tejiebox.widget.OnGuideShowListener
import com.fortune.tejiebox.widget.OnGuideViewAttachedListener
import com.fortune.tejiebox.widget.OnGuideViewOffsetProvider
import com.fortune.tejiebox.widget.OnHighLightClickListener
import kotlinx.android.synthetic.main.dialog_guide_over.ll_guide_over

/**
 * 引导工具类
 */
object GuideUtils {

    /**
     * 显示引导
     * @param activity Activity
     * @param backgroundColor 背景颜色
     * @param highLightView 高亮View
     * @param highLightShape 高亮形状
     * @param guideLayout 引导布局
     * @param guideLayoutGravity 引导布局位置
     * @param guideViewOffsetProvider 引导布局位置调整
     * @param guideViewAttachedListener 引导布局添加成功时的监听
     * @param highLightClickListener 高亮点击回调
     * @param guideShowListener 引导显示监听
     */
    fun showGuide(
        activity: Activity,
        backgroundColor: Int,
        highLightView: View,
        highLightShape: Int,
        guideLayout: Int,
        guideLayoutGravity: Int,
        guideViewOffsetProvider: OnGuideViewOffsetProvider,
        guideViewAttachedListener: OnGuideViewAttachedListener,
        highLightClickListener: OnHighLightClickListener,
        guideShowListener: OnGuideShowListener,
        drawHighLightCallback: OnDrawHighLightCallback? = null
    ) {
        // 先行干掉一波再展示, 方式出现不可控意外
        EasyGuideLayer.with(activity)
            .clearItems()
            .setDismissIfNoItems(true)

        // 创建引导层示例，并为引导层添加指定配置
        val item = GuideItem.newInstance(highLightView, 10)
            .setLayout(guideLayout)// 设置引导View
            .setGravity(guideLayoutGravity)// 设置引导View位置
            .setHighLightShape(highLightShape)// 设置高亮区域绘制模式
            .setOffsetProvider(guideViewOffsetProvider)// 设置用于进行引导View的位置调整
            .setOnViewAttachedListener(guideViewAttachedListener)// 设置引导View被添加到蒙层布局中时的通知监听
            .setOnHighLightClickListener(highLightClickListener)// 设置高亮点击回调监听
            .setOnDrawHighLightCallback(drawHighLightCallback)// 设置高亮区域绘制回调

        // 创建蒙层实例并绑定引导层，进行展示：
        EasyGuideLayer.with(activity)
            .setBackgroundColor(backgroundColor)// 蒙层背景色
            .setOnGuideShownListener(guideShowListener)// 蒙层展示、消失监听
            .addItem(item)// 绑定引导层实例
            .setDismissOnClickOutside(false)// 设置点击到蒙层上的非点击区域时，是否自动让蒙层消失
            .setDismissIfNoItems(true)// 设置当蒙层中一个引导层实例都没绑定时，自动让蒙层消失
            .show()// 展示蒙层
    }


    private var mDialog: BaseDialog? = null

    /**
     * 显示结束引导页结束弹框, 点击之后跳转到白嫖界面
     */
    fun showGuideOverDialog(context: Context, callback: OnGuideOverCallback) {
        if (mDialog != null) {
            mDialog?.dismiss()
            mDialog = null
        }
        mDialog = BaseDialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_guide_over)
        mDialog?.setCancelable(false)
        mDialog?.setCanceledOnTouchOutside(false)

        mDialog?.ll_guide_over?.let {
            it.setOnClickListener {
                callback.over()
                dismissLoading()
            }
        }

        mDialog?.setOnCancelListener {
            dismissLoading()
        }
        mDialog?.show()
    }

    interface OnGuideOverCallback {
        fun over()
    }

    /**
     * 取消弹框
     */
    fun dismissLoading() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mDialog = null
        }
    }
}