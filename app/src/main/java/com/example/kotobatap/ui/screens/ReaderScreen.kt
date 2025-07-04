package com.example.kotobatap.ui.screens

import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotobatap.R
import com.example.kotobatap.ui.components.AppHeader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.min

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
                            super.onPageFinished(view, url)
                            try {
                                val inputStream = context.assets.open("js/highlightWords.js")
                                val reader = inputStream.bufferedReader()
                                val jsCode = reader.use { it.readText() }
                                view?.evaluateJavascript(jsCode, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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

                val (meanings, hiragana) = parseMeanings(json)
                showWordDetailsDialog(word, meanings, hiragana)
            } catch (e: Exception) {
                showAlert("Error: ${e.message}")
            }
        }.start()
    }

    private fun parseMeanings(json: JSONObject): Pair<List<String>, String?> {
        val meanings = mutableListOf<String>()
        var hiragana: String? = null

        try {
            val data = json.optJSONArray("data") ?: return Pair(meanings, hiragana)

            if (data.length() > 0) {
                val entry = data.getJSONObject(0)

                val japanese = entry.optJSONArray("japanese")
                if (japanese != null && japanese.length() > 0) {
                    hiragana = japanese.getJSONObject(0).optString("reading", null)
                }

                val senses = entry.optJSONArray("senses")
                if (senses != null && senses.length() > 0) {
                    val sense = senses.getJSONObject(0)

                    val englishDefenitions = sense.optJSONArray("english_definitions")
                    if (englishDefenitions != null) {
                        for (k in 0 until min(englishDefenitions.length(), 5)) {
                            englishDefenitions.optString(k)?.let {
                                meanings.add(it)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(meanings, hiragana)
    }

    private fun showWordDetailsDialog(word: String, meanings: List<String>, hiragana: String?) {
        Handler(Looper.getMainLooper()).post {
            val builder = AlertDialog.Builder(context)
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_word_details, null)

            builder.setView(dialogView)

            val tvWord: TextView = dialogView.findViewById(R.id.tvWord)
            val tvMeanings: TextView = dialogView.findViewById(R.id.tvMeanings)
            val tvHiragana: TextView = dialogView.findViewById(R.id.tvHiragana)
            val closeButton: Button = dialogView.findViewById(R.id.closeButton)

            tvWord.text = word
            tvHiragana.text = hiragana ?: "No reading available"
            tvMeanings.text = if (meanings.isNotEmpty()) {
                "• " + meanings.joinToString("\n• ")
            } else {
                "No definitions found"
            }

            val dialog = builder.create()

            val displayMetrics = context.resources.displayMetrics
            val maxWidth = (displayMetrics.widthPixels * 0.9).toInt()
            dialog.window?.setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun showAlert(message: String) {
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
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