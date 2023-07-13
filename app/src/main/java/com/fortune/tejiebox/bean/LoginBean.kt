package com.fortune.tejiebox.bean

class LoginBean {
    var code = 0
    var msg: String? = null
    var data: Data? = null

    inner class Data {
        var token: String? = null
        var first_login = 0
        var phone: String? = null
        var user_id: String? = null
        val user_id_raw: String? = null
        var id_card: Int? = 0
        var card_name: String? = null
        var car_num: String? = null
        var integral: Int? = null
        var account: String? = null
    }
}

