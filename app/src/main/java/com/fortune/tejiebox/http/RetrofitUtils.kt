package com.fortune.tejiebox.http

import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.bean.*
import com.fortune.tejiebox.constants.SPArgument
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
 * Author: 蔡小树
 * Time: 2019/12/26 14:44
 * Description:
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
                        "HFDD-APP/Android/${
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
        @FormUrlEncoded
        @POST(HttpUrls.GAME_LIST)
        fun gameList(
            @Field("page", encoded = true) page: Int
        ): Flowable<GameListBean>

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
        @FormUrlEncoded
        @POST(HttpUrls.SEARCH_SUGREC)
        fun searchSugrec(
            @Field("wd", encoded = true) wd: String
        ): Flowable<HotSearchBean>

        /**
         * 搜索游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEARCH)
        fun search(
            @Field("wd", encoded = true) wd: String,
            @Field("page", encoded = true) page: Int
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
        fun likeGame(): Flowable<LikeAndPlayingBean>

        /**
         * 在玩游戏列表
         */
        @GET(HttpUrls.PLAYING_GAME)
        fun playingGame(): Flowable<LikeAndPlayingBean>

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
    }
}