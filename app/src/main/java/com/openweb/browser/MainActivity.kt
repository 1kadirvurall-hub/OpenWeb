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
    private lateinit var tabStrip: LinearLayout

    private lateinit var web: WebView
    private val tabs = mutableListOf<WebView>()
    private val privateTabs = mutableSetOf<WebView>()
    private var activeTab = 0
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar

    private val bg = Color.BLACK
    private val panel = Color.BLACK
    private val privateBg = Color.rgb(8, 13, 24)
    private val accent = Color.rgb(190, 210, 245)
    private val muted = Color.rgb(145, 164, 192)
    private val prefs by lazy { getSharedPreferences("openweb_settings", MODE_PRIVATE) }
    private var addressBarAtBottom = false

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

        addressBarAtBottom = prefs.getBoolean("address_bottom", false)
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
            showTabsMenu()
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
        if (!addressBarAtBottom) root.addView(addressBar)

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
        if (addressBarAtBottom) root.addView(addressBar)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.BLACK, 24f)
            setPadding(4, 4, 4, 4)
        }

        bottom.addView(makeNavButton("‹", "Geri") {
            if (web.canGoBack()) web.goBack()
        }, LinearLayout.LayoutParams(0, 62, 1f))

        bottom.addView(makeNavButton("›", "İleri") {
            if (web.canGoForward()) web.goForward()
        }, LinearLayout.LayoutParams(0, 62, 1f))

        bottom.addView(makeNavButton("⌂", "Ana sayfa") {
            loadHome()
        }, LinearLayout.LayoutParams(0, 62, 1f))

        bottom.addView(makeNavButton("▢", "Sekmeler") {
            showTabsMenu()
        }, LinearLayout.LayoutParams(0, 62, 1f))

        bottom.addView(makeNavButton("⋮", "Menü") {
            showBrowserMenu()
        }, LinearLayout.LayoutParams(0, 62, 1f))

        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 6, 4, 6)
            background = rounded(Color.BLACK, 14f)
        }

        val tabScroll = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(tabStrip, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }


        root.addView(tabScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 58
        ))
        root.removeView(bottom)
        root.addView(bottom, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
        refreshTabStrip()

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
            textSize = 29f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(Color.TRANSPARENT, 30f)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(54, 54)
        }
    }

    private fun makeNavButton(
        symbol: String, description: String, action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = symbol
            contentDescription = description
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setOnClickListener { action() }
        }
    }


    private fun showTabsMenu() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.BLACK)
        }

        val dialog = android.app.AlertDialog.Builder(
            this, android.R.style.Theme_Material_Dialog_Alert
        ).setTitle("Sekmeler (${tabs.size})")
            .setView(content)
            .setNegativeButton("Kapat", null)
            .create()

        val grid = android.widget.GridLayout(this).apply {
            columnCount = 2
            setBackgroundColor(Color.BLACK)
        }

        tabs.forEachIndexed { i, tab ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                background = rounded(
                    if (i == activeTab) Color.rgb(28, 35, 48) else Color.rgb(15, 15, 15),
                    16f
                )
                setOnClickListener {
                    dialog.dismiss()
                    switchTab(i)
                }
            }

            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            top.addView(TextView(this).apply {
                text = if (i == activeTab) "●" else "○"
                textSize = 13f
                setTextColor(if (i == activeTab) accent else Color.GRAY)
            })

            top.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            top.addView(TextView(this).apply {
                text = "×"
                textSize = 23f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(255, 110, 110))
                setPadding(8, 0, 0, 0)
                setOnClickListener {
                    dialog.dismiss()
                    closeTab(i)
                }
            })
            card.addView(top)

            card.addView(TextView(this).apply {
                text = tab.title?.takeIf { it.isNotBlank() && it != "about:blank" }
                    ?: "Yeni sekme"
                textSize = 14f
                setTextColor(Color.WHITE)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, 10, 0, 6)
            })

            card.addView(TextView(this).apply {
                text = tab.url?.takeIf { it.isNotBlank() && it != "about:blank" }
                    ?.removePrefix("https://")?.removePrefix("http://")
                    ?: "Ana sayfa"
                textSize = 11f
                setTextColor(Color.LTGRAY)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            })

            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = 132
                columnSpec = android.widget.GridLayout.spec(i % 2, 1f)
                setMargins(5, 5, 5, 5)
            }
            grid.addView(card, params)
        }

        content.addView(grid)
        content.addView(TextView(this).apply {
            text = "＋  Yeni sekme"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setPadding(12, 16, 12, 12)
            setOnClickListener {
                dialog.dismiss()
                addTab()
            }
        })

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accent)
    }

    private fun refreshTabStrip() {
            tabStrip.removeAllViews()
            tabs.forEachIndexed { i, tab ->
                val label = tab.title?.takeIf { it.isNotBlank() && it != "about:blank" }
                    ?: tab.url?.takeIf { it.isNotBlank() && it != "about:blank" }
                        ?.substringAfter("://")?.substringBefore("/")?.removePrefix("www.")
                    ?: "Yeni sekme"

                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12, 8, 8, 8)
                    background = rounded(
                        if (i == activeTab) Color.rgb(35, 42, 55) else Color.rgb(18, 18, 18),
                        12f
                    )
                }

                item.addView(TextView(this).apply {
                    text = label.take(18)
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setOnClickListener { switchTab(i) }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

                item.addView(TextView(this).apply {
                    text = "×"
                    textSize = 20f
                    setTextColor(Color.LTGRAY)
                    setPadding(10, 0, 0, 0)
                    setOnClickListener { closeTab(i) }
                })

                item.setOnClickListener { switchTab(i) }
                tabStrip.addView(item, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
                ).apply { rightMargin = 6 })
            }

            tabStrip.addView(TextView(this).apply {
                text = "+"
                textSize = 25f
                gravity = Gravity.CENTER
                setTextColor(accent)
                setPadding(12, 0, 12, 0)
                setOnClickListener { addTab() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
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
        refreshTabStrip()
        loadHome()
        Toast.makeText(this, "Yeni sekme açıldı", Toast.LENGTH_SHORT).show()
    }

    private fun addPrivateTab() {
        val tab = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            clearCache(true)
            clearHistory()
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
        privateTabs.add(tab)
        tab.setBackgroundColor(privateBg)
        android.webkit.CookieManager.getInstance().setAcceptCookie(false)
        web = tab
        activeTab = tabs.lastIndex
        refreshTabStrip()
        loadHome()
        Toast.makeText(this, "Gizli sekme açıldı", Toast.LENGTH_SHORT).show()
    }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices) return
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as ViewGroup
        (web.parent as? ViewGroup)?.removeView(web)
        web = tabs[index]
        activeTab = index
        refreshTabStrip()
        val privateMode = privateTabs.contains(web)
        val rootView = root
        rootView.setBackgroundColor(if (privateMode) privateBg else bg)
        rootView.getChildAt(1)?.background = rounded(if (privateMode) privateBg else panel, 40f)
        web.setBackgroundColor(if (privateMode) privateBg else bg)
        android.webkit.CookieManager.getInstance().setAcceptCookie(!privateTabs.contains(web))
        web.settings.domStorageEnabled = !privateTabs.contains(web)
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
        refreshTabStrip()
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
            ${if (privateTabs.contains(web)) "body{background:#080d18!important}.logo,form,a{background:#101a2b!important;border-color:#203651!important}button{background:#36c9f0!important;color:#081426!important}a:active{background:#203956!important}" else ""}
            *{box-sizing:border-box}
            body{margin:0;background:#000000;
            color:#f0f5ff;font-family:Arial,sans-serif;min-height:100vh}
            .wrap{width:100%;max-width:620px;margin:auto;padding:52px 22px 30px;text-align:center}
            .brand{display:flex;align-items:center;justify-content:center;gap:12px;margin:0 auto 18px}.logo{width:56px;height:56px;flex-shrink:0;margin:0;border-radius:20px;
            display:grid;place-items:center;background:#000000;border:1px solid #222222;
            color:#36c9f0;font-size:32px;box-shadow:0 10px 35px #0005}
            h1{font-size:29px;letter-spacing:.4px;margin:0}
            .tagline{color:#91a4c0;margin:10px 0 32px;font-size:14px}
            form{display:flex;align-items:center;background:#000000;border:1px solid #222222;
            border-radius:22px;padding:7px 8px 7px 18px;gap:8px;box-shadow:0 8px 25px #0003}
            input{flex:1;min-width:0;background:transparent;border:0;outline:0;
            color:#f0f5ff;font-size:16px;padding:12px 0}
            input::placeholder{color:#91a4c0}
            button{border:0;border-radius:16px;width:46px;height:46px;
            background:#36c9f0;color:#081426;font-size:23px;font-weight:bold}
            .section{margin:30px 0 14px;text-align:left;font-size:14px;font-weight:bold;color:#d5e2f8}
            .links{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
            .quick{position:relative;background:#000;border:1px solid #292929;
            border-radius:20px;min-height:94px;padding:16px 4px 12px;
            color:#d5e2f8;text-decoration:none;font-size:13px;
            display:flex;flex-direction:column;align-items:center;justify-content:center;gap:9px}
            .quick:active{background:#151515}
            .quick img{width:32px;height:32px;object-fit:contain;border-radius:8px}
            .quick .remove{position:absolute;right:5px;top:3px;width:23px;height:23px;
            border:0;border-radius:50%;background:#252525;color:#fff;font-size:15px;padding:0}
            .add{width:100%;min-height:94px;padding:16px 4px 12px;
            background:#000;border:1px dashed #36c9f0;border-radius:20px;
            color:#36c9f0;font-size:32px;font-weight:normal;
            display:flex;flex-direction:column;align-items:center;justify-content:center;
            box-sizing:border-box}
            .symbol{display:block;font-size:27px;margin-bottom:12px;color:#36c9f0}
            .foot{margin-top:34px;color:#627895;font-size:12px}
            </style>
            </head>
            <body><main class="wrap">
            <div class="brand"><div class="logo">◉</div><h1>OpenWeb</h1></div>
            <p class="tagline">Dünyayı keşfet, özgürce gezin.</p>
            <form action="https://www.google.com/search" method="get">
            <input name="q" placeholder="Ara veya web adresi yaz..." autocomplete="off">
            <button type="submit" aria-label="Ara">➜</button>
            </form>
            <div class="section">Hızlı erişim</div>
            <div class="links" id="quickLinks"></div>
            <script>
            (function(){
              const key = "openweb_quick_links";
              const defaults = [
                {name:"Google",url:"https://www.google.com"},
                {name:"YouTube",url:"https://www.youtube.com"},
                {name:"Wikipedia",url:"https://www.wikipedia.org"},
                {name:"Reddit",url:"https://www.reddit.com"},
                {name:"GitHub",url:"https://github.com"},
                {name:"Bing",url:"https://www.bing.com"}
              ];
              let sites;
              try { sites = JSON.parse(localStorage.getItem(key)) || defaults; }
              catch(e) { sites = defaults; }
              const host = document.getElementById("quickLinks");
              function draw(){
                host.innerHTML = "";
                sites.forEach(function(site,index){
                  const a = document.createElement("a");
                  a.className = "quick";
                  a.href = site.url;
                  const img = document.createElement("img");
                  try {
                    const u = new URL(site.url);
                    img.src = "https://www.google.com/s2/favicons?domain=" +
                      encodeURIComponent(u.hostname) + "&sz=128";
                  } catch(e) { img.alt = "🌐"; }
                  img.alt = "";
                  img.onerror = function(){ this.style.display="none"; };
                  const label = document.createElement("span");
                  label.textContent = site.name;
                  const remove = document.createElement("button");
                  remove.className = "remove";
                  remove.type = "button";
                  remove.textContent = "×";
                  remove.setAttribute("aria-label","Kısayolu sil");
                  remove.onclick = function(ev){
                    ev.preventDefault(); ev.stopPropagation();
                    sites.splice(index,1);
                    save();
                  };
                  a.appendChild(img); a.appendChild(label); a.appendChild(remove);
                  host.appendChild(a);
                });
                const add = document.createElement("button");
                add.className = "add";
                add.type = "button";
                add.textContent = "+";
                add.setAttribute("aria-label","Site ekle");
                add.onclick = function(){
                  let url = prompt("Eklenecek sitenin adresi:");
                  if(!url) return;
                  url = url.trim();
                  if(!/^https?:\/\//i.test(url)) url = "https://" + url;
                  try {
                    const parsed = new URL(url);
                    if(!/^https?:$/.test(parsed.protocol)) throw new Error();
                    url = parsed.href;
                  } catch(e) {
                    alert("Geçerli bir web adresi gir.");
                    return;
                  }
                  const name = prompt("Site adı:", new URL(url).hostname.replace(/^www\./,""));
                  if(name === null) return;
                  sites.push({name:name.trim() || new URL(url).hostname,url:url});
                  save();
                };
                host.appendChild(add);
              }
              function save(){
                try { localStorage.setItem(key,JSON.stringify(sites)); }
                catch(e) {}
                draw();
              }
              draw();
            })();
            </script>
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
            "İndirilenler",
            "Arama çubuğu: " + if (addressBarAtBottom) "Üste al" else "Alta al"
        )
        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("OpenWeb")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Yeni sekme özelliğini sonraki adımda ekleyeceğiz.", Toast.LENGTH_SHORT).show()
                    1 -> addPrivateTab()
                    2 -> Toast.makeText(this, "Uzantı desteğini sonraki adımda ele alacağız.", Toast.LENGTH_SHORT).show()
                    3 -> web.reload()
                    4 -> startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    5 -> {
                        addressBarAtBottom = !addressBarAtBottom
                        prefs.edit().putBoolean("address_bottom", addressBarAtBottom).apply()
                        recreate()
                    }
                }
            }
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.black)
            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<android.widget.TextView>(titleId)?.setTextColor(Color.WHITE)
            val listId = resources.getIdentifier("select_dialog_listview", "id", "android")
            dialog.findViewById<android.widget.ListView>(listId)?.apply {
                setBackgroundColor(Color.BLACK)
                for (i in 0 until childCount) {
                    (getChildAt(i) as? android.widget.TextView)?.setTextColor(Color.WHITE)
                }
            }
        }
        dialog.show()
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
