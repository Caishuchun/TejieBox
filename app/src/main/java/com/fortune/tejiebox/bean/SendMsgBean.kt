package com.fortune.tejiebox.bean

data class SendMsgBean(
    val code: Int,
    val msg: String,
    val data: Data,
) {
    data class Data(val chat_id: Int)
}