package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fortune.tejiebox.GlideEngine
import com.fortune.tejiebox.R
import com.fortune.tejiebox.adapter.BaseAdapterWithPosition4CustomerService
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.event.ShowNumChange
import com.fortune.tejiebox.http.RetrofitProgressUploadListener
import com.fortune.tejiebox.http.RetrofitUploadProgressUtil
import com.fortune.tejiebox.http.RetrofitUtils
import com.fortune.tejiebox.room.CustomerServiceInfo
import com.fortune.tejiebox.room.CustomerServiceInfoDao
import com.fortune.tejiebox.room.CustomerServiceInfoDataBase
import com.fortune.tejiebox.utils.HttpExceptionUtils
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.PhoneInfoUtils
import com.fortune.tejiebox.utils.SoftKeyBoardListener
import com.fortune.tejiebox.utils.StatusBarUtils
import com.fortune.tejiebox.utils.ToastUtils
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.engine.CompressFileEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.style.*
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_customer_service.*
import kotlinx.android.synthetic.main.item_customer_service.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import top.zibin.luban.Luban
import top.zibin.luban.OnNewCompressListener
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class CustomerServiceActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: CustomerServiceActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        private lateinit var mCustomerServiceDao: CustomerServiceInfoDao
    }

    private var mAdapter: BaseAdapterWithPosition4CustomerService<CustomerServiceInfo>? = null
    private var mData = mutableListOf<CustomerServiceInfo>()

    private var picIndex = 0 //上传图片时,图片index
    private var picNum = 0 // 客服未回复消息时, 共有多少条消息时图片

    private var uploadPictureObservable: Disposable? = null
    private var sendMsgObservable: Disposable? = null

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

        RxView.clicks(iv_customerService_send)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val quest = et_customerService_msg.text.toString().trim()
                //先判断是不是常见问题的模糊询问, 如果是的话, 走"是否想问"
                when (isNormalQuest(quest)) {
                    1 -> requestResponseNormalQuest(quest, 4, "1")
                    2 -> requestResponseNormalQuest(quest, 4, "2")
                    4 -> requestResponseNormalQuest(quest, 4, "4")
                    5 -> requestResponseNormalQuest(quest, 4, "5")
                    6 -> requestResponseNormalQuest(quest, 4, "6")
                    else -> toSendMsg(quest)
                }
            }

        //获取最近聊天数据
        val dataBase = CustomerServiceInfoDataBase.getDataBase(this.applicationContext)
        mCustomerServiceDao = dataBase.customerServiceInfoDao()
//        mCustomerServiceDao.clear()

        val info = mCustomerServiceDao.getInfo()
        if (info.isEmpty()) {
            //没有数据,添加开始提示
            initCustomerServiceInfo()
        } else {
            //有数据
            mData.clear()
            for (data in info) {
                if (data.is_read == 0) {
                    data.is_read = 1
                    mCustomerServiceDao.update(data)
                }
                mData.add(data)
            }
            isHavePassMaxPic()
            val lastCustomerServiceInfo = mData[mData.size - 1]
            if (lastCustomerServiceInfo.form == 0 && lastCustomerServiceInfo.chat_type != 3) {
                val chatTime = lastCustomerServiceInfo.chat_time
                val currentTimeMillis = System.currentTimeMillis()
                if (currentTimeMillis - chatTime >= 24 * 60 * 60 * 1000) {
                    //超过24小时, 重新来一次常见问题
                    initCustomerServiceInfo(true)
                }
            }
            EventBus.getDefault().postSticky(ShowNumChange(0))
        }
//        LogUtils.d(Gson().toJson(mData))

        mAdapter = BaseAdapterWithPosition4CustomerService.Builder<CustomerServiceInfo>()
            .setLayoutId(R.layout.item_customer_service)
            .setData(mData)
            .addBindView { itemView, itemData, position, payloads ->
                when (itemData.form) {
                    0 -> {
                        //客服方消息
                        if (itemData.chat_content != null) {
                            //文字信息
                            itemView.ll_item_customerService_left_msg.visibility = View.VISIBLE
                            itemView.ll_item_customerService_left_img.visibility = View.GONE
                            itemView.ll_item_customerService_right_msg.visibility = View.GONE
                            itemView.ll_item_customerService_right_img.visibility = View.GONE
                            itemView.tv_item_customerService_left_msg_time.text =
                                formatChatTime(itemData.chat_time)
                            when (itemData.chat_type) {
                                3 -> {
                                    //常见问题
                                    val ssb = SpannableStringBuilder(
                                        """
                                            常见问题
                                            ￣￣￣￣￣￣￣￣￣￣￣￣￣
                                            如何下载特戒盒子
                                            
                                            游戏内玩法或者游戏bug等问题
                                            
                                            为何不能免费充值
                                            
                                            盒子无法下载或更新
                                            
                                            游戏内白嫖的余额使用介绍
                                            
                                            为什么被封号了
                                            """.trimIndent()
                                    )
                                    //常见问题
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.color = Color.parseColor("#1A1A1A")
                                            ds.isUnderlineText = false
                                            ds.isFakeBoldText = true
                                        }

                                    }, 0, 18, 0)
                                    //如何下载特戒盒子
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            requestResponseNormalQuest(
                                                "如何下载特戒盒子",
                                                0,
                                                "1. 盒子下载地址是 69t.top 在浏览器输入网址下载\n2. 目前只支持手机和平板, 电脑可用模拟器下载盒子"
                                            )
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }

                                    }, 19, 27, 0)
                                    //游戏内玩法或者游戏bug等问题
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            requestResponseNormalQuest(
                                                "游戏内玩法或者游戏bug等问题",
                                                0,
                                                "游戏内的问题, 可以在游戏里找到当前游戏里的客服处理"
                                            )
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }

                                    }, 29, 44, 0)
                                    //为何不能免费充值
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            requestResponseNormalQuest(
                                                "为何不能免费充值",
                                                0,
                                                "1. 一个游戏里包含多个版本无法免费充值\n2. 游戏商家没有打开免费充值功能的话, 该游戏无法免费充值\n" +
                                                        "3. “全部游戏”里面的游戏不发免费充值\n4. 盒子余额不能充值, 只能白嫖"
                                            )
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }

                                    }, 46, 54, 0)
                                    //盒子无法下载或更新
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            requestResponseNormalQuest(
                                                "盒子无法下载或更新",
                                                0,
                                                "2703159366 您加一下这个QQ，让技术排查一下原因"
                                            )
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }
                                    }, 56, 65, 0)
                                    //游戏内白嫖的余额使用介绍
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            requestResponseNormalQuest(
                                                "游戏内白嫖的余额使用介绍",
                                                0,
                                                "1. 1个手机每天只能签到一个账号\n" +
                                                        "2. 当天签到的余额, 30天后会自动到期清除, 比如今天是19号白嫖10块钱, " +
                                                        "等到下个月20号这个10块钱没有使用, 清除的就是今天领的10块钱, 明天领的后天领的是不会清除的"
                                            )
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }

                                    }, 67, 79, 0)
                                    //为什么被封号了
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            requestResponseNormalQuest(
                                                "为什么被封号了",
                                                0,
                                                "游戏内的问题, 可以在游戏里找到当前游戏里的客服处理"
                                            )
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }

                                    }, 81, 88, 0)

                                    itemView.tv_item_customerService_left_msg.let {
                                        it.movementMethod = LinkMovementMethod.getInstance()
                                        it.setText(ssb, TextView.BufferType.SPANNABLE)
                                        it.highlightColor = Color.TRANSPARENT
                                    }
                                }

                                4 -> {
                                    //是否想问
                                    val ssb = SpannableStringBuilder(
                                        """
                                            是否想问?
                                            ￣￣￣￣￣￣￣￣￣￣￣￣￣
                                            
                                            """.trimIndent()
                                    )
                                    //是否想问
                                    ssb.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.color = Color.parseColor("#1A1A1A")
                                            ds.isUnderlineText = false
                                            ds.isFakeBoldText = true
                                        }

                                    }, 0, 19, 0)
                                    when (itemData.chat_content!!.toInt()) {
                                        1 -> {
                                            ssb.append(SpannableStringBuilder("如何下载特戒盒子"))
                                            //如何下载特戒盒子
                                            ssb.setSpan(object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    requestResponseNormalQuest(
                                                        "如何下载特戒盒子",
                                                        0,
                                                        "1. 盒子下载地址是 69t.top 在浏览器输入网址下载\n2. 目前只支持手机和平板, 电脑可用模拟器下载盒子"
                                                    )
                                                }

                                                override fun updateDrawState(ds: TextPaint) {
                                                    super.updateDrawState(ds)
                                                    ds.isUnderlineText = false
                                                }

                                            }, 20, 28, 0)
                                        }

                                        2 -> {
                                            ssb.append(SpannableStringBuilder("游戏内玩法或者游戏bug等问题\n\n为何不能免费充值"))
                                            //如何下载特戒盒子
                                            ssb.setSpan(object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    requestResponseNormalQuest(
                                                        "游戏内玩法或者游戏bug等问题",
                                                        0,
                                                        "游戏内的问题, 可以在游戏里找到当前游戏里的客服处理"
                                                    )
                                                }

                                                override fun updateDrawState(ds: TextPaint) {
                                                    super.updateDrawState(ds)
                                                    ds.isUnderlineText = false
                                                }

                                            }, 20, 35, 0)
                                            ssb.setSpan(object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    requestResponseNormalQuest(
                                                        "为何不能免费充值",
                                                        0,
                                                        "1. 一个游戏里包含多个版本无法免费充值\n2. 游戏商家没有打开免费充值功能的话, 该游戏无法免费充值\n" +
                                                                "3. “全部游戏”里面的游戏不发免费充值\n4. 盒子余额不能充值. 只能白嫖"
                                                    )
                                                }

                                                override fun updateDrawState(ds: TextPaint) {
                                                    super.updateDrawState(ds)
                                                    ds.isUnderlineText = false
                                                }

                                            }, 37, 45, 0)
                                        }

                                        4 -> {
                                            ssb.append(SpannableStringBuilder("盒子无法下载或更新"))
                                            //如何下载特戒盒子
                                            ssb.setSpan(object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    requestResponseNormalQuest(
                                                        "盒子无法下载或更新",
                                                        0,
                                                        "2703159366 您加一下这个QQ，让技术排查一下原因"
                                                    )
                                                }

                                                override fun updateDrawState(ds: TextPaint) {
                                                    super.updateDrawState(ds)
                                                    ds.isUnderlineText = false
                                                }

                                            }, 20, 29, 0)
                                        }

                                        5 -> {
                                            ssb.append(SpannableStringBuilder("游戏内白嫖的余额使用介绍"))
                                            //如何下载特戒盒子
                                            ssb.setSpan(object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    requestResponseNormalQuest(
                                                        "游戏内白嫖的余额使用介绍",
                                                        0,
                                                        "1. 1个手机每天只能签到一个账号\n" +
                                                                "2. 当天签到的余额, 30天后会自动到期清除, 比如今天是19号白嫖10块钱, " +
                                                                "等到下个月20号这个10块钱没有使用, 清除的就是今天领的10块钱, 明天领的后天领的是不会清除的"
                                                    )
                                                }

                                                override fun updateDrawState(ds: TextPaint) {
                                                    super.updateDrawState(ds)
                                                    ds.isUnderlineText = false
                                                }

                                            }, 20, 32, 0)
                                        }

                                        6 -> {
                                            ssb.append(SpannableStringBuilder("为什么被封号了"))
                                            //如何下载特戒盒子
                                            ssb.setSpan(object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    requestResponseNormalQuest(
                                                        "为什么被封号了",
                                                        0,
                                                        "游戏内的问题, 可以在游戏里找到当前游戏里的客服处理"
                                                    )
                                                }

                                                override fun updateDrawState(ds: TextPaint) {
                                                    super.updateDrawState(ds)
                                                    ds.isUnderlineText = false
                                                }

                                            }, 20, 27, 0)
                                        }
                                    }
                                    itemView.tv_item_customerService_left_msg.let {
                                        it.movementMethod = LinkMovementMethod.getInstance()
                                        it.setText(ssb, TextView.BufferType.SPANNABLE)
                                        it.highlightColor = Color.TRANSPARENT
                                    }
                                }

                                else -> {
                                    itemView.tv_item_customerService_left_msg.text =
                                        itemData.chat_content
                                }
                            }
                        } else {
                            //图片消息
                            itemView.ll_item_customerService_left_msg.visibility = View.GONE
                            itemView.ll_item_customerService_left_img.visibility = View.VISIBLE
                            itemView.ll_item_customerService_right_msg.visibility = View.GONE
                            itemView.ll_item_customerService_right_img.visibility = View.GONE

                            val layoutParams =
                                itemView.rl_item_customerService_left_img.layoutParams
                            layoutParams.width =
                                formatImgSize(itemData.imgW!!.toInt(), itemData.imgH!!.toInt())[0]
                            layoutParams.height =
                                formatImgSize(itemData.imgW.toInt(), itemData.imgH.toInt())[1]
                            itemView.rl_item_customerService_left_img.layoutParams = layoutParams

                            val options =
                                RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                            Glide.with(this)
                                .load(itemData.chat_img_url)
                                .apply(options)
                                .into(object : SimpleTarget<Drawable>() {
                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: Transition<in Drawable>?
                                    ) {
                                        itemView.iv_item_customerService_left_img.setImageDrawable(
                                            resource
                                        )
                                    }
                                })

                            itemView.tv_item_customerService_left_img_time.text =
                                formatChatTime(itemData.chat_time)

                            RxView.clicks(itemView)
                                .throttleFirst(200, TimeUnit.MILLISECONDS)
                                .subscribe {
                                    val intent = Intent(this, ShowPicActivity::class.java)
                                    intent.putExtra(ShowPicActivity.POSITION, 0)
                                    intent.putStringArrayListExtra(
                                        ShowPicActivity.LIST,
                                        arrayListOf(itemData.chat_img_url)
                                    )
                                    startActivity(intent)
                                }
                        }
                    }

                    1 -> {
                        //用户方消息
                        if (itemData.chat_content != null) {
                            //文字信息
                            itemView.ll_item_customerService_left_msg.visibility = View.GONE
                            itemView.ll_item_customerService_left_img.visibility = View.GONE
                            itemView.ll_item_customerService_right_msg.visibility = View.VISIBLE
                            itemView.ll_item_customerService_right_img.visibility = View.GONE

                            itemView.tv_item_customerService_right_msg.text = itemData.chat_content
                            itemView.tv_item_customerService_right_msg_time.text =
                                formatChatTime(itemData.chat_time)
                        } else {
                            //图片消息
                            itemView.ll_item_customerService_left_msg.visibility = View.GONE
                            itemView.ll_item_customerService_left_img.visibility = View.GONE
                            itemView.ll_item_customerService_right_msg.visibility = View.GONE
                            itemView.ll_item_customerService_right_img.visibility = View.VISIBLE

                            val layoutParams =
                                itemView.rl_item_customerService_right_img.layoutParams
                            layoutParams.width =
                                formatImgSize(itemData.imgW!!.toInt(), itemData.imgH!!.toInt())[0]
                            layoutParams.height =
                                formatImgSize(itemData.imgW.toInt(), itemData.imgH.toInt())[1]
                            itemView.rl_item_customerService_right_img.layoutParams = layoutParams

                            val options =
                                RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                            Glide.with(this)
                                .load(itemData.chat_img_url)
                                .apply(options)
                                .into(object : SimpleTarget<Drawable>() {
                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: Transition<in Drawable>?
                                    ) {
                                        itemView.iv_item_customerService_right_img.setImageDrawable(
                                            resource
                                        )
                                    }
                                })
                            itemView.tv_item_customerService_right_img_time.text =
                                formatChatTime(itemData.chat_time)

                            RxView.clicks(itemView)
                                .throttleFirst(200, TimeUnit.MILLISECONDS)
                                .subscribe {
                                    val intent = Intent(this, ShowPicActivity::class.java)
                                    intent.putExtra(ShowPicActivity.POSITION, 0)
                                    intent.putStringArrayListExtra(
                                        ShowPicActivity.LIST,
                                        arrayListOf(itemData.chat_img_url)
                                    )
                                    startActivity(intent)
                                }
                        }
                    }
                }
            }.create()

        rv_customerService_info.adapter = mAdapter
        rv_customerService_info.layoutManager = SafeLinearLayoutManager(this)
        rv_customerService_info.scrollToPosition(if (mData.size == 0) 0 else mData.size - 1)
    }

    /**
     * 是否在客服未回消息时, 有超过一定数量的图片
     * 数据直接翻转, 只要遍历到一个客服消息, 直接gg
     */
    private fun isHavePassMaxPic() {
        val reversed = mData.reversed()
        for (info in reversed) {
            if (info.form == 0) {
                return
            }
            if (info.chat_type == 2) {
                picNum++
            }
        }
    }

    /**
     * 初始化在线客服聊天
     * @param isExpire 是否是已过期
     */
    private fun initCustomerServiceInfo(isExpire: Boolean = false) {
        val customerServiceInfo = CustomerServiceInfo(
            -2, 0, 0,
            "您好, 请问有什么可以帮助您! 请反馈遇到的问题, 我们将在一个工作日内解答。",
            null, null, null,
            System.currentTimeMillis(),
            1
        )
        Thread.sleep(200)
        val customerServiceInfoTips = CustomerServiceInfo(
            -1, 0, 3, "",
            null, null, null,
            System.currentTimeMillis(),
            1
        )
        mCustomerServiceDao.addInfo(customerServiceInfo)
        mCustomerServiceDao.addInfo(customerServiceInfoTips)
        mData.add(customerServiceInfo)
        mData.add(customerServiceInfoTips)
    }

    /**
     * 提问和回答常见问题
     * @param quest 提问
     * @param chatType 0默认文字, 是已经点击过常见问题了  4.是否想问
     * */
    private fun requestResponseNormalQuest(quest: String, chatType: Int, chatContent: String) {
        if (chatType == 4) {
            et_customerService_msg.setText("")
        }
        val customerServiceInfo4Request = CustomerServiceInfo(
            System.currentTimeMillis().toInt(), 1, 1,
            quest, null, null, null,
            System.currentTimeMillis(),
            1
        )
        Thread.sleep(200)
        val customerServiceInfo4Response = CustomerServiceInfo(
            System.currentTimeMillis().toInt(), 0,
            chatType, chatContent, null, null, null,
            System.currentTimeMillis(),
            1
        )
        mCustomerServiceDao.addInfo(customerServiceInfo4Request)
        mCustomerServiceDao.addInfo(customerServiceInfo4Response)
        val oldSize = mData.size
        mData.add(customerServiceInfo4Request)
        mData.add(customerServiceInfo4Response)
        mAdapter?.notifyItemChanged(oldSize)
        mAdapter?.notifyItemChanged(oldSize + 1)
        rv_customerService_info.scrollToPosition(oldSize + 1)
    }

    /**
     * 是否是常见问题的模糊询问
     */
    private fun isNormalQuest(quest: String): Int {
        val list4Quest1 = mutableListOf("下载", "更新", "掉签")
        val list4Quest23 =
            mutableListOf(
                "bug",
                "BUG",
                "新区",
                "角色",
                "测试",
                "充值",
                "开区",
                "称号",
                "大陆",
                "版本",
                "充值",
                "免费"
            )
        val list4Quest4 = mutableListOf("怎么更新", "下载不了")
        val list4Quest5 = mutableListOf("白嫖", "签到", "余额")
        val list4Quest6 = mutableListOf("封号", "被封")
        val isContainList1Info = isContainListInfo(quest, list4Quest1)
        if (isContainList1Info) {
            return 1
        }
        val isContainList2Info = isContainListInfo(quest, list4Quest23)
        if (isContainList2Info) {
            return 2
        }
        val isContainList4Info = isContainListInfo(quest, list4Quest4)
        if (isContainList4Info) {
            return 4
        }
        val isContainList5Info = isContainListInfo(quest, list4Quest5)
        if (isContainList5Info) {
            return 5
        }
        val isContainList6Info = isContainListInfo(quest, list4Quest6)
        if (isContainList6Info) {
            return 6
        }
        return 0
    }

    /**
     * 是否含有集合中的数据
     */
    private fun isContainListInfo(quest: String, list: MutableList<String>): Boolean {
        for (info in list) {
            if (quest.contains(info)) {
                return true
            }
        }
        return false
    }

    /**
     * 格式化时间
     * 今日时间显示    xx:xx
     * 昨日时间显示    昨天 xx:xx
     * 再之前时间显示  xx月xx日 xx:xx
     * 远超一年的显示  xxxx年xx月xx日 xx:xx
     */
    @SuppressLint("SimpleDateFormat")
    private fun formatChatTime(chatTime: Long): String {
        return getTime(chatTime)
    }

    /**
     * 获取时间
     * @param time 时间戳
     * @return 时间 今天 昨天 月日 时分
     */
    @SuppressLint("SimpleDateFormat")
    fun getTime(time: Long): String {
        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date(now))
        val yesterday = SimpleDateFormat("yyyy-MM-dd").format(Date(now - 24 * 60 * 60 * 1000))
        val year = SimpleDateFormat("yyyy").format(Date(now))
        val timeStr = SimpleDateFormat("HH:mm").format(Date(time))
        val date4Time = SimpleDateFormat("yyyy-MM-dd").format(Date(time))
        val year4Time = SimpleDateFormat("yyyy").format(Date(time))
        val month4Time = SimpleDateFormat("MM").format(Date(time))
        val day4Time = SimpleDateFormat("dd").format(Date(time))
        val hour4Time = SimpleDateFormat("HH").format(Date(time))
        val minute4Time = SimpleDateFormat("mm").format(Date(time))
        return if (date4Time == today) {
            timeStr
        } else if (date4Time == yesterday) {
            "昨日\n$timeStr"
        } else if (year == year4Time) {
            "${month4Time}月${day4Time}日\n$hour4Time:$minute4Time"
        } else {
            "${year4Time}年${month4Time}月${day4Time}日\n$hour4Time:$minute4Time"
        }
    }

    /**
     * 发送信息
     * @param type 1文本  2图片
     */
    private fun toSendMsg(
        msg: String,
        type: Int = 1,
        customerServiceInfo4Pic: CustomerServiceInfo? = null,
        image_width: Int? = null,
        image_height: Int? = null,
        isUploadPicOver: Boolean? = null
    ) {
        val sendMsg = RetrofitUtils.builder().sendMsg(msg, type, image_width, image_height)
        sendMsgObservable = sendMsg.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (type == 2) {
                                customerServiceInfo4Pic?.chat_id = it.data.chat_id
                                mCustomerServiceDao.addInfo(customerServiceInfo4Pic!!)
                                rv_customerService_info?.scrollToPosition(mData.size - 1)
                                picNum++
                                if (isUploadPicOver == true && picNum >= 3) {
                                    picNum = 0
                                    //图片上传结束, 且图片个数大于等于3张, 则
                                    Thread.sleep(200)
                                    val customerServiceInfo4Response = CustomerServiceInfo(
                                        System.currentTimeMillis().toInt(), 0,
                                        4, "2", null, null, null,
                                        System.currentTimeMillis(),
                                        1
                                    )
                                    mCustomerServiceDao.addInfo(customerServiceInfo4Response)
                                    val oldSize = mData.size
                                    mData.add(customerServiceInfo4Response)
                                    mAdapter?.notifyItemChanged(oldSize)
                                    rv_customerService_info.scrollToPosition(oldSize)
                                }
                            } else {
                                et_customerService_msg.setText("")
                                val customerServiceInfo = CustomerServiceInfo(
                                    it.data.chat_id,
                                    1, 1, msg,
                                    null, null, null,
                                    System.currentTimeMillis(), 1
                                )
                                mData.add(customerServiceInfo)
                                mCustomerServiceDao.addInfo(customerServiceInfo)
                                rv_customerService_info?.scrollToPosition(mData.size - 1)
                            }
                        }

                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
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
            .setCompressEngine(CompressFileEngine { context, source, call ->
                Luban.with(context).load(source).ignoreBy(100)
                    .setCompressListener(object : OnNewCompressListener {
                        override fun onStart() {
                        }

                        override fun onSuccess(source: String?, compressFile: File?) {
                            if (call != null && compressFile != null) {
                                call.onCallback(source, compressFile.absolutePath)
                            }
                        }

                        override fun onError(source: String?, e: Throwable?) {
                            call?.onCallback(source, null)
                        }

                    }).launch()
            })
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
            val customerServiceInfo = CustomerServiceInfo(
                0, 1, 2, null,
                result[picIndex].compressPath,
                result[picIndex].width,
                result[picIndex].height,
                System.currentTimeMillis(), 1
            )
            mData.add(customerServiceInfo)
            updateImgProgress(mData.size - 1, 0)
            //可以上传图片
            val file = File(result[picIndex].compressPath)
            val body = RequestBody.create(
                MediaType.parse("multipart/form-data"), file
            )
            val progressRequestBody = RetrofitUploadProgressUtil.getProgressRequestBody(body,
                object : RetrofitProgressUploadListener {
                    override fun progress(progress: Int) {
                        updateImgProgress(mData.size - 1, progress)
                    }

                    override fun speedAndTimeLeft(speed: String, timeLeft: String) {
                    }
                })
            val createFormData = MultipartBody.Part.createFormData(
                "file",
                URLEncoder.encode(file.name, "UTF-8"),
                progressRequestBody
            )
            val uploadPicture = RetrofitUtils.builder().uploadPicture(createFormData)
            uploadPictureObservable = uploadPicture.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    when (it.code) {
                        1 -> {
                            updateImgProgress(mData.size - 1, 100)
                            customerServiceInfo.chat_img_url = it.data.url
                            toSendMsg(
                                it.data.path,
                                2,
                                customerServiceInfo,
                                result[picIndex].width,
                                result[picIndex].height,
                                picIndex == result.size - 1
                            )
                        }

                        else -> {
                            mAdapter?.notifyItemRemoved(mData.size - 1)
                            ToastUtils.show("发送图片异常,请稍后重试! ")
                        }
                    }
                    picIndex++
                    toSendPic(result)
                }, {
                    mAdapter?.notifyItemRemoved(mData.size - 1)
//                    ToastUtils.show(it.message)
                    LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                })
        } else {
            //图片上传结束
            picIndex = 0
        }
    }

    /**
     * 上传图片的进度
     */
    @SuppressLint("CheckResult", "SetTextI18n")
    private fun updateImgProgress(position: Int, progress: Int) {
        runOnUiThread {
            mAdapter?.notifyItemChanged(position, progress)
            rv_customerService_info?.scrollToPosition(mData.size - 1)
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
            if (imgHeight > height) {
                imgHeight = height
            }
            imgWidth = (width * (imgHeight.toFloat() / height)).toInt()
        } else {
            //长图,宽度80,高度自适应
            imgWidth = (80 * (screenWidth.toFloat() / 360)).toInt()
            if (imgWidth > width) {
                imgWidth = width
            }
            imgHeight = (height * (imgWidth.toFloat()) / width).toInt()
        }
        return intArrayOf(imgWidth, imgHeight)
    }

    override fun destroy() {
        uploadPictureObservable?.dispose()
        uploadPictureObservable = null

        sendMsgObservable?.dispose()
        sendMsgObservable = null
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