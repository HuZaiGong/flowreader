package com.flowreader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flowreader.app.ui.FlowReaderRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * URI handed in by another app ("open with FlowReader"). Consumed once so a configuration
     * change cannot re-import the same file. The import pipeline is fully capped (BookParser
     * read limits, zip-slip guards), so untrusted shares are safe to accept.
     */
    private var pendingImportUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingImportUri = resolveImportUri(intent)
        intent.data = null
        intent.removeExtra(Intent.EXTRA_STREAM)
        enableEdgeToEdge()
        setContent {
            FlowReaderRoot(initialImportUri = pendingImportUri)
        }
    }

    private fun resolveImportUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return intent.data
            ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
    }
}
