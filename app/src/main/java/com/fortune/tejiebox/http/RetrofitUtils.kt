package com.fortune.tejiebox.http

import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.*
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.event.CanGetIntegralBean
import com.fortune.tejiebox.myapp.MyApp
import com.fortune.tejiebox.utils.LogUtils
import com.fortune.tejiebox.utils.SPUtils
import io.reactivex.Flowable
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
                    .addHeader(
                        "App-Version",
                        MyApp.getInstance().getVersion()
                    )
                    .addHeader("cookie", "locale=$locale")
                    .addHeader("Connection", "Upgrade, HTTP2-Settings")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Upgrade", "h2c")
                    .addHeader("Accept-Encoding", "identity")
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
            @Field("access_token", encoded = true) access_token: String
        ): Flowable<LoginBean>

        /**
         * 发送短信验证码
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEND_CODE)
        fun sendCode(
            @Field("phone", encoded = true) phone: String
        ): Flowable<BaseBean>

        /**
         * 短信验证码登录
         */
        @FormUrlEncoded
        @POST(HttpUrls.LOGIN)
        fun login(
            @Field("phone", encoded = true) phone: String,
            @Field("captcha", encoded = true) captcha: Int
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
        fun getShareUrl(): Flowable<GetShareUrlBean>

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
         * 获取游戏充值基数
         */
        @GET(HttpUrls.GET_GIFT_CODE)
        fun getGiftCode(
            @Query("code", encoded = true) code: String
        ): Flowable<GetGiftCodeBean>

        /**
         * 获取游戏充值基数
         */
        @GET(HttpUrls.SPLASH_URL)
        fun getSplashUrl(): Flowable<SplashUrlBean>
    }
}