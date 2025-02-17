package com.toasterofbread.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun isWebViewLoginSupported(): Boolean

interface WebViewRequest {
    val url: String
    val isRedirect: Boolean
    val method: String
    val requestHeaders: Map<String, String>
}

@Composable
expect fun WebViewLogin(
    initial_url: String,
    modifier: Modifier = Modifier,
    onClosed: () -> Unit,
    shouldShowPage: (url: String) -> Boolean,
    loading_message: String? = null,
    onRequestIntercepted: (WebViewRequest, openUrl: (String) -> Unit, getCookie: (String) -> String) -> Unit
)
