package com.fortune.tejiebox.bean

data class GameListInfoBean2(
    val code: Int,
    val `data`: List<BaseGameListInfoBean>,
    val msg: String
) {
}