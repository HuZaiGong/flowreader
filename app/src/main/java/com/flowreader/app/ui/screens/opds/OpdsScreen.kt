package com.flowreader.app.ui.screens.opds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.FlowScaffold
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.component.FlowTopBar
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.util.OpdsEntry

/**
 * Browse and import from an OPDS catalog running on the local network.
 *
 * The screen states the constraint out loud (`opds_notice`) because "this reader talks to a
 * server" is exactly the kind of behaviour an offline-first app must not do quietly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsScreen(
    onBackClick: () -> Unit,
    viewModel: OpdsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val error = uiState.error
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val imported = uiState.importedTitle
    val importedMessage = imported?.let { stringResource(R.string.opds_imported, it) }
    LaunchedEffect(imported) {
        if (importedMessage != null) {
            snackbarHostState.showSnackbar(importedMessage)
            viewModel.clearImported()
        }
    }

    FlowScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.opds_title),
                subtitle = uiState.feed?.title?.takeIf { it.isNotBlank() },
                onNavigateUp = {
                    if (uiState.breadcrumbs.size > 1) viewModel.goBack() else onBackClick()
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.url,
                        onValueChange = { viewModel.updateUrl(it) },
                        label = { Text(stringResource(R.string.opds_url_label)) },
                        placeholder = { Text(stringResource(R.string.opds_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.connect() },
                        enabled = uiState.url.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.padding(start = FlowSpacing.sm)
                    ) {
                        Text(stringResource(R.string.opds_connect))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = FlowSpacing.xs)
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = FlowSpacing.xs)
                    )
                    Text(
                        text = stringResource(R.string.opds_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.downloadingTitle != null) {
                Column(modifier = Modifier.padding(horizontal = FlowSpacing.lg)) {
                    Text(
                        text = stringResource(R.string.opds_downloading, uiState.downloadingTitle.orEmpty()),
                        style = MaterialTheme.typography.labelMedium
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            HorizontalDivider()

            FlowStateHost(
                isLoading = uiState.isLoading,
                isEmpty = uiState.feed?.entries.isNullOrEmpty(),
                error = null,
                modifier = Modifier.fillMaxSize(),
                emptyTitle = stringResource(R.string.opds_empty),
                emptyIcon = Icons.Default.Folder
            ) {
                val entries = uiState.feed?.entries.orEmpty()
                LazyColumn(contentPadding = PaddingValues(vertical = FlowSpacing.sm)) {
                    items(entries, key = { "${it.title}-${it.acquisitionUrl ?: it.navigationUrl}" }) { entry ->
                        OpdsEntryRow(
                            entry = entry,
                            onOpen = { viewModel.openEntry(entry) },
                            onDownload = { viewModel.download(entry) }
                        )
                    }
                    if (uiState.feed?.nextUrl != null) {
                        item(key = "next_page") {
                            TextButton(
                                onClick = { viewModel.loadNextPage() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FlowSpacing.md)
                            ) {
                                Text(stringResource(R.string.opds_next_page))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpdsEntryRow(entry: OpdsEntry, onOpen: () -> Unit, onDownload: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FlowSpacing.lg, vertical = FlowSpacing.xs)
    ) {
        ListItem(
            headlineContent = { Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(FlowSpacing.xs)) {
                    if (entry.author.isNotBlank()) {
                        Text(entry.author, style = MaterialTheme.typography.bodySmall)
                    }
                    if (entry.summary.isNotBlank()) {
                        Text(
                            entry.summary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    imageVector = if (entry.isNavigation) Icons.Default.Folder else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null
                )
            },
            trailingContent = {
                when {
                    entry.acquisitionUrl != null -> IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.opds_download))
                    }

                    entry.navigationUrl != null -> IconButton(onClick = onOpen) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.opds_open)
                        )
                    }

                    else -> Unit
                }
            }
        )
    }
}
