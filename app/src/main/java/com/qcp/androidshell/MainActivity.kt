package com.qcp.androidshell

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

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
        runBlocking {
            try {
                startForegroundService(intent)
                println("启动成功")
            } catch (e: Exception) {
                println("出现错误")
                print(e)
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startFrontService()


        WebView.setWebContentsDebuggingEnabled(true)

        webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
//            override fun shouldInterceptRequest(
//                view: WebView,
//                request: WebResourceRequest
//            ): WebResourceResponse? {
//                val res = super.shouldInterceptRequest(view, request)
//                if (request.requestHeaders["Referer"] == null) return res
//
//                val local = URI(request.requestHeaders["Referer"])
//                if (request.url.host != local.host) {
//                    println("拦截 ${request.url}")
//                    val (contentType, realContent) =
//                        loadRealContent(request.url.toString(), request.requestHeaders)
//
//                    // 返回包含实际请求内容和自定义请求头的 WebResourceResponse
//                    return WebResourceResponse(
//                        contentType,
//                        "utf-8",
//                        ByteArrayInputStream(realContent)
//                    )
//                        .apply {
//                            val headers = mutableMapOf<String, String>()
//                            headers["Access-Control-Allow-Origin"] = "*"
//                            setStatusCodeAndReasonPhrase(200, "OK")
//                            responseHeaders = headers
//                        }
//                }
//                return res
//
//            }
        }
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

        }

        webView.addJavascriptInterface(JsObject(), "shell")
        webView.loadUrl("file:///android_asset/index.html")
//        webView.loadUrl("http://192.168.185.36:8686/#/")
        setContentView(webView)

//        runBlocking {
//            delay(200)
//            setTheme(R.style.Theme_AndroidShell)
//            setContentView(webView)
//        }
//        println(webSettings.userAgentString)

    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.postInvalidate()
    }

    private fun loadRealContent(
        url: String,
        requestHeaders: Map<String, String>?
    ): Pair<String, ByteArray> {
        // 使用 HttpURLConnection 进行网络请求获取实际请求内容
        val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection

        // 将 request 的头信息传递到实际请求中
        requestHeaders?.let { headers ->
            for ((key, value) in headers) {
                connection.setRequestProperty(key, value)
            }
        }

        try {
            val inputStream: InputStream = connection.inputStream

            return Pair(connection.contentType, inputStream.readBytes())
        } finally {
            connection.disconnect()
        }
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
