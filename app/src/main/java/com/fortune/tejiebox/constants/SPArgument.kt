package com.fortune.tejiebox.constants

/**
 * SP的常量
 */

object SPArgument {
    const val LOGIN_TOKEN = "login_token" //登录token
    const val PHONE_NUMBER = "phone_number" //登录token
    const val CODE_TIME = "code_time" //获得短信验证码的时间戳
    const val CODE_TIME_4_CHANGE_PHONE = "code_time_4_change_phone" //获得短信验证码的时间戳
    const val IS_LOGIN = "is_login" //是否在登录状态
    const val IS_NEED_UPDATE_DIALOG = "is_need_update_dialog" //是否在登录状态
    const val APP_DOWNLOAD_PATH = "app_download_path" //app下载后的地址
    const val ONLY_DEVICE_ID = "only_device_id"//设备唯一标识符
    const val IS_NEED_SHADE_NEW = "is_need_shade_new" //是否需要遮罩层,第二版
    const val GAME_TIME_INFO = "game_time_info" //游戏信息 game_id-startTime-endTime

    const val IS_HAVE_ID = "is_have_id" //是否认证了,拥有id
    const val ID_NAME = "id_name" //身份证名称
    const val ID_NUM = "id_num" //身份证号
    const val USER_ID = "user_id" //userId
    const val INTEGRAL = "integral" //积分

    const val IS_LOGIN_ED = "isLogined" //是否已经登录了
}