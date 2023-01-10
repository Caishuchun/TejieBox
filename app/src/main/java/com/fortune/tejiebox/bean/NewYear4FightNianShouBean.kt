package com.fortune.tejiebox.bean

data class NewYear4FightNianShouBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val current_blood: Int,
        val current_energy: Int
    )
}