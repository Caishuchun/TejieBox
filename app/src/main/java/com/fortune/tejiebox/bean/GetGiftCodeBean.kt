package com.fortune.tejiebox.bean

data class GetGiftCodeBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val code: String,
        val ttl: Long,
        val user_duration: Int?,
        val limit_duration: Int?
    )
}