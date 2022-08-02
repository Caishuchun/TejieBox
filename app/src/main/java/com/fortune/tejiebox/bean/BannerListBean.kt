package com.fortune.tejiebox.bean

data class BannerListBean(
    val code: Int,
    val `data`: List<Data>,
    val msg: String
) {
    data class Data(
        val game_cover: String,
        val game_id: Int
    )
}