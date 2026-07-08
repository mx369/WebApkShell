package com.qcp.androidshell

import android.content.Intent
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var snapshot: ImageView? = null
    private var visualStateCallbackId = 0L

    private fun getStatusBarHeight(): Int {
        var res = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val density = resources.displayMetrics.density
        if (resourceId > 0) res = (resources.getDimensionPixelSize(resourceId) / density).toInt()
        return res
    }

    private fun backHome() {
        val setIntent = Intent(Intent.ACTION_MAIN)
        setIntent.addCategory(Intent.CATEGORY_HOME)
        setIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(setIntent)
    }

    private fun startFrontService() {
        val intent = Intent(this, ForegroundService::class.java)
        try {
            startForegroundService(intent)
            println("启动成功")
        } catch (e: Exception) {
            println("出现错误")
            print(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 不切主题：保持 manifest 的启动屏 windowBackground，WebView 透明让其透过来
        super.onCreate(savedInstanceState)
        startFrontService()

        WebView.setWebContentsDebuggingEnabled(true)

        webView = WebView(this)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(Color.WHITE)
        webView.webViewClient = WebViewClient()
        webView.scrollBarSize = 0
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.mixedContentMode = 0
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.allowFileAccess = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowContentAccess = true

        class JsObject {
            @get:JavascriptInterface
            val statusBarHeight = "${getStatusBarHeight()}px"

            @JavascriptInterface
            fun dismissSplash() {
                webView.post { webView.visibility = View.VISIBLE }
            }
        }

        webView.addJavascriptInterface(JsObject(), "shell")
        webView.setVisibility(View.INVISIBLE)
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)

        // 到时间显示 WebView，启动屏（windowBackground）被盖住
        webView.postDelayed({
            webView.visibility = View.VISIBLE
        }, ShellConfig.SPLASH_TIMEOUT)
    }

    override fun onPause() {
        // 截取当前 WebView 画面，覆盖一层 ImageView，防止回前台时闪白
        if (webView.width > 0 && webView.height > 0) {
            val bmp = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            webView.draw(Canvas(bmp))
            ImageView(this).apply {
                setImageBitmap(bmp)
                scaleType = ImageView.ScaleType.FIT_XY
                addContentView(this, FrameLayout.LayoutParams(-1, -1))
                snapshot = this
            }
        }
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        webView.invalidate()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.invalidate()
        // 使用 postVisualStateCallback 精确监测 Chromium 渲染完成时机。
        // 当 Chromium 确认视觉状态已准备好（下一次 onDraw 能画出画面）时回调 onComplete。
        visualStateCallbackId++
        val currentId = visualStateCallbackId
        webView.postVisualStateCallback(currentId, object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                if (requestId != currentId) return
                webView.post {
                    snapshot?.let { (it.parent as? ViewGroup)?.removeView(it) }
                    snapshot = null
                }
            }
        })
        // 兜底：如果 2 秒内 callback 没触发（极端情况），强制移除 overlay
        webView.postDelayed({
            if (snapshot != null && currentId == visualStateCallbackId) {
                snapshot?.let { (it.parent as? ViewGroup)?.removeView(it) }
                snapshot = null
            }
        }, 2000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_BACK) {
            if (webView.canGoBack()) webView.goBack()
            else backHome()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
