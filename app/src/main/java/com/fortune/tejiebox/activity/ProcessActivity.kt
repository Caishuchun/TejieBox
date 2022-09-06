package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_process.*

class ProcessActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ProcessActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    override fun getLayoutId() = R.layout.activity_process

    override fun doSomething() {
        finish()
    }

    override fun destroy() {
    }
}