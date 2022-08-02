package com.fortune.tejiebox.http

/**
 * Author: 蔡小树
 * Time: 2020/5/13 上午 10:18
 * Description:
 */

object HttpUrls {

    const val TEST_URL = "http://47.94.252.136:10001/"       //测试区
    const val REAL_URL = "https://tjbox.lelehuyu.com/"       //正式区

    //一键登录
    const val QUICK_LOGIN_ALI = "/android/login/aliyunOneClick"

    //登录发送短信验证码
    const val SEND_CODE = "/android/login/sendCode"

    //短信验证码登录
    const val LOGIN = "/android/login/login"

    //轮播图
    const val BANNER_LIST = "/android/v2.game/slideshow"

    //获取游戏列表数据
    const val GAME_LIST = "/android/v2.game/list"

    //修改手机号的短信验证码
    const val SEND_CODE_4_CHANGE_PHONE = "/android/user/sendCode"

    //修改手机号
    const val CHANGE_PHONE = "/android/user/updatePhone"

    //版本检查
    const val CHECK_VERSION = "/android/v2.update/check"

    //获取热门搜索
    const val HOT_SEARCH = "/android/v2.game/hotSearch"

    //搜索建议
    const val SEARCH_SUGREC = "/android/v2.game/sugrec"

    //游戏搜索
    const val SEARCH = "/android/v2.game/search"

    //游戏搜索
    const val ADD_TO_HOT_SEARCH = "/android/v2.game/recordSearch"

    //游戏信息
    const val GAME_INFO = "/android/v2.game/read"

    //收藏/取消收藏游戏
    const val COLLECT_GAME = "/android/game/fav"

    //游戏收藏列表
    const val COLLECT_LIST = "/android/v2.game/myFav"

    //在玩列表
    const val PLAYING_GAME = "/android/v2.game/playList"

    //添加到在玩
    const val TO_ADD_PLAYING = "/android/v2.game/play"

    //上传游戏在线时长
    const val UPDATE_GAME_TIME_INFO = "android/v2.game/duration"

}