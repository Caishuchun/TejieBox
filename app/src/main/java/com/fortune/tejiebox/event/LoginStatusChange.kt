package com.fortune.tejiebox.event

data class LoginStatusChange(
    val isLogin: Boolean,
    val phone: String?,
    val account: String?,
    val isHaveRewardInteger: Boolean? = false,
    val isFirstLogin: Boolean? = false
)