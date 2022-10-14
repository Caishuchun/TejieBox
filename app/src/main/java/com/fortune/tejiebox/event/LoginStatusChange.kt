package com.fortune.tejiebox.event

data class LoginStatusChange(
    val isLogin: Boolean,
    val phone: String?,
    val isHaveRewardInteger: Boolean? = false
)