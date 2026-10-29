package com.openweb.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 0)
            setBackgroundColor(Color.rgb(245, 247, 250))
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        address = EditText(this).apply {
            hint = "Adres veya arama yaz"
            setSingleLine(true)
            textSize = 15f
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(12, 0, 12, 0)
        }

        bar.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        bar.addView(makeButton("Git") { loadAddress() })
        root.addView(bar)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        controls.addView(makeButton("Geri") {
            if (web.canGoBack()) web.goBack()
        })
        controls.addView(makeButton("İleri") {
            if (web.canGoForward()) web.goForward()
        })
        controls.addView(makeButton("Yenile") { web.reload() })
        controls.addView(makeButton("Ana sayfa") {
            web.loadUrl("https://www.google.com")
        })

        root.addView(controls)

        progress = ProgressBar(this, null,
            android.R.attr.progressBarStyleHorizontal)
        root.addView(progress, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 4
        ))

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                return handleUrl(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                address.setText(url)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility =
                    if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        root.addView(web, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        address.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                 event.action == KeyEvent.ACTION_DOWN)) {
                loadAddress()
                true
            } else false
        }

        web.loadUrl("https://www.google.com")
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun loadAddress() {
        var text = address.text.toString().trim()
        if (text.isEmpty()) return

        if (!text.contains("://")) {
            text = if (text.contains(".") && !text.contains(" ")) {
                "https://$text"
            } else {
                "https://www.google.com/search?q=" +
                    Uri.encode(text)
            }
        }

        handleUrl(text)
    }

    private fun handleUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme == "http" || uri.scheme == "https") {
            web.loadUrl(url)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        return true
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
