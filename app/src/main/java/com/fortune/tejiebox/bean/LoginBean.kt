package com.fortune.tejiebox.bean

class LoginBean {
    var code = 0
    var msg: String? = null
    var data: Data? = null

    inner class Data {
        var token: String? = null
        var first_login = 0
        var phone: String? = null
    }
}

