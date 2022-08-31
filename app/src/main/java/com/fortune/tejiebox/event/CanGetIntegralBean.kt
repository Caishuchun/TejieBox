package com.fortune.tejiebox.event

data class CanGetIntegralBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val daily_clock_in: Int,
        val invite: Int,
        val invite_share: Int,
        val limit_time: Int,
        var is_click: Int
    )
}