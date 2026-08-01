package com.flowreader.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.util.LanTransferServer

/**
 * LAN transfer dialog (v55): serve a generated backup over the local network or receive one
 * from a peer URL. Offline-first — everything stays inside the WiFi.
 */
@Composable
fun LanTransferDialog(
    serverUrl: String?,
    message: String?,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onImportUrl: (String) -> Unit,
    onMessageShown: () -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(message) {
        if (message != null) onMessageShown()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_lan_transfer)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_lan_transfer_hint),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(FlowSpacing.md))

                if (serverUrl == null) {
                    OutlinedButton(
                        onClick = onStartServer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_lan_serve))
                    }
                } else {
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = FlowSpacing.sm)
                    )
                    androidx.compose.foundation.layout.Row {
                        OutlinedButton(onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(serverUrl))
                        }) {
                            androidx.compose.material3.Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp)
                            )
                            Spacer(modifier = Modifier.width(FlowSpacing.sm))
                            Text(stringResource(R.string.settings_lan_copy))
                        }
                        OutlinedButton(onClick = onStopServer) {
                            Text(stringResource(R.string.settings_lan_stop))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FlowSpacing.md))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(stringResource(R.string.settings_lan_receive_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                OutlinedButton(
                    onClick = { onImportUrl(urlInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_lan_receive))
                }

                message?.let {
                    Spacer(modifier = Modifier.height(FlowSpacing.sm))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(FlowSpacing.sm))
                Text(
                    text = stringResource(
                        R.string.settings_lan_ip_hint,
                        LanTransferServer.localIpv4Address() ?: "未知"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
