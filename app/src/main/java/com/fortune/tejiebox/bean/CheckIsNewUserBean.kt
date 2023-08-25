package com.fortune.tejiebox.bean

data class CheckIsNewUserBean(
    val code: Int,
    val `data`: Data,
    val msg: String
){
    data class Data(
        val is_new: Boolean
    )
}