package com.fortune.tejiebox.bean

data class LikeAndPlayingBean(
    val code: Int,
    val `data`: List<Data>,
    val msg: String
) {
    data class Data(
        val game_icon: String,
        val game_id: Int,
        val game_name: String
    )
}