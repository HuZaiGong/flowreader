package com.flowreader.app.ui

import androidx.compose.runtime.Composable

@Composable
fun FlowReaderRoot(initialImportUri: android.net.Uri? = null) {
    FlowReaderNavHost(initialImportUri = initialImportUri)
}
