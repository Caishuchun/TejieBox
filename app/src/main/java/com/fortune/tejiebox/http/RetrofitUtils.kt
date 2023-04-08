package com.fortune.tejiebox.http

import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.*
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.CanGetIntegralBean
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.SPUtils
import io.reactivex.Flowable
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Retrofit工具类
 */

object RetrofitUtils {

    private var retrofit: Retrofit? = null
    private var locale: String? = null
    var baseUrl = if (BaseAppUpdateSetting.appType) HttpUrls.REAL_URL else HttpUrls.TEST_URL
    private var client: OkHttpClient? = null

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        if (LogUtils.isDebug) {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(60 * 5, TimeUnit.SECONDS)
            .readTimeout(60 * 5, TimeUnit.SECONDS)
            .writeTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor {
                val request = it.request()
                val build = request.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader(
                        "User-Agent",
                        "TejieBox-APP/Android/${
                            MyApp.getInstance().getVersion()
                        }${BaseAppUpdateSetting.patch}"
                    )
                    .addHeader("App-Version", MyApp.getInstance().getVersion())
                    .addHeader("cookie", "locale=$locale")
                    .addHeader("Connection", "Upgrade, HTTP2-Settings")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Upgrade", "h2c")
                    .addHeader("Accept-Encoding", "identity")
                    .addHeader("mac", SPUtils.getString(SPArgument.ONLY_DEVICE_ID_NEW) ?: "android_mac")
//                    .addHeader(
//                        "box-type",
//                        if (BaseAppUpdateSetting.isToPromoteVersion) "1" else ""
//                    )
                    .addHeader(
                        "Authorization",
                        "Bearer ${SPUtils.getString(SPArgument.LOGIN_TOKEN)}"
                    )
                    .build()
                return@addInterceptor it.proceed(build)
            }
            .build()
    }

    fun builder(): RetrofitImp {
        locale = "zh"
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client!!)
            .addConverterFactory(BaseGsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        return retrofit!!.create(RetrofitImp::class.java)
    }

    interface RetrofitImp {
        /**
         * 一键登录
         */
        @FormUrlEncoded
        @POST(HttpUrls.QUICK_LOGIN_ALI)
        fun quickLogin4Ali(
            @Field("access_token", encoded = true) access_token: String,
            @Field("device_id", encoded = true) device_id: String? = null,
            @Field("game_channel", encoded = true) game_channel: String? = null,
            @Field("game_id", encoded = true) game_id: Int? = null,
            @Field("game_version", encoded = true) game_version: String? = null,
            @Field("i", encoded = true) i: String? = null
        ): Flowable<LoginBean>

        /**
         * 发送短信验证码
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEND_CODE)
        fun sendCode(
            @Field("phone", encoded = true) phone: String,
            @Field("is_verify", encoded = true) is_verify: Int? = null,
        ): Flowable<BaseBean>

        /**
         * 短信验证码登录
         */
        @FormUrlEncoded
        @POST(HttpUrls.LOGIN)
        fun login(
            @Field("phone", encoded = true) phone: String,
            @Field("captcha", encoded = true) captcha: Int,
            @Field("device_id", encoded = true) device_id: String? = null,
            @Field("game_channel", encoded = true) game_channel: String? = null,
            @Field("game_id", encoded = true) game_id: Int? = null,
            @Field("game_version", encoded = true) game_version: String? = null,
            @Field("i", encoded = true) i: String? = null
        ): Flowable<LoginBean>

        /**
         * 轮播图
         */
        @GET(HttpUrls.BANNER_LIST)
        fun bannerList(): Flowable<BannerListBean>

        /**
         * 获取游戏列表
         */
        @GET(HttpUrls.GAME_LIST)
        fun gameList(
            @Query("page", encoded = true) page: Int
        ): Flowable<GameListInfoBean>

        /**
         * 发送短信验证码_修改手机号
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEND_CODE_4_CHANGE_PHONE)
        fun sendCode4changePhone(
            @Field("phone", encoded = true) phone: String
        ): Flowable<BaseBean>

        /**
         * 修改手机号
         */
        @FormUrlEncoded
        @POST(HttpUrls.CHANGE_PHONE)
        fun changePhone(
            @Field("phone", encoded = true) phone: String,
            @Field("captcha", encoded = true) captcha: Int
        ): Flowable<BaseBean>

        /**
         * 检查版本更新
         */
        @GET(HttpUrls.CHECK_VERSION)
        fun checkVersion(
            @Query("device_id", encoded = true) device_id: String,
            @Query("device", encoded = true) device: String = "android"
        ): Flowable<VersionBean>

        /**
         * 获取热门搜索
         */
        @GET(HttpUrls.HOT_SEARCH)
        fun hotSearch(): Flowable<HotSearchBean>

        /**
         * 搜索建议
         */
        @GET(HttpUrls.SEARCH_SUGREC)
        fun searchSugrec(
            @Query("wd", encoded = true) wd: String
        ): Flowable<HotSearchBean>

        /**
         * 搜索游戏
         */
        @GET(HttpUrls.SEARCH)
        fun search(
            @Query("wd", encoded = true) wd: String,
            @Query("page", encoded = true) page: Int
        ): Flowable<GameListBean>

        /**
         * 热门搜索+1
         */
        @FormUrlEncoded
        @POST(HttpUrls.ADD_TO_HOT_SEARCH)
        fun addToHotSearch(
            @Field("wd", encoded = true) wd: String
        ): Flowable<BaseBean>

        /**
         * 游戏信息
         */
        @GET(HttpUrls.GAME_INFO)
        fun gameInfo(
            @Query("game_id", encoded = true) game_id: Int? = null,
            @Query("game_channel_id", encoded = true) game_channel_id: String? = null
        ): Flowable<GameInfoBean>

        /**
         * 收藏/取消收藏游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.COLLECT_GAME)
        fun addLikeGame(
            @Field("game_id", encoded = true) game_id: Int,
            @Field("is_fav", encoded = true) is_cancel: Int
        ): Flowable<BaseBean>

        /**
         * 获取游戏收藏列表
         */
        @GET(HttpUrls.COLLECT_LIST)
        fun likeGame(): Flowable<GameListInfoBean2>

        /**
         * 在玩游戏列表
         */
        @GET(HttpUrls.PLAYING_GAME)
        fun playingGame(): Flowable<GameListInfoBean2>

        /**
         * 添加在玩标记
         */
        @FormUrlEncoded
        @POST(HttpUrls.TO_ADD_PLAYING)
        fun addPlayingGame(
            @Field("game_id", encoded = true) game_id: Int,
            @Field("is_cancel", encoded = true) is_cancel: Int,
        ): Flowable<BaseBean>

        /**
         * 上传游戏在线时长
         */
        @FormUrlEncoded
        @POST(HttpUrls.UPDATE_GAME_TIME_INFO)
        fun updateGameTimeInfo(
            @Field("game_id", encoded = true) game_id: Int,
            @Field("start_time", encoded = true) start_time: String,
            @Field("end_time", encoded = true) end_time: String,
            @Field("type", encoded = true) type: Int = 1,
        ): Flowable<BaseBean>

        /**
         * 身份证实名认证
         */
        @FormUrlEncoded
        @POST(HttpUrls.ID_CARD)
        fun idCard(
            @Field("name", encoded = true) name: String,
            @Field("idCard", encoded = true) idCard: String,
        ): Flowable<BaseBean>

        /**
         * 获取签到列表
         */
        @GET(HttpUrls.DAILY_CHECK_LIST)
        fun dailyCheckList(): Flowable<DailyCheckListBean>

        /**
         * 签到
         */
        @FormUrlEncoded
        @POST(HttpUrls.DAILY_CHECK)
        fun dailyCheck(
            @Field(
                "nothing",
                encoded = true
            ) nothing: String = ""
        ): Flowable<DailyCheckBean>

        /**
         * 限时白嫖_时段列表
         */
        @GET(HttpUrls.WHITE_PIAO_LIST)
        fun whitePiaoList(): Flowable<WhitePiaoListBean>

        /**
         * 限时白嫖_领取
         */
        @FormUrlEncoded
        @POST(HttpUrls.WHITE_PIAO)
        fun whitePiao(
            @Field("id", encoded = true) id: Int
        ): Flowable<DailyCheckBean>

        /**
         * 获取分享链接
         */
        @GET(HttpUrls.GET_SHARE_URL)
        fun getShareUrl(
            @Query("version", encoded = true) version: Int? = null
        ): Flowable<GetShareUrlBean>

        /**
         * 获取邀请列表
         */
        @GET(HttpUrls.GET_SHARE_LIST)
        fun getShareList(): Flowable<GetShareListBean>

        /**
         * 领取邀请奖励
         */
        @FormUrlEncoded
        @POST(HttpUrls.GET_INVITE_GIFT)
        fun getInviteGift(
            @Field("id", encoded = true) id: Int
        ): Flowable<DailyCheckBean>

        /**
         * 获取当前积分
         */
        @GET(HttpUrls.GET_INTEGRAL)
        fun getIntegral(): Flowable<GetIntegralBean>

        /**
         * 是否可以获取积分
         */
        @GET(HttpUrls.CAN_GET_INTEGRAL)
        fun canGetIntegral(): Flowable<CanGetIntegralBean>

        /**
         * 获取游戏角色
         */
        @GET(HttpUrls.GET_GAME_ROLE)
        fun getGameRole(
            @Query("game_id", encoded = true) game_id: Int
        ): Flowable<RoleListBean>

        /**
         * 获取游戏充值基数
         */
        @GET(HttpUrls.GET_GAME_RECHARGE)
        fun getGameRecharge(
            @Query("serverId", encoded = true) serverId: Int,
            @Query("gameVersion", encoded = true) gameVersion: String
        ): Flowable<RechargeListBean>

        /**
         * 积分兑换
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_RECHARGE)
        fun gameRecharge(
            @Field("playerId", encoded = true) playerId: Int,
            @Field("rechargeId", encoded = true) rechargeId: Int,
            @Field("channel", encoded = true) channel: String,
        ): Flowable<BaseBean>

        /**
         * 获取游戏礼包
         */
        @GET(HttpUrls.GET_GIFT_CODE)
        fun getGiftCode(
            @Query("code", encoded = true) code: String
        ): Flowable<GetGiftCodeBean>

        /**
         * 获取启动封面图
         */
        @GET(HttpUrls.SPLASH_URL)
        fun getSplashUrl(
            @Query("version", encoded = true) version: Int? = null
        ): Flowable<SplashUrlBean>

        /**
         * 账号检查
         */
        @FormUrlEncoded
        @POST(HttpUrls.CHECK_ACCOUNT)
        fun checkAccount(
            @Field("account", encoded = true) account: String
        ): Flowable<BaseBean>

        /**
         * 账号注册
         */
        @FormUrlEncoded
        @POST(HttpUrls.ACCOUNT_SIGN)
        fun accountSign(
            @Field("account", encoded = true) account: String,
            @Field("password", encoded = true) password: String
        ): Flowable<LoginBean>

        /**
         * 账号注册
         */
        @FormUrlEncoded
        @POST(HttpUrls.ACCOUNT_LOGIN)
        fun accountLogin(
            @Field("account", encoded = true) account: String,
            @Field("password", encoded = true) password: String
        ): Flowable<LoginBean>

        /**
         * 上传图片
         */
        @Multipart
        @POST(HttpUrls.UPLOAD_PICTURE)
        fun uploadPicture(
            @Part file: MultipartBody.Part
        ): Flowable<UploadPictureBean>

        /**
         * 发送消息
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEND_MSG)
        fun sendMsg(
            @Field("content", encoded = true) content: String,
            @Field("type", encoded = true) type: Int,
            @Field("image_width", encoded = true) image_width: Int?,
            @Field("image_height", encoded = true) image_height: Int?,
        ): Flowable<SendMsgBean>

        /**
         * 获取客服回复消息
         */
        @GET(HttpUrls.GET_MSG)
        fun getMsg(): Flowable<ReCustomerBean>

        /**
         * 绑定账号
         */
        @FormUrlEncoded
        @POST(HttpUrls.BIND_ACCOUNT)
        fun bingAccount(
            @Field("account", encoded = true) account: String,
            @Field("password", encoded = true) password: String
        ): Flowable<BaseBean>

        /**
         * 绑定手机号
         */
        @FormUrlEncoded
        @POST(HttpUrls.BIND_PHONE)
        fun bingPhone(
            @Field("phone", encoded = true) phone: String,
            @Field("code", encoded = true) code: String,
            @Field("device_id", encoded = true) device_id: String
        ): Flowable<BaseBean>

        /**
         * 新年活动_白嫖详情
         */
        @GET(HttpUrls.FREE_GIVE_NY)
        fun freeGive(): Flowable<NewYear4WhitePiaoBean>

        /**
         * 新年活动_领取白嫖
         */
        @GET(HttpUrls.RECEIVE_FREE_GIVE_NY)
        fun receiveFreeGive(): Flowable<BaseBean>

        /**
         * 新年活动_邀请详情
         */
        @GET(HttpUrls.INVITE_NY)
        fun invite(): Flowable<NewYear4InviteBean>

        /**
         * 新年活动_邀请开箱子
         */
        @FormUrlEncoded
        @POST(HttpUrls.OPEN_BOX_NY)
        fun openBox(
            @Field("treasure_id", encoded = true) treasure_id: Int
        ): Flowable<BaseBean>

        /**
         * 新年活动_年兽详情
         */
        @GET(HttpUrls.NIAN_SHOU_INFO_NY)
        fun nianShouInfo(): Flowable<NewYear4NianShouBean>

        /**
         * 新年活动_打年兽
         */
        @FormUrlEncoded
        @POST(HttpUrls.FIGHT_NIAN_SHOU_NY)
        fun fightNianShou(
            @Field("fire_type", encoded = true) fire_type: Int
        ): Flowable<NewYear4FightNianShouBean>

        /**
         * 新年活动_年兽宝箱
         */
        @FormUrlEncoded
        @POST(HttpUrls.NIAN_SHOU_OPEN_BOX_NY)
        fun openBox4NianShou(
            @Field("treasure_id", encoded = true) treasure_id: Int
        ): Flowable<BaseBean>

        /**
         * 新年活动_能量星详情
         */
        @GET(HttpUrls.NIAN_SHOU_STAR_INFO_NY)
        fun nianShouStarInfo(): Flowable<NewYear4StarBean>

        /**
         * 获取游戏列表_未上架
         */
        @GET(HttpUrls.ALL_GAME)
        fun gameListNew(
            @Query("page", encoded = true) page: Int
        ): Flowable<GameListInfoBean>

        /**
         * 所有账号
         */
        @GET(HttpUrls.ALL_ACCOUNT)
        fun allAccount(): Flowable<AllAccountBean>

        /**
         * 校验账号
         */
        @FormUrlEncoded
        @POST(HttpUrls.SAVE_ACCOUNT)
        fun saveAccount(
            @Field("account", encoded = true) account: String,
            @Field("password", encoded = true) password: String
        ): Flowable<BaseBean>

        /**
         * 根据游戏渠道获取全部游戏的游戏id等信息
         */
        @GET(HttpUrls.GET_GAME_ID)
        fun getGameId(
            @Query("game_channel", encoded = true) game_channel: String
        ): Flowable<GetGameIdBean>

        /**
         * 同步时间
         */
        @FormUrlEncoded
        @POST(HttpUrls.UPDATE_GAME_DURATION)
        fun updateGameDuration(
            @Field("game_id", encoded = true) game_id: Int
        ): Flowable<BaseBean>

        /**
         * 修改密码_旧密码校验
         */
        @FormUrlEncoded
        @POST(HttpUrls.CHANGE_PASS_USE_OLD)
        fun changePassUseOldPass(
            @Field("initial_password", encoded = true) initial_password: String,
            @Field("new_password", encoded = true) new_password: String
        ): Flowable<BaseBean>

        /**
         * 修改密码_发送验证码
         */
        @GET(HttpUrls.CHANGE_PASS_SEND_CODE)
        fun changePassSendCode(): Flowable<BaseBean>

        /**
         * 修改密码_验证码校验
         */
        @FormUrlEncoded
        @POST(HttpUrls.CHANGE_PASS_USE_CODE)
        fun changePassUseCode(
            @Field("password", encoded = true) password: String,
            @Field("captcha", encoded = true) captcha: String
        ): Flowable<BaseBean>
    }
}