package com.hjw.qbremote.ui

import java.net.URI

data class HostInputHints(
    val useHttps: Boolean?,
    val port: Int?,
)

fun parseHostInputHints(rawInput: String): HostInputHints? {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return null

    if (trimmed.contains("://")) {
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        return HostInputHints(
            useHttps = scheme == "https",
            port = uri.port.takeIf { it in 1..65535 },
        )
    }

    val uri = runCatching { URI("http://$trimmed") }.getOrNull() ?: return null
    val explicitPort = uri.port.takeIf { it in 1..65535 } ?: return null
    return HostInputHints(
        useHttps = null,
        port = explicitPort,
    )
}
