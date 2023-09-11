package com.fortune.tejiebox.constants

/**
 * SP的常量
 */

object SPArgument {
    const val LOGIN_TOKEN = "login_token" //登录token
    const val PHONE_NUMBER = "phone_number" //登录手机号
    const val CODE_TIME = "code_time" //获得短信验证码的时间戳
    const val CODE_TIME_4_CHANGE_PHONE = "code_time_4_change_phone" //获得短信验证码的时间戳
    const val IS_LOGIN = "is_login" //是否在登录状态
    const val IS_NEED_UPDATE_DIALOG = "is_need_update_dialog" //是否需要显示更新Dialog
    const val NOTICE_ID = "notice_id" //公告Id
    const val APP_DOWNLOAD_PATH = "app_download_path" //app下载后的地址
    const val ONLY_DEVICE_ID_NEW = "only_device_id"//设备唯一标识符
    const val IS_NEED_SHADE_NEW = "is_need_shade_new" //是否需要遮罩层,第二版
    const val GAME_TIME_INFO = "game_time_info" //游戏信息 game_id-startTime-endTime

    const val IS_HAVE_ID = "is_have_id" //是否认证了,拥有id
    const val ID_NAME = "id_name" //身份证名称
    const val ID_NUM = "id_num" //身份证号
    const val USER_ID = "user_id" //userId
    const val INTEGRAL = "integral" //积分

    const val IS_LOGIN_ED = "isLogined" //是否已经登录了
    const val UM_CHANNEL_ID = "umChannelID" //友盟ChannelID

    const val IS_CHECK_AGREEMENT = "isCheckAgreement" //是否已经同意协议
    const val IS_CHECK_AGREEMENT_SPLASH = "isCheckAgreement4Splash" //是否已经同意协议_专属启动页
    const val LOGIN_ACCOUNT = "login_account" //登录账号
    const val LOGIN_ACCOUNT_PASS = "login_account_pass" //登录账号密码

    const val IS_SHOW_SHADE = "is_show_shade" //是否显示遮罩层

    const val NEED_JUMP_GAME_ID_JUMP = "needJumpGameId4Jump" //需要跳转的游戏Id_跳转用
    const val NEED_JUMP_GAME_ID_UPDATE = "needJumpGameId4Update" //需要跳转的游戏Id_传值用

    const val INVITE_CODE_USED = "inviteCodeUsed" //邀请码是否使用过了

    const val OPEN_INSTALL_USED = "openInstallUsed" //openInstall是佛是否使用过了
    const val OPEN_INSTALL_INFO = "openInstallInfo" //openInstall传递的值
    const val OPEN_INSTALL_INFO_INVITE = "openInstallInfoInvite" //openInstall传递的值_分享来的

    const val GET_GAME_LIST_TIME = "getGameListTime" //获取游戏列表的时间

    const val VERIFICATION_CODE_TIME = "verification_code_time" //获取验证码的时间
    const val USER_ID_NEW = "user_id_new" //明文userid

    const val IS_NEED_SHOW_GUIDE = "isNeedShowGuide" //是否需要显示引导页
    const val IS_NEED_SHOW_INSTALL_GIFT = "isNeedShowInstallGift" //是否需要显示安装礼包

    const val TODAY_DATE = "today_date" //今日日期
}