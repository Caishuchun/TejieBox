package com.fortune.tejiebox.bean

data class RechargeListBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val pricelist: List<Pricelist>
    ) {
        data class Pricelist(
            val id: Int,
            val money: String
        )
    }
}