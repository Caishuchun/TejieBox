package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.graphics.drawable.NinePatchDrawable
import android.view.View
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.fortune.tejiebox.GlideEngine
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.utils.PhoneInfoUtils
import com.fortune.tejiebox.utils.SoftKeyBoardListener
import com.fortune.tejiebox.utils.StatusBarUtils
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.style.*
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_customer_service.*
import kotlinx.android.synthetic.main.item_customer_service.view.*
import java.util.concurrent.TimeUnit

class CustomerServiceActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: CustomerServiceActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    private var mAdapter: BaseAdapterWithPosition<String>? = null
    private var mData = mutableListOf<String>()

    private var picIndex = 0 //上传图片时,图片index

    override fun getLayoutId(): Int = R.layout.activity_customer_service

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        initSoftKeyBoardListener()

        picIndex = 0
        mData.clear()
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_customerService_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxTextView.textChanges(et_customerService_msg)
            .skipInitialValue()
            .subscribe {
                if (it.isNullOrEmpty()) {
                    iv_customerService_send.visibility = View.GONE
                    iv_customerService_selectPic.visibility = View.VISIBLE
                } else {
                    iv_customerService_send.visibility = View.VISIBLE
                    iv_customerService_selectPic.visibility = View.GONE
                }
            }

        RxView.clicks(iv_customerService_selectPic)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toSelectPic()
            }

        val str =
            "S12赛季的比赛已经全部结束了，相信绝大多数的玩家都关注了最近一段时间的转会期，lpl赛区的整体人员动态是非常大的。因为今年lpl的世界赛成绩并不是特别的突出，所以各大战队都准备在这一次转会期中捡漏，拿到一些适合自己的选手。随着转会的逐渐进行，业内人士也是透露了非常多的消息，前v5战队的教练和aj就谈到了很多的细节，让玩家们比较意外的是，ig战队的冠军野辅竟然在这一次的转会，其中要复出了，而且价格非常的低，两个人只值200w。"
        for (index in 0..15) {
            mData.add(str.substring(0, (str.indices).random()))
        }

        mAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.item_customer_service)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                when {
                    position % 5 == 1 -> {
                        itemView.ll_item_customerService_left_msg.visibility = View.GONE
                        itemView.ll_item_customerService_left_img.visibility = View.VISIBLE
                        itemView.ll_item_customerService_right_msg.visibility = View.GONE
                        itemView.ll_item_customerService_right_img.visibility = View.GONE

                        val layoutParams =
                            itemView.iv_item_customerService_left_img.layoutParams as LinearLayout.LayoutParams
                        layoutParams.width = formatImgSize(1034, 500)[0]
                        layoutParams.height = formatImgSize(1034, 500)[1]
                        itemView.iv_item_customerService_left_img.layoutParams = layoutParams

                        Glide.with(this)
                            .load("https://t7.baidu.com/it/u=1819248061,230866778&fm=193&f=GIF")
                            .into(itemView.iv_item_customerService_left_img)

                    }
                    position % 5 == 2 -> {
                        itemView.ll_item_customerService_left_msg.visibility = View.VISIBLE
                        itemView.ll_item_customerService_left_img.visibility = View.GONE
                        itemView.ll_item_customerService_right_msg.visibility = View.GONE
                        itemView.ll_item_customerService_right_img.visibility = View.GONE

                        itemView.tv_item_customerService_left_msg.text = itemData
                    }
                    position % 5 == 3 -> {
                        itemView.ll_item_customerService_left_msg.visibility = View.GONE
                        itemView.ll_item_customerService_left_img.visibility = View.GONE
                        itemView.ll_item_customerService_right_msg.visibility = View.GONE
                        itemView.ll_item_customerService_right_img.visibility = View.VISIBLE

                        val layoutParams =
                            itemView.iv_item_customerService_right_img.layoutParams as LinearLayout.LayoutParams
                        layoutParams.width = formatImgSize(500, 900)[0]
                        layoutParams.height = formatImgSize(500, 900)[1]
                        itemView.iv_item_customerService_right_img.layoutParams = layoutParams

                        Glide.with(this)
                            .load("https://t7.baidu.com/it/u=2291349828,4144427007&fm=193&f=GIF")
                            .into(itemView.iv_item_customerService_right_img)

                    }
                    else -> {
                        itemView.ll_item_customerService_left_msg.visibility = View.GONE
                        itemView.ll_item_customerService_left_img.visibility = View.GONE
                        itemView.ll_item_customerService_right_msg.visibility = View.VISIBLE
                        itemView.ll_item_customerService_right_img.visibility = View.GONE

                        itemView.tv_item_customerService_right_msg.text = itemData
                    }
                }

            }.create()

        rv_customerService_info.adapter = mAdapter
        rv_customerService_info.layoutManager = SafeLinearLayoutManager(this)
        rv_customerService_info.scrollToPosition(if (mData.size == 0) 0 else mData.size - 1)
    }

    /**
     * 选择图片
     */
    private fun toSelectPic() {
        val style = PictureSelectorStyle()
        val titleBarStyle = TitleBarStyle()
        val albumWindowStyle = AlbumWindowStyle()
        val bottomNavBarStyle = BottomNavBarStyle()
        val selectMainStyle = SelectMainStyle()

//        selectMainStyle.statusBarColor = Color.parseColor("#FFFFFF")
//        selectMainStyle.isDarkStatusBarBlack = true
//        selectMainStyle.navigationBarColor = Color.parseColor("#FFFFFF")
//        selectMainStyle.isSelectNumberStyle = false
//
//        titleBarStyle.titleBackgroundColor = Color.parseColor("#FFFFFF")
//        titleBarStyle.titleLeftBackResource = R.mipmap.back_black
//        titleBarStyle.titleTextColor = Color.parseColor("#000000")
//        titleBarStyle.titleCancelTextColor = Color.parseColor("#F7F7F7")

//        selectMainStyle.selectNormalText = "发送"
        selectMainStyle.selectText = "发送"

        style.titleBarStyle = titleBarStyle
        style.albumWindowStyle = albumWindowStyle
        style.bottomBarStyle = bottomNavBarStyle
        style.selectMainStyle = selectMainStyle

        PictureSelector.create(this)
            .openGallery(SelectMimeType.ofImage())
            .setMaxSelectNum(9)
            .setSelectorUIStyle(style)
            .setImageEngine(GlideEngine.createGlideEngine())
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: ArrayList<LocalMedia>?) {
                    if (result?.isNotEmpty() == true) {
                        toSendPic(result)
                    }
                }

                override fun onCancel() {

                }
            })
    }

    /**
     * 上传图片
     */
    private fun toSendPic(result: ArrayList<LocalMedia>) {
        if (picIndex < result.size) {
            //可以上传图片
            picIndex++
            toSendPic(result)
        } else {
            //图片上传结束
            picIndex = 0
        }
    }


    /**
     * 初始化软键盘监听
     */
    private fun initSoftKeyBoardListener() {
        SoftKeyBoardListener.setListener(this,
            object : SoftKeyBoardListener.OnSoftKeyBoardChangeListener {
                override fun keyBoardShow(height: Int) {
                    val layoutParams =
                        view_customerService_keybord.layoutParams as LinearLayout.LayoutParams
                    layoutParams.height = height
                    view_customerService_keybord.layoutParams = layoutParams
                    rv_customerService_info.scrollToPosition(if (mData.size == 0) 0 else mData.size - 1)
                }

                override fun keyBoardHide(height: Int) {
                    val layoutParams =
                        view_customerService_keybord.layoutParams as LinearLayout.LayoutParams
                    layoutParams.height = 0
                    view_customerService_keybord.layoutParams = layoutParams
                    rv_customerService_info.scrollToPosition(if (mData.size == 0) 0 else mData.size - 1)
                }
            })
    }

    /**
     * 格式化图片尺寸
     */
    private fun formatImgSize(width: Int, height: Int): IntArray {
        val screenWidth = PhoneInfoUtils.getWidth(this)
        var imgWidth = 0
        var imgHeight = 0
        if (width >= height) {
            //宽图,高度80,宽度自适应
            imgHeight = (80 * (screenWidth.toFloat() / 360)).toInt()
            imgWidth = (width * (imgHeight.toFloat() / height)).toInt()
        } else {
            //长图,宽度80,高度自适应
            imgWidth = (80 * (screenWidth.toFloat() / 360)).toInt()
            imgHeight = (height * (imgWidth.toFloat()) / width).toInt()
        }
        return intArrayOf(imgWidth, imgHeight)
    }

    override fun destroy() {
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}