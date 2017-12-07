package com.github.macpersia.planty_alexa_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * @author will on 3/6/2016.
 */
class LoginWebViewActivity : Activity() {

    internal lateinit var mWebView: WebView

    internal var mWebViewClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, url)
            }
            if (url.startsWith("http") || url.startsWith("https")) {
                return super.shouldOverrideUrlLoading(view, url)
            }

            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivityForResult(i, RESULT_LOGIN)

            return true
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mWebView = findViewById(R.id.webview) as WebView

        mWebView.setWebViewClient(mWebViewClient)

        // Get the intent that started this activity
        val intent = intent
        val data = intent.data

        if (data != null) {
            mWebView.loadUrl(data.toString())
        } else {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOGIN) {
            this@LoginWebViewActivity.finish()
        }
    }

    companion object {

        private val TAG = "LoginWebViewActivity"

        private val RESULT_LOGIN = 1
    }
}
