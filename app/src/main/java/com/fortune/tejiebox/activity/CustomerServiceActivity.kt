package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
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
import com.fortune.tejiebox.utils.*
import com.fortune.tejiebox.widget.SafeLinearLayoutManager
import com.google.gson.Gson
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
                toSendMsg(et_customerService_msg.text.toString().trim())
            }

        //获取最近聊天数据
        val dataBase = CustomerServiceInfoDataBase.getDataBase(this.applicationContext)
        mCustomerServiceDao = dataBase.customerServiceInfoDao()
//        mCustomerServiceDao.clear()

        val info = mCustomerServiceDao.getInfo()
        if (info.isEmpty()) {
            //没有数据,添加开始提示
            val customerServiceInfo = CustomerServiceInfo(
                -1, 0, 1,
                "您好, 请问有什么可以帮助您! 请反馈遇到的问题, 我们将在一个工作日内解答。",
                null, null, null,
                System.currentTimeMillis(),
                1
            )
            mCustomerServiceDao.addInfo(customerServiceInfo)
            mData.add(customerServiceInfo)
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
            EventBus.getDefault().postSticky(ShowNumChange(0))
        }
        LogUtils.d(Gson().toJson(mData))

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

                            itemView.tv_item_customerService_left_msg.text = itemData.chat_content
                            itemView.tv_item_customerService_left_msg_time.text =
                                formatChatTime(itemData.chat_time)
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

                            RxView.clicks(itemView.rootView)
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

                            RxView.clicks(itemView.rootView)
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
     * 格式化时间
     * 今日时间显示    xx:xx
     * 昨日时间显示    昨天 xx:xx
     * 再之前时间显示  xx月xx日 xx:xx
     * 远超一年的显示  xxxx年xx月xx日 xx:xx
     */
    @SuppressLint("SimpleDateFormat")
    private fun formatChatTime(chatTime: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
        // 聊天时间
        val chatTimeStr = simpleDateFormat.format(chatTime)
        // 当前时间
        val currentTime = System.currentTimeMillis()
        val currentTimeStr = simpleDateFormat.format(currentTime)
        // 今日0点时间
        val todayZeroTimeStr = "${currentTimeStr.split(" ")[0]} 00:00:00"
        val todayZeroTime = simpleDateFormat.parse(todayZeroTimeStr).time
        // 昨日零点时间
        val yesterdayZeroTime = todayZeroTime - 1000 * 60 * 60 * 24L
        // 时隔一年的时间
        val oneYearTime = currentTime - 1000 * 60 * 60 * 24 * 365L

        val result = when {
            chatTime > todayZeroTime -> {
                // 今日发送的消息
                val sb = SimpleDateFormat("HH:mm")
                sb.format(chatTime)
            }
            chatTime in (yesterdayZeroTime + 1)..todayZeroTime -> {
                // 昨天发送的消息
                val sb = SimpleDateFormat("HH:mm")
                "昨天 ${sb.format(chatTime)}"
            }
            chatTime in (oneYearTime + 1)..yesterdayZeroTime -> {
                // 其他时间
                val sb = SimpleDateFormat("MM月dd日 HH:mm")
                sb.format(chatTime)
            }
            else -> {
                chatTimeStr
            }
        }
        return result.replace(" ", "\n")
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
        image_height: Int? = null
    ) {
        val sendMsg = RetrofitUtils.builder().sendMsg(msg, type, image_width, image_height)
        sendMsgObservable = sendMsg.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it.code) {
                    1 -> {
                        if (type == 2) {
                            customerServiceInfo4Pic?.chat_id = it.data.chat_id
                            mCustomerServiceDao.addInfo(customerServiceInfo4Pic!!)
                            rv_customerService_info?.scrollToPosition(mData.size - 1)
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
            }, {
                ToastUtils.show(it.message)
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
                                result[picIndex].height
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
                    ToastUtils.show(it.message)
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