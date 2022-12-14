package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.FilesArgument
import com.fortune.tejiebox.http.HttpUrls
import com.fortune.tejiebox.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_web.*
import java.util.concurrent.TimeUnit

class WebActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: WebActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val TYPE = "type"
        const val USER_AGREEMENT = "user_agreement"
        const val PRIVACY_AGREEMENT = "privacy_agreement"
        const val GAME_WEB = "game_web"
        const val GAME_NAME = "game_name"
        const val GAME_URL = "game_url"
    }

    override fun getLayoutId() = R.layout.activity_web

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        when (intent.getStringExtra(TYPE)) {
            USER_AGREEMENT -> {
                tv_web_title.text = getString(R.string.app_name)
                toLoadUrl((if (BaseAppUpdateSetting.appType) HttpUrls.REAL_URL else HttpUrls.TEST_URL) + FilesArgument.PROTOCOL_SERVICE)
            }
            PRIVACY_AGREEMENT -> {
                tv_web_title.text = getString(R.string.app_name)
                toLoadUrl((if (BaseAppUpdateSetting.appType) HttpUrls.REAL_URL else HttpUrls.TEST_URL) + FilesArgument.PROTOCOL_PRIVACY)
            }
            GAME_WEB -> {
                val gameName = intent.getStringExtra(GAME_NAME)
                val gameUrl = intent.getStringExtra(GAME_URL)
                tv_web_title.text = gameUrl
                tv_web_title.visibility = View.VISIBLE
                toLoadUrl("http://$gameUrl")
            }
        }
        RxView.clicks(iv_web_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }
    }

    /**
     * ??????url
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun toLoadUrl(url: String) {
        web_web.loadUrl(url)
        val webSettings: WebSettings = web_web.settings
        //??????js
        webSettings.javaScriptEnabled = true
        //?????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webSettings.blockNetworkImage = false

        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
//        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS

        //????????????
        webSettings.setSupportZoom(true) //????????????????????????true??????????????????????????????
        webSettings.builtInZoomControls = true //????????????????????????????????????false?????????WebView????????????
        webSettings.displayZoomControls = false //???????????????????????????
        //??????????????????
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT //??????webview?????????
        webSettings.allowFileAccess = true //????????????????????????
        webSettings.javaScriptCanOpenWindowsAutomatically = true //????????????JS???????????????
        webSettings.loadsImagesAutomatically = true //????????????????????????
        webSettings.defaultTextEncodingName = "utf-8" //??????????????????

        web_web.requestFocus()

        web_web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                pb_web.progress = newProgress
                if (newProgress == 100)
                    pb_web.visibility = View.GONE
            }
        }

        web_web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(
                view: WebView,
                url: String
            ) {
                super.onPageFinished(view, url)
//                runOnUiThread {
//                    DialogUtils.dismissLoading()
//                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
//                super.onReceivedSslError(view, handler, error)
                //????????????????????????????????????ssl???????????????
                handler!!.proceed()
            }

            // ?????????????????????????????????
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                return if (Build.VERSION.SDK_INT < 26) {
                    view.loadUrl(url) // ??????????????? WebView ????????? url
                    false
                } else {
                    false
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && web_web.canGoBack()) {
            web_web.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun destroy() {
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}