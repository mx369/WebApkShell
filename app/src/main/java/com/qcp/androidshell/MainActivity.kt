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
    private var splashDismissed = false

    private fun getStatusBarHeightPx(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun getStatusBarHeight(): Int {
        val density = resources.displayMetrics.density
        return (getStatusBarHeightPx() / density).toInt()
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
        // 保持 manifest 的启动屏作为冷启动背景，页面显示后再替换为白色
        super.onCreate(savedInstanceState)
        startFrontService()

        WebView.setWebContentsDebuggingEnabled(true)

        webView = WebView(this)
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
                webView.post { dismissSplashInternal() }
            }
        }

        webView.addJavascriptInterface(JsObject(), "shell")
        webView.setVisibility(View.INVISIBLE)
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)

        // 到时间显示 WebView，启动屏（windowBackground）被盖住
        webView.postDelayed({
            dismissSplashInternal()
        }, ShellConfig.SPLASH_TIMEOUT)
    }

    private fun dismissSplashInternal() {
        if (splashDismissed) return
        splashDismissed = true
        webView.alpha = 0f
        webView.visibility = View.VISIBLE
        webView.animate()
            .alpha(1f)
            .setDuration(600)
            .withEndAction {
                webView.postDelayed({
                    window.setBackgroundDrawableResource(android.R.color.white)
                }, 600)
            }
            .start()
    }

    private fun removeSnapshot() {
        snapshot?.let { (it.parent as? ViewGroup)?.removeView(it) }
        snapshot = null
    }

    override fun onPause() {
        // 使上一轮恢复回调失效，避免它误删本轮创建的遮罩
        visualStateCallbackId++
        removeSnapshot()

        // 截取当前 WebView 画面，覆盖一层 ImageView，防止回前台时闪白
        val snapshotTop = getStatusBarHeightPx().coerceIn(0, webView.height)
        val snapshotHeight = webView.height - snapshotTop
        if (webView.width > 0 && snapshotHeight > 0) {
            val bmp = Bitmap.createBitmap(webView.width, snapshotHeight, Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply {
                translate(0f, -snapshotTop.toFloat())
                webView.draw(this)
            }
            ImageView(this).apply {
                setImageBitmap(bmp)
                scaleType = ImageView.ScaleType.FIT_XY
                addContentView(this, FrameLayout.LayoutParams(-1, snapshotHeight).apply {
                    topMargin = snapshotTop
                })
                snapshot = this
            }
        }
        webView.invalidate()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.invalidate()
        // 使用 postVisualStateCallback 精确监测 Chromium 渲染完成时机。
        // 当 Chromium 确认视觉状态已准备好（下一次 onDraw 能画出画面）时回调 onComplete。
        visualStateCallbackId++
        val currentId = visualStateCallbackId
        webView.postVisualStateCallback(currentId, object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                if (requestId != visualStateCallbackId) return
                // callback 只保证下一次 draw 可用；等待一个完整绘制帧后再移除遮罩
                webView.postOnAnimation firstFrame@{
                    if (requestId != visualStateCallbackId) return@firstFrame
                    webView.postOnAnimation secondFrame@{
                        if (requestId != visualStateCallbackId) return@secondFrame
                        removeSnapshot()
                    }
                }
            }
        })
        // 兜底：如果 2 秒内 callback 没触发（极端情况），强制移除 overlay
        webView.postDelayed({
            if (snapshot != null && currentId == visualStateCallbackId) {
                removeSnapshot()
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
