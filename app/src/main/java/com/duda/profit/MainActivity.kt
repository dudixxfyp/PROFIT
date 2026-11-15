package com.duda.profit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0F141C"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(Bridge(applicationContext), "Android")
            webViewClient = WebViewClient()
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onResume() {
        super.onResume()
        // Recarrega o estado do dia (ex.: água marcada pelo widget)
        web.evaluateJavascript("window.onNativeResume && window.onNativeResume()", null)
    }

    override fun onPause() {
        super.onPause()
        PainelWidget.refreshAll(applicationContext)
    }

    /** Ponte usada pela página: substitui o localStorage pelo armazenamento do app. */
    class Bridge(private val ctx: Context) {
        private val store = Store(ctx)

        @JavascriptInterface
        fun get(key: String): String? = store.get(key)

        @JavascriptInterface
        fun set(key: String, value: String) {
            store.set(key, value)
            if (!key.startsWith("profit-auth")) PainelWidget.refreshAll(ctx)
        }

        @JavascriptInterface
        fun remove(key: String) {
            store.remove(key)
            PainelWidget.refreshAll(ctx)
        }

        @JavascriptInterface
        fun clear() {
            store.clear()
            PainelWidget.refreshAll(ctx)
        }
    }
}
