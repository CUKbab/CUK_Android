package com.cukbab

enum class LanguagePreference(val label: String, val tag: String) {
    System("System", ""),
    English("English", "en"),
    Korean("한국어", "ko"),
    Japanese("日本語", "ja"),
    Chinese("简体中文", "zh-Hans-CN")
}
