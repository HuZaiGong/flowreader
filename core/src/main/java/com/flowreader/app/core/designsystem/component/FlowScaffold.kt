package com.flowreader.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.theme.FlowTheme

private val ZERO_PADDING = PaddingValues(0.dp)

/**
 * The standard screen container.
 *
 * Wrapping `Scaffold` buys two things the raw component does not: a snackbar host is always wired
 * (screens used to forget it and swallow their own errors — see the v52 library import fix), and
 * the content padding is applied for you, so no screen can ship content hidden under the bottom
 * bar again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    applyContentPadding: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets
    ) { padding ->
        if (applyContentPadding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                content(ZERO_PADDING)
            }
        } else {
            content(padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@FlowComponentPreviews
@Composable
private fun FlowScaffoldPreview() {
    FlowTheme {
        FlowScaffold(topBar = { FlowTopBar(title = "书架") }) {
            Text("内容区")
        }
    }
}
