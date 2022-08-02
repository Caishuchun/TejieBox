package com.fortune.tejiebox.bean

data class GameListBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val list: List<Game>,
        val paging: Paging
    ) {
        data class Game(
            val game_channelId: String,
            val game_desc: String,
            val game_icon: String,
            val game_id: Int,
            val game_name: String,
            val game_tag: List<String>,
            val game_type: String
        )

        data class Paging(
            val count: Int,
            val limit: Int,
            val list: String,
            val page: Int
        )
    }
}