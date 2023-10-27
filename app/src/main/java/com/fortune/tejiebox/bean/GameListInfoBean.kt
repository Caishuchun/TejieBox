package com.fortune.tejiebox.bean

data class GameListInfoBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val list: List<BaseGameListInfoBean>,
        val recommended_games:List<BaseGameListInfoBean>,
        val paging: Paging
    ) {
        data class Paging(
            val count: Int,
            val limit: Int,
            val list: String,
            val page: Int
        )
    }
}