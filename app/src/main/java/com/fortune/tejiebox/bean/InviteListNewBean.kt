package com.fortune.tejiebox.bean

data class InviteListNewBean(
    val code: Int,
    val data: List<Data>?,
    val msg: String
) {
    data class Data(
        var h1: Int,
        var h2: Int,
        var h3: Int,
        var h4: Int,
        var h5: Int,
        val phone_ending: String,
        val share_id: Int,
        val total_duration: Int,
        val create_time: String,
        val update_time: String
    )
}