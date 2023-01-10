package com.fortune.tejiebox.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import com.fortune.tejiebox.R
import com.fortune.tejiebox.base.BaseActivity
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.utils.*
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_new_year_share.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class NewYearShareActivity : BaseActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: NewYearShareActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        const val SHARE_LINK = "share_link"
    }

    private var shareLink: String? = null
    override fun getLayoutId() = R.layout.activity_new_year_share

    @SuppressLint("UseCompatLoadingForDrawables", "CheckResult")
    override fun doSomething() {
        shareLink = intent.getStringExtra(SHARE_LINK)

        RxView.clicks(iv_newYearShare_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        val screenWidth = PhoneInfoUtils.getWidth(this)
        val picWidth = (screenWidth * (200f / 360)).toInt()

        iv_newYearShare_qrCode.setImageBitmap(
            QrCodeUtils.createQRCodeWithLogo(
                QrCodeUtils.createQRCode(
                    shareLink,
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

        RxView.clicks(tv_newYear_share_save)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                Thread {
                    toSavePic()
                }.start()
            }
    }

    /**
     * 保存图片
     */
    private fun toSavePic() {
        ll_newYearShare_root.isDrawingCacheEnabled = true
        ll_newYearShare_root.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        ll_newYearShare_root.drawingCacheBackgroundColor = Color.WHITE

        val width = ll_newYearShare_root.width
        val height = ll_newYearShare_root.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        ll_newYearShare_root.layout(0, 0, width, height)
        ll_newYearShare_root.draw(canvas)

        var fos: FileOutputStream? = null
        val shareImgDir = getExternalFilesDir("shareImg")
        val userId = SPUtils.getString(SPArgument.USER_ID)
        val mD5 = GetDeviceId.getMD5(userId ?: "tejieBox", false)
        val fileName = "shareImg_$mD5.png"
        val file = File(shareImgDir, fileName)
//        if (file.exists() && file.length() > 100) {
//            runOnUiThread {
//                ShareJumpUtils.showDefaultDialog(this, "")
//            }
//            return
//        }
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        } catch (e: Exception) {
            runOnUiThread {
                LogUtils.d("保存图片失败!")
            }
        } finally {
            fos?.flush()
            fos?.close()
            if (file.exists() && file.length() > 100) {
                runOnUiThread {
                    ShareJumpUtils.showDefaultDialog(this, "")
                }
            }
            try {
                //通知相册
                MediaStore.Images.Media.insertImage(
                    contentResolver, file.absolutePath, fileName, null
                )
                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(File(file.path))
                    )
                )
            } catch (e: Exception) {
                runOnUiThread {
                    LogUtils.d("保存图片失败!!")
                }
            }
        }
        ll_newYearShare_root.destroyDrawingCache()
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