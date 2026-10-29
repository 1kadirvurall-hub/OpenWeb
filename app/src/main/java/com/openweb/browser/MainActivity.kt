package com.openweb.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.*

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar

    private val bg = Color.rgb(8, 13, 24)
    private val panel = Color.rgb(17, 29, 48)
    private val accent = Color.rgb(190, 210, 245)
    private val muted = Color.rgb(145, 164, 192)

    private fun rounded(color: Int, radius: Float = 24f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 8, 14, 4)
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 4, 2, 10)
        }

        val logo = TextView(this).apply {
            text = "◉"
            textSize = 30f
            setTextColor(accent)
            gravity = Gravity.CENTER
        }
        header.addView(logo, LinearLayout.LayoutParams(42, 44))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleBox.addView(TextView(this).apply {
            text = "OpenWeb"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(240, 245, 255))
        })
        titleBox.addView(TextView(this).apply {
            text = "Özgürce keşfet."
            textSize = 11f
            setTextColor(muted)
        })
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))

        header.addView(makeIconButton("▢", "Sekmeler") {
            Toast.makeText(this, "Sekmeler özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
        })
        header.addView(makeIconButton("⋮", "Menü") {
            showBrowserMenu()
        })
        root.addView(header)

        val addressBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, 40f)
            setPadding(14, 0, 8, 0)
        }

        address = EditText(this).apply {
            hint = "Ara veya adres yaz…"
            setHintTextColor(muted)
            setTextColor(Color.rgb(240, 245, 255))
            textSize = 15f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
            background = null
            setPadding(4, 0, 4, 0)
        }
        addressBar.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        addressBar.addView(makeIconButton("➜", "Git") {
            loadAddress()
        })
        root.addView(addressBar)

        progress = ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accent)
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 3
        ))

        web = WebView(this).apply {
            setBackgroundColor(bg)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView, url: String
                ): Boolean {
                    return handleUrl(url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (!url.startsWith("data:")) {
                        address.setText(url)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    this@MainActivity.progress.progress = newProgress
                    this@MainActivity.progress.visibility =
                        if (newProgress < 100) View.VISIBLE else View.GONE
                }
            }
        }

        root.addView(web, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(16, 26, 43), 24f)
            setPadding(4, 4, 4, 4)
        }

        bottom.addView(makeNavButton("‹", "Geri") {
            if (web.canGoBack()) web.goBack()
        }, LinearLayout.LayoutParams(0, 52, 1f))

        bottom.addView(makeNavButton("›", "İleri") {
            if (web.canGoForward()) web.goForward()
        }, LinearLayout.LayoutParams(0, 52, 1f))

        bottom.addView(makeNavButton("⌂", "Ana sayfa") {
            loadHome()
        }, LinearLayout.LayoutParams(0, 52, 1f))

        bottom.addView(makeNavButton("▢", "Sekmeler") {
            Toast.makeText(this, "Sekmeler özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, 52, 1f))

        bottom.addView(makeNavButton("⋮", "Menü") {
            showBrowserMenu()
        }, LinearLayout.LayoutParams(0, 52, 1f))

        root.addView(bottom, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
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

        loadHome()
    }

    private fun makeIconButton(
        symbol: String, description: String, action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = symbol
            contentDescription = description
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(Color.TRANSPARENT, 30f)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(42, 44)
        }
    }

    private fun makeNavButton(
        symbol: String, description: String, action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = symbol
            contentDescription = description
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setOnClickListener { action() }
        }
    }

    private fun loadHome() {
        address.setText("")
        val html = """
            <!doctype html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            *{box-sizing:border-box}
            body{margin:0;background:#080d18;color:#f0f5ff;
            font-family:Arial,sans-serif;min-height:100vh;
            display:flex;align-items:center;justify-content:center}
            .wrap{width:100%;max-width:560px;padding:24px;text-align:center}
            .logo{font-size:54px;color:#bed2f5}
            h1{font-size:30px;margin:8px 0}
            p{color:#91a4c0;margin:8px 0 28px}
            form{display:flex;background:#111d30;border-radius:32px;
            padding:6px 8px 6px 18px;gap:8px}
            input{flex:1;min-width:0;background:transparent;border:0;
            outline:0;color:#f0f5ff;font-size:16px}
            button{border:0;border-radius:50%;width:42px;height:42px;
            background:#243956;color:#fff;font-size:20px}
            .links{display:grid;grid-template-columns:repeat(3,1fr);
            gap:12px;margin-top:28px}
            a{background:#111d30;border-radius:18px;padding:18px 4px;
            color:#d5e2f8;text-decoration:none;font-size:13px}
            </style>
            </head>
            <body><div class="wrap">
            <div class="logo">◉</div><h1>OpenWeb</h1>
            <p>Özgürce keşfet.</p>
            <form action="https://www.google.com/search" method="get">
            <input name="q" placeholder="Google'da ara..." autocomplete="off">
            <button type="submit">➜</button></form>
            <div class="links">
            <a href="https://www.google.com">◎<br><br>Google</a>
            <a href="https://www.youtube.com">▶<br><br>YouTube</a>
            <a href="https://www.wikipedia.org">◈<br><br>Wikipedia</a>
            </div></div></body></html>
        """.trimIndent()
        web.loadDataWithBaseURL("https://openweb.local/", html, "text/html", "UTF-8", null)
    }

    private fun loadAddress() {
        var text = address.text.toString().trim()
        if (text.isEmpty()) return

        if (!text.contains("://")) {
            text = if (text.contains(".") && !text.contains(" ")) {
                "https://$text"
            } else {
                "https://www.google.com/search?q=" + Uri.encode(text)
            }
        }
        handleUrl(text)
    }

    private fun handleUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme == "http" || uri.scheme == "https") {
            web.loadUrl(url)
        } else {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(this, "Bu bağlantı açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    private fun showBrowserMenu() {
        val options = arrayOf(
            "Yeni sekme",
            "Gizli sekme",
            "Uzantılar",
            "Sayfayı yenile"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("OpenWeb")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Yeni sekme özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Gizli gezinme özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Uzantı desteğini sonraki adımda ele alacağız.", Toast.LENGTH_SHORT).show()
                    3 -> web.reload()
                }
            }
            .show()
    }

    @Deprecated("Deprecated in Android")
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
