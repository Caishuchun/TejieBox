package com.fortune.tejiebox.bean

data class BaseGameListInfoBean(
    val game_channelId: String,
    val game_cover: String,
    val game_desc: String,
    val game_id: Int,
    val game_name: String,
    val game_tag: List<String>,
    val game_type: String,
    val game_top: Int, //0不推荐1推荐
    val icon_type: Int, //1新 2红 3没
    val duration_sum: Int, //仅在在玩和收藏的时长显示
    val is_integral: Int = 0 //1是积分置顶游戏，0不是
)