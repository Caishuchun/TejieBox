package com.fortune.tejiebox.bean

data class PhoneChargeInfoBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val clockin_days: Int,
        val clockin_today: Int,
        val expire_time: String,
        val phone: String,
        val status: Int
    )
}