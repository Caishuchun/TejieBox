package com.fortune.tejiebox.bean

data class RoleListBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val gameVersion: String,
        val roleList: List<Role>
    ) {
        data class Role(
            val areaName: String,
            val job: Int,
            val jobName: String,
            val lev: Int,
            val recharge: Int,
            val regServerId: Int,
            val roleId: Int,
            val roleName: String,
            val serverId: Int,
            val serverName: String
        )
    }
}