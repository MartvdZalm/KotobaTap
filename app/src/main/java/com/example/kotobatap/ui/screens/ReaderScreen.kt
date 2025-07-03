package com.example.kotobatap.ui.screens

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotobatap.ui.components.AppHeader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

@Composable
fun ReaderScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppHeader(
                title = "Reading Mode",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true

                    val jishoInterface = JishoInterface(context, this)
                    addJavascriptInterface(jishoInterface, "Android")
                    webChromeClient = JishoChromeClient(jishoInterface)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(
                                """
                                    function highlightJapaneseWords() {
                                        const style = document.createElement('style');
                                        style.textContent = `
                                            .japanese-word {
                                                background-color: #FFF59D;
                                                border-radius: 3px;
                                                padding: 0 2px;
                                                margin: 0 1px;
                                                box-shadow: 0 0 2px rgba(0,0,0,0.2);
                                                transition: background-color 0.3s;
                                            }
                                            .japanese-word:hover {
                                                background-color: #FFEE58;
                                            }
                                        `;
                                        document.head.appendChild(style);
                                    
                                        function tokenizeJapanese(text) {
                                            if (window.Intl && Intl.Segmenter) {
                                                const segmenter = new Intl.Segmenter('ja-JP', { granularity: 'word' });
                                                const segments = segmenter.segment(text);
                                                return Array.from(segments).map(seg => seg.segment);
                                            } else if (window.Intl && Intl.v8BreakIterator) {
                                                const it = Intl.v8BreakIterator(['ja-JP'], {type: 'word'});
                                                it.adoptText(text);
                                                const words = [];
                                                let cur = 0, prev = 0;
                                                while (cur < text.length) {
                                                    prev = cur;
                                                    cur = it.next();
                                                    words.push(text.substring(prev, cur));
                                                }
                                                return words;
                                            }
                                            return [text];
                                        }
                                    
                                        function isJapanese(text) {
                                            return /[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]/.test(text);
                                        }
                                    
                                        function walkTextNodes(node, callback) {
                                            if (node.nodeType === Node.TEXT_NODE) {
                                                callback(node);
                                            } else {
                                                for (let child of node.childNodes) {
                                                    walkTextNodes(child, callback);
                                                }
                                            }
                                        }
                                    
                                        walkTextNodes(document.body, (textNode) => {
                                            const text = textNode.nodeValue;
                                            if (!isJapanese(text)) return;
                                    
                                            const words = tokenizeJapanese(text);
                                            const fragment = document.createDocumentFragment();
                                    
                                            words.forEach(word => {
                                                if (isJapanese(word)) {
                                                    const span = document.createElement('span');
                                                    span.className = 'japanese-word';
                                                    span.textContent = word;
                                                    span.addEventListener('click', function(event) {
                                                        event.stopPropagation();
                                                        prompt('JISHO_LOOKUP',word)
                                                    });
                                                    fragment.appendChild(span);
                                                } else {
                                                    fragment.appendChild(document.createTextNode(word));
                                                }
                                            });
                                            textNode.parentNode.replaceChild(fragment, textNode);
                                        });
                                    }
                                    
                                    highlightJapaneseWords();
                                """.trimIndent(), null
                            )
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = modifier.padding(padding)
        )
    }
}

class JishoInterface(
    private val context: Context,
    private val webView: WebView
) {
    private val client = OkHttpClient()

    @JavascriptInterface
    fun lookupWord(word: String) {
        Thread {
            try {
                val url = "https://jisho.org/api/v1/search/words?keyword=${
                    URLEncoder.encode(
                        word,
                        "UTF-8"
                    )
                }"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                val meanings = parseMeanings(json)
                showAlert("$word\n\n${meanings.joinToString("\n• ")}")
            } catch (e: Exception) {
                showAlert("Error: ${e.message}")
            }
        }.start()
    }

    private fun parseMeanings(json: JSONObject): List<String> {
        val meanings = mutableListOf<String>()
        val data = json.optJSONArray("data") ?: return meanings

        for (i in 0 until data.length()) {
            val entry = data.getJSONObject(i)
            val senses = entry.getJSONArray("senses")

            for (j in 0 until senses.length()) {
                senses.getJSONObject(j).getJSONArray("english_definitions").let { defs ->
                    for (k in 0 until defs.length()) {
                        meanings.add(defs.getString(k))
                    }
                }
            }
        }

        return meanings.take(5)
    }

    private fun showAlert(message: String) {
        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript("alert(`${message.replace("'", "\\'")}`);", null)
        }
    }
}

private class JishoChromeClient(
    private val jishoInterface: JishoInterface
) : WebChromeClient() {
    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String,
        defaultValue: String,
        result: JsPromptResult
    ): Boolean {
        if (message == "JISHO_LOOKUP") {
            jishoInterface.lookupWord(defaultValue)
            result.confirm()
            return true
        }
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }
}