package com.fortune.tejiebox.utils

import com.fortune.tejiebox.constants.SPArgument

object Box2GameUtils {

    /**
     * 获取手机号和token
     */
    fun getPhoneAndToken(): String {
        val phoneNum = SPUtils.getString(SPArgument.PHONE_NUMBER)
        val token = SPUtils.getString(SPArgument.USER_ID)
        return "|phone=$phoneNum|token=$token|logintype=1"
    }
}