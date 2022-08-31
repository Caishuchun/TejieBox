package com.fortune.tejiebox.bean

data class GetIntegralBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val integral: Int
    )
}