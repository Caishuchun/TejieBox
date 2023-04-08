package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.text.Html
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.utils.*
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_invite_page.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class InvitePageActivity : BaseActivity() {

    private var inViteUrl: String? = null
    private var inviteCode: String? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: InvitePageActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val INVITE_URL = "inviteUrl"
        const val INVITE_CODE = "inviteCode"
    }

    override fun getLayoutId() = R.layout.activity_invite_page

    @SuppressLint("CheckResult")
    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        inViteUrl = intent.getStringExtra(INVITE_URL)
        inviteCode = intent.getStringExtra(INVITE_CODE)

        RxView.clicks(iv_invitePage_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        tv_invitePage_InviteCode.text = Html.fromHtml("<u>$inviteCode</u>")
        tv_invitePage_InviteCode.text = Html.fromHtml("<u>80A63Q</u>")

        val screenWidth = PhoneInfoUtils.getWidth(this)
        val picWidth = (screenWidth * (200f / 360)).toInt()

        iv_invitePage_qrCode.setImageBitmap(
            QrCodeUtils.createQRCodeWithLogo(
                QrCodeUtils.createQRCode(
                    inViteUrl,
                    picWidth, picWidth,
                    "UTF-8",
                    "H",
                    "0",
                    Color.BLACK,
                    Color.WHITE
                ),
                (resources.getDrawable(R.mipmap.icon) as BitmapDrawable).bitmap,
                0.2f
            )
        )

        RxView.clicks(tv_invitePage_save)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
//                tv_invitePage_tips1.visibility = View.VISIBLE
//                tv_invitePage_tips2.visibility = View.VISIBLE
//                ShareUtils.shareText(this,"https:69t.top")
                Thread {
                    toSavePic()
                }.start()
            }
    }

    /**
     * 保存图片
     */
    private fun toSavePic() {
        ll_invitePage_root.isDrawingCacheEnabled = true
        ll_invitePage_root.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        ll_invitePage_root.drawingCacheBackgroundColor = Color.WHITE

        val width = ll_invitePage_root.width
        val height = ll_invitePage_root.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        ll_invitePage_root.layout(0, 0, width, height)
        ll_invitePage_root.draw(canvas)

        val screenWidth = PhoneInfoUtils.getWidth(this)
        val location = IntArray(2)
        tv_invitePage_tips1.getLocationOnScreen(location)
        val mPaint = Paint()
        mPaint.color = Color.BLACK
        mPaint.style = Paint.Style.FILL_AND_STROKE
        mPaint.strokeWidth = 1.5f
        mPaint.textSize = (screenWidth.toFloat() / 360) * 14
        canvas.drawText(
            tv_invitePage_tips1.text.toString(),
            location[0].toFloat(),
            location[1].toFloat() + (screenWidth.toFloat() / 360) * 10,
            mPaint
        )
        tv_invitePage_tips2.getLocationOnScreen(location)
        canvas.drawText(
            tv_invitePage_tips2.text.toString(),
            location[0].toFloat(),
            location[1].toFloat() + (screenWidth.toFloat() / 360) * 10,
            mPaint
        )
        var fos: FileOutputStream? = null
        val shareImgDir = getExternalFilesDir("shareImg")
        val userId = SPUtils.getString(SPArgument.USER_ID)
        val mD5 = GetDeviceId.getMD5(userId ?: "tejieBox", false)
        val fileName = "shareImg_$mD5.png"
        val file = File(shareImgDir, fileName)
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        } catch (e: Exception) {
            runOnUiThread {
                ToastUtils.show("保存分享界面失败!")
            }
        } finally {
            fos?.flush()
            fos?.close()
            if (file.exists() && file.length() > 100) {
                runOnUiThread {
                    ShareJumpUtils.showDefaultDialog(this, bitmapFilePath = file.path)
//                    ShareUtils.shareImage(this, bitmap)
                }
            }
//            try {
//                //通知相册
//                MediaStore.Images.Media.insertImage(
//                    contentResolver, file.absolutePath, fileName, null
//                )
//                sendBroadcast(
//                    Intent(
//                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
//                        Uri.fromFile(File(file.path))
//                    )
//                )
//            } catch (e: Exception) {
//                runOnUiThread {
//                    ToastUtils.show("保存分享界面失败!")
//                }
//            }
        }
        ll_invitePage_root.destroyDrawingCache()
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun destroy() {
    }
}