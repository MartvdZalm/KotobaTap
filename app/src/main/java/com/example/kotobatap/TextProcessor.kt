package com.example.kotobatap

class TextProcessor {
    external fun highlightKanji(text: String): String

    companion object {
        init {
            System.loadLibrary("textprocessor")
        }
    }
}