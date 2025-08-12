package com.example.kotobatap.ui.screens

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotobatap.R
import com.example.kotobatap.managers.DataStoreManager
import com.example.kotobatap.ui.components.appHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.min

@Serializable
data class HighlightCorrection(
    val originalText: String,
    val correctedText: String,
    val context: String = "",
)

@Serializable
data class WordData(
    val word: String,
    val type: String,
    val context: String,
)

@SuppressLint("ComposableNaming")
@Composable
fun readerScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val json = Json { ignoreUnknownKeys = true }

    val autoHighlight by DataStoreManager.getBooleanValue(context, "auto_highlight", true).collectAsState(initial = true)
    val fontSize by DataStoreManager.getFloatValue(context, "font_size", 16f).collectAsState(initial = 16f)
    val lineSpacing by DataStoreManager.getFloatValue(context, "line_spacing", 1.2f).collectAsState(initial = 1.2f)
    val highlightColor by DataStoreManager.getStringValue(context, "highlight_color", "#FFF59D").collectAsState(initial = "#FFF59D")

    Scaffold(
        topBar = {
            appHeader(
                title = "Reading Mode",
                showBackButton = true,
                onBackClick = onBack,
            )
        },
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    val jishoInterface = JishoInterface(context, this, scope, json)
                    addJavascriptInterface(jishoInterface, "Android")
                    webChromeClient = JishoChromeClient(jishoInterface)

                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                super.onPageFinished(view, url)
                                try {
                                    val inputStream = context.assets.open("js/highlightWords.js")
                                    val reader = inputStream.bufferedReader()
                                    val jsCode = reader.use { it.readText() }

                                    view?.evaluateJavascript(jsCode, null)

                                    scope.launch {
                                        val currentCorrections = getHighlightCorrections(context, json)
                                        val correctionsJson = json.encodeToString(currentCorrections)

                                        view?.evaluateJavascript("setHighlightCorrections('$correctionsJson')", null)

                                        if (autoHighlight) {
                                            view?.evaluateJavascript("highlightJapaneseWords()", null)
                                        } else {
                                            view?.evaluateJavascript("cleanupJapaneseHighlighting()", null)
                                        }

                                        view?.evaluateJavascript("changeFontSize($fontSize)", null)
                                        view?.evaluateJavascript("changeLineSpacing($lineSpacing)", null)
                                        view?.evaluateJavascript("changeHighlightingColor('$highlightColor')", null)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    loadUrl(url)
                }
            },
            modifier = modifier.padding(padding),
        )
    }
}

private suspend fun getHighlightCorrections(
    context: Context,
    json: Json,
): List<HighlightCorrection> {
    return try {
        val correctionsJson = DataStoreManager.getStringValue(context, "highlight_corrections", "[]").first()
        json.decodeFromString<List<HighlightCorrection>>(correctionsJson)
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun saveHighlightCorrections(
    context: Context,
    json: Json,
    corrections: List<HighlightCorrection>,
) {
    val correctionsJson = json.encodeToString(corrections)
    DataStoreManager.putValue(context, "highlight_corrections", correctionsJson)
}

class JishoInterface(
    private val context: Context,
    private val webView: WebView,
    private val scope: CoroutineScope,
    private val json: Json,
) {
    private val client = OkHttpClient()

    @JavascriptInterface
    fun lookupWord(wordDataJson: String) {
        Thread {
            try {
                val wordData = parseWordData(wordDataJson)
                val url = "https://jisho.org/api/v1/search/words?keyword=${
                    URLEncoder.encode(
                        wordData.word,
                        "UTF-8",
                    )
                }"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                val (meanings, hiragana, partsOfSpeech) = parseMeanings(json)
                showWordDetailsDialog(wordData, meanings, hiragana, partsOfSpeech)
            } catch (e: Exception) {
                showAlert("Error: ${e.message}")
            }
        }.start()
    }

    private fun parseWordData(jsonString: String): WordData {
        return try {
            val json = JSONObject(jsonString)
            WordData(
                word = json.optString("word", ""),
                type = json.optString("type", "unknown"),
                context = json.optString("context", ""),
            )
        } catch (e: Exception) {
            WordData(word = jsonString, type = "unknown", context = "")
        }
    }

    private fun parseMeanings(json: JSONObject): Triple<List<String>, String?, List<String>> {
        val meanings = mutableListOf<String>()
        val partsOfSpeech = mutableListOf<String>()
        var hiragana: String? = null

        try {
            val data = json.optJSONArray("data") ?: return Triple(meanings, hiragana, partsOfSpeech)

            if (data.length() > 0) {
                val entry = data.getJSONObject(0)

                val japanese = entry.optJSONArray("japanese")
                if (japanese != null && japanese.length() > 0) {
                    val firstReading = japanese.getJSONObject(0)
                    hiragana = firstReading.optString("reading", null)

                    if (hiragana.isNullOrEmpty()) {
                        for (i in 0 until japanese.length()) {
                            val reading = japanese.getJSONObject(i).optString("reading", "")
                            if (reading.isNotEmpty()) {
                                hiragana = reading
                                break
                            }
                        }
                    }
                }

                val senses = entry.optJSONArray("senses")
                if (senses != null && senses.length() > 0) {
                    for (i in 0 until min(senses.length(), 3)) {
                        val sense = senses.getJSONObject(i)

                        val pos = sense.optJSONArray("parts_of_speech")
                        if (pos != null && pos.length() > 0) {
                            for (j in 0 until pos.length()) {
                                val part = pos.optString(j)
                                if (part.isNotEmpty() && !partsOfSpeech.contains(part)) {
                                    partsOfSpeech.add(part)
                                }
                            }
                        }

                        val englishDefinitions = sense.optJSONArray("english_definitions")
                        if (englishDefinitions != null) {
                            val senseMeanings = mutableListOf<String>()
                            for (k in 0 until min(englishDefinitions.length(), 3)) {
                                englishDefinitions.optString(k)?.let {
                                    senseMeanings.add(it)
                                }
                            }
                            if (senseMeanings.isNotEmpty()) {
                                meanings.add(senseMeanings.joinToString(", "))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Triple(meanings, hiragana, partsOfSpeech.take(3))
    }

    private fun showWordDetailsDialog(
        wordData: WordData,
        meanings: List<String>,
        hiragana: String?,
        partsOfSpeech: List<String>,
    ) {
        Handler(Looper.getMainLooper()).post {
            val builder = AlertDialog.Builder(context)
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_word_details, null)

            builder.setView(dialogView)

            val tvWord: TextView = dialogView.findViewById(R.id.tvWord)
            val tvMeanings: TextView = dialogView.findViewById(R.id.tvMeanings)
            val tvHiragana: TextView = dialogView.findViewById(R.id.tvHiragana)
            val tvType: TextView? = dialogView.findViewById(R.id.tvType)
            val tvContext: TextView? = dialogView.findViewById(R.id.tvContext)
            val correctHighlightingButton: Button = dialogView.findViewById(R.id.correctHighlightingButton)
            val closeButton: Button = dialogView.findViewById(R.id.closeButton)

            tvWord.text = wordData.word
            tvHiragana.text = hiragana ?: "No reading available"

            val typeInfo =
                buildString {
                    append("Type: ${getJapaneseTypeDisplayName(wordData.type)}")
                    if (partsOfSpeech.isNotEmpty()) {
                        append("\nParts of Speech: ${partsOfSpeech.joinToString(", ")}")
                    }
                }
            tvType?.text = typeInfo

            if (wordData.context.isNotEmpty() && wordData.context != wordData.word) {
                tvContext?.text = "Context: \"${wordData.context.trim()}\""
                tvContext?.visibility = android.view.View.VISIBLE
            } else {
                tvContext?.visibility = android.view.View.GONE
            }

            tvMeanings.text =
                if (meanings.isNotEmpty()) {
                    meanings.mapIndexed { index, meaning ->
                        "${index + 1}. $meaning"
                    }.joinToString("\n\n")
                } else {
                    "No definitions found"
                }

            val dialog = builder.create()

            val displayMetrics = context.resources.displayMetrics
            val maxWidth = (displayMetrics.widthPixels * 0.9).toInt()
            dialog.window?.setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

            correctHighlightingButton.setOnClickListener {
                dialog.dismiss()
                showCorrectionDialog(wordData.word)
            }

            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun showCorrectionDialog(originalWord: String) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_correct_highlighting, null)
        builder.setView(dialogView)

        val correctedWordInput: EditText = dialogView.findViewById(R.id.correctedWordInput)
        val saveButton: Button = dialogView.findViewById(R.id.saveButton)
        val cancelButton: Button = dialogView.findViewById(R.id.cancelButton)

        correctedWordInput.setText(originalWord)

        val correctionDialog = builder.create()

        saveButton.setOnClickListener {
            val correctedText = correctedWordInput.text.toString().trim()
            if (correctedText.isNotEmpty() && correctedText != originalWord) {
                scope.launch {
                    val currentCorrections = getHighlightCorrections(context, json)
                    val newCorrection =
                        HighlightCorrection(
                            originalText = originalWord,
                            correctedText = correctedText,
                            context = "",
                        )

                    val updatedCorrections =
                        currentCorrections.filterNot {
                            it.originalText == originalWord
                        } + newCorrection

                    saveHighlightCorrections(context, json, updatedCorrections)

                    val correctionsJson = json.encodeToString(updatedCorrections)
                    webView.evaluateJavascript("setHighlightCorrections('$correctionsJson')", null)
                    webView.evaluateJavascript("reHighlightWithCorrections()", null)
                }
            }
            correctionDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            correctionDialog.dismiss()
        }

        correctionDialog.show()
    }

    private fun getJapaneseTypeDisplayName(type: String): String {
        return when (type.lowercase()) {
            "kanji" -> "Kanji (漢字)"
            "hiragana" -> "Hiragana (ひらがな)"
            "katakana" -> "Katakana (カタカナ)"
            "mixed" -> "Mixed"
            else -> "Unknown"
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
    private val jishoInterface: JishoInterface,
) : WebChromeClient() {
    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String,
        defaultValue: String,
        result: JsPromptResult,
    ): Boolean {
        if (message == "JISHO_LOOKUP") {
            jishoInterface.lookupWord(defaultValue)

            result.confirm()
            return true
        }
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }
}
