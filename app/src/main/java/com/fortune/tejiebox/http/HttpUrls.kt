package com.fortune.tejiebox.http

/**
 * url请求地址
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

    //添加到热门搜索
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

    //身份证实名认证
    const val ID_CARD = "android/v2.user/idCard"

    //签到列表
    const val DAILY_CHECK_LIST = "/android/activity.dailyClockIn/list"

    //点击签到
    const val DAILY_CHECK = "/android/activity.dailyClockIn/submit"

    //白嫖时段
    const val WHITE_PIAO_LIST = "/android/activity.limitTime/list"

    //白嫖领取
    const val WHITE_PIAO = "/android/activity.limitTime/receive"

    //获取分享链接
    const val GET_SHARE_URL = "/android/activity.invite/getShareUrl"

    //获取分享列表
    const val GET_SHARE_LIST = "/android/activity.invite/list"

    //领取邀请奖励
    const val GET_INVITE_GIFT = "/android/activity.invite/receive"

    //获取实时积分
    const val GET_INTEGRAL = "/android/user/integral"

    //是否可以领取积分界面
    const val CAN_GET_INTEGRAL = "/android/user/redDot"

    //获取游戏角色
    const val GET_GAME_ROLE = "/android/v2.Exchange/getGvRoleList"

    //获取区服充值基数
    const val GET_GAME_RECHARGE = "/android/v2.Exchange/getRechargeList"

    //积分充值
    const val GAME_RECHARGE = "/android/v2.Exchange/boxrecharge"

    //获取游戏礼包
    const val GET_GIFT_CODE = "/android/v2.game/tjGift"

    //获取封面图片
    const val SPLASH_URL = "/android/v2.game/carousel"

    //注册账号检查
    const val CHECK_ACCOUNT = "/android/v2.accountLogin/accountValidate"

    //账号注册
    const val ACCOUNT_SIGN = "/android/v2.accountLogin/register"

    //账号登录
    const val ACCOUNT_LOGIN = "/android/v2.accountLogin/login"

    //绑定账号
    const val BIND_ACCOUNT = "/android/v2.user/bindAccount"

    //绑定手机号
    const val BIND_PHONE = "/android/v2.user/bindPhone"

    //上传图片_客服聊天
    const val UPLOAD_PICTURE = "/android/v2.chats/upload"

    //发送消息
    const val SEND_MSG = "/android/v2.chats/send"

    //获取客服消息
    const val GET_MSG = "/android/v2.chats/user"

    //新年活动_白嫖详情
    const val FREE_GIVE_NY = "/android/activity.newYear/freeGive"

    //新年活动_领取白嫖
    const val RECEIVE_FREE_GIVE_NY = "/android/activity.newYear/receiveFreeGive"

    //新年活动_邀请详情
    const val INVITE_NY = "/android/activity.newYear/invite"

    //新年活动_邀请开箱子
    const val OPEN_BOX_NY = "/android/activity.newYear/inviteOpenBox"

    //新年活动_年兽详情页
    const val NIAN_SHOU_INFO_NY = "/android/activity.newYear/monster"

    //新年活动_打年兽
    const val FIGHT_NIAN_SHOU_NY = "/android/activity.newYear/attack"

    //新年活动_年兽开箱子
    const val NIAN_SHOU_OPEN_BOX_NY = "/android/activity.newYear/monsterOpenBox"

    //新年活动_能量星详情
    const val NIAN_SHOU_STAR_INFO_NY = "/android/activity.newYear/getStar"

    //所有游戏
    const val ALL_GAME = "/android/v2.allGame/list"

    //所有账号
    const val ALL_ACCOUNT = "/android/v2.userGameAccount"

    //账号校验
    const val SAVE_ACCOUNT = "/android/v2.userGameAccount/saveOrUpdate"
}