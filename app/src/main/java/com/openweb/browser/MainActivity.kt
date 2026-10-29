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
import android.app.DownloadManager
import android.os.Environment
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.URLUtil
import android.widget.*

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val tabs = mutableListOf<WebView>()
    private var activeTab = 0
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

        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(fileName)
                    setDescription("OpenWeb üzerinden indiriliyor")
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, fileName
                    )
                }
                val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Toast.makeText(this, "İndirme başladı: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "İndirme başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        tabs.add(web)
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
            showTabsMenu()
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


    private fun showTabsMenu() {
        val labels = tabs.mapIndexed { i, tab ->
            "${if (i == activeTab) "✓ " else ""}${tab.title?.takeIf { it.isNotBlank() } ?: "Yeni sekme"}"
        }.toMutableList()
        labels.add("＋ Yeni sekme")
        labels.add("× Geçerli sekmeyi kapat")
        android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Sekmeler (${tabs.size})")
            .setItems(labels.toTypedArray()) { _, which ->
                when {
                    which < tabs.size -> switchTab(which)
                    which == tabs.size -> addTab()
                    else -> closeTab(activeTab)
                }
            }.show()
    }

    private fun addTab() {
        val tab = WebView(this).apply {
            setBackgroundColor(bg)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String) = handleUrl(url)
                override fun onPageFinished(view: WebView, url: String) {
                    if (view === web && !url.startsWith("data:")) address.setText(url)
                }
            }
            webChromeClient = WebChromeClient()
        }
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as ViewGroup
        (web.parent as? ViewGroup)?.removeView(web)
        root.addView(tab, 2, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        tabs.add(tab)
        web = tab
        activeTab = tabs.lastIndex
        loadHome()
        Toast.makeText(this, "Yeni sekme açıldı", Toast.LENGTH_SHORT).show()
    }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices) return
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as ViewGroup
        (web.parent as? ViewGroup)?.removeView(web)
        web = tabs[index]
        activeTab = index
        (web.parent as? ViewGroup)?.removeView(web)
        root.addView(web, 2, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        address.setText(web.url ?: "")
    }

    private fun closeTab(index: Int) {
        if (tabs.size <= 1) {
            loadHome()
            return
        }
        val closing = tabs.removeAt(index)
        (closing.parent as? ViewGroup)?.removeView(closing)
        closing.destroy()
        activeTab = (index - 1).coerceAtLeast(0)
        switchTab(activeTab)
    }

    private fun loadHome() {
        address.setText("")
        val html = """
            <!doctype html>
            <html lang="tr">
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="dark">
            <style>
            *{box-sizing:border-box}
            body{margin:0;background:radial-gradient(circle at 50% 25%,#142b49 0,#080d18 58%);
            color:#f0f5ff;font-family:Arial,sans-serif;min-height:100vh}
            .wrap{width:100%;max-width:620px;margin:auto;padding:52px 22px 30px;text-align:center}
            .logo{width:76px;height:76px;margin:0 auto 18px;border-radius:26px;
            display:grid;place-items:center;background:#111d30;border:1px solid #294566;
            color:#36c9f0;font-size:43px;box-shadow:0 10px 35px #0005}
            h1{font-size:32px;letter-spacing:.4px;margin:0}
            .tagline{color:#91a4c0;margin:10px 0 32px;font-size:14px}
            form{display:flex;align-items:center;background:#111d30;border:1px solid #29405d;
            border-radius:22px;padding:7px 8px 7px 18px;gap:8px;box-shadow:0 8px 25px #0003}
            input{flex:1;min-width:0;background:transparent;border:0;outline:0;
            color:#f0f5ff;font-size:16px;padding:12px 0}
            input::placeholder{color:#91a4c0}
            button{border:0;border-radius:16px;width:46px;height:46px;
            background:#36c9f0;color:#081426;font-size:23px;font-weight:bold}
            .section{margin:30px 0 14px;text-align:left;font-size:14px;font-weight:bold;color:#d5e2f8}
            .links{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
            a{background:linear-gradient(145deg,#14243a,#101a2b);border:1px solid #203651;
            border-radius:20px;padding:19px 5px;color:#d5e2f8;text-decoration:none;font-size:13px}
            a:active{background:#203956}
            .symbol{display:block;font-size:27px;margin-bottom:12px;color:#36c9f0}
            .foot{margin-top:34px;color:#627895;font-size:12px}
            </style>
            </head>
            <body><main class="wrap">
            <div class="logo">◎</div>
            <h1>OpenWeb</h1>
            <p class="tagline">Dünyayı keşfet, özgürce gezin.</p>
            <form action="https://www.google.com/search" method="get">
            <input name="q" placeholder="Ara veya web adresi yaz..." autocomplete="off">
            <button type="submit" aria-label="Ara">➜</button>
            </form>
            <div class="section">Hızlı erişim</div>
            <div class="links">
            <a href="https://www.google.com"><span class="symbol">⌕</span>Google</a>
            <a href="https://www.youtube.com"><span class="symbol">▶</span>YouTube</a>
            <a href="https://www.wikipedia.org"><span class="symbol">◈</span>Wikipedia</a>
            <a href="https://www.reddit.com"><span class="symbol">◉</span>Reddit</a>
            <a href="https://github.com"><span class="symbol">⌘</span>GitHub</a>
            <a href="https://www.bing.com"><span class="symbol">◇</span>Bing</a>
            </div>
            <div class="foot">OpenWeb · Özgürce keşfet.</div>
            </main></body></html>
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
            "Sayfayı yenile",
            "İndirilenler"
        )
        android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("OpenWeb")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Yeni sekme özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Gizli gezinme özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Uzantı desteğini sonraki adımda ele alacağız.", Toast.LENGTH_SHORT).show()
                    3 -> web.reload()
                    4 -> startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
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
