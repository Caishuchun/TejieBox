package com.fortune.tejiebox.bean

data class GameInfoBean(
    val code: Int,
    val `data`: Data,
    val msg: String?
) {
    data class Data(
        val cdkey: String?,
        val desc: String?,
        val game_channelId: String,
        val game_desc: String,
        val game_icon: String,
        val game_cover: String,
        val game_id: Int,
        val game_intro: String,
        val game_name: String,
        val game_open_times: List<String>,
        val game_pic: List<String>,
        val game_tag: List<String>,
        val game_type: String,
        val game_update_time: Int,
        val is_fav: Int,
        val game_style: String?,
        val is_open_free: Int?
    )
}