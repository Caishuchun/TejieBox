package com.fortune.tejiebox.bean

data class AllAccountBean(
    val code: Int,
    val `data`: List<Data>,
    val msg: String
) {
    data class Data(
        val account: String,
        val password: String
    )
}