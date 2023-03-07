package com.fortune.tejiebox.bean

data class GetGameIdBean(
    val code: Int,
    val mst: String,
    val data: Data?
) {
    data class Data(
        val game_id: Int?,
        val game_name: String?,
        val game_channelId: String?
    )
}