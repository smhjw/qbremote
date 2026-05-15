package com.hjw.qbremote.data

import java.util.Locale

internal fun looksLikeHtml(text: String): Boolean {
    val normalized = text.trim().lowercase(Locale.US)
    return normalized.startsWith("<!doctype html") ||
        normalized.startsWith("<html") ||
        normalized.contains("<title>")
}

internal fun looksLikeHtmlPayload(text: String): Boolean = looksLikeHtml(text)

internal fun summarizeResponseText(text: String, maxChars: Int = 160): String {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return "Unexpected response."
    return Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
        .find(trimmed)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: trimmed
            .replace(Regex("\\s+"), " ")
            .take(maxChars)
}

internal fun summarizeQbLoginResponseText(text: String): String =
    summarizeResponseText(text, maxChars = 160)

internal fun looksLikeAuthFailure(text: String): Boolean {
    val normalized = text.lowercase(Locale.US)
    return normalized.contains("unauthorized") ||
        normalized.contains("forbidden") ||
        normalized.contains("auth") ||
        normalized.contains("permission denied")
}
