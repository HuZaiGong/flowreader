package com.flowreader.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.flowreader.app.core.designsystem.theme.FlowTheme
import com.flowreader.core.R

/**
 * The standard screen header.
 *
 * Every screen hand-rolled its own `TopAppBar` before v53, which is why the back arrow carried
 * three different content descriptions and two of them were `null`. Titles ellipsize at one line
 * so a long book name can never push the actions off screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    navigateUpDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val upDescription = navigateUpDescription ?: stringResource(R.string.flow_action_navigate_up)
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = upDescription)
                }
            }
        },
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * The contextual header shown while a multi-select is active.
 *
 * Uses the primary container so the mode change is unmistakable — a selection bar that looks like
 * the normal bar is how users end up deleting the wrong books.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowSelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.flow_selection_count, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.flow_action_close_selection))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@FlowComponentPreviews
@Composable
private fun FlowTopBarPreview() {
    FlowTheme {
        Column {
            FlowTopBar(title = "心流阅读", subtitle = "42 本书")
            FlowTopBar(title = "一个很长很长很长很长很长很长的书名详情页", onNavigateUp = {})
            FlowSelectionTopBar(selectedCount = 3, onClose = {})
        }
    }
}
