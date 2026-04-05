package com.flowreader.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.domain.model.User

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账户") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.user == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "登录以同步数据",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "登录后您的阅读进度、书签和设置将在云端备份",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.navigateToAuth() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("登录 / 注册")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.user?.displayName ?: "用户",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = uiState.user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsSection(title = "同步设置") {
                    SettingsSwitch(
                        icon = Icons.Default.CloudSync,
                        title = "启用云端同步",
                        subtitle = "自动同步阅读进度、书签和设置",
                        checked = uiState.syncEnabled,
                        onCheckedChange = { viewModel.toggleSync(it) }
                    )

                    if (uiState.syncEnabled) {
                        SettingsItem(
                            icon = Icons.Default.Sync,
                            title = "立即同步",
                            subtitle = if (uiState.isSyncing) "同步中..." else "点击手动同步",
                            onClick = { viewModel.syncNow() }
                        )

                        if (uiState.lastSyncTime != null) {
                            SettingsItem(
                                icon = Icons.Default.Schedule,
                                title = "上次同步",
                                subtitle = uiState.lastSyncTime,
                                onClick = {}
                            )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) {{ Text(subtitle) }} else null,
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

                SettingsSection(title = "账户操作") {
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = "退出登录",
                        subtitle = "退出当前账户",
                        onClick = { viewModel.signOut() }
                    )

                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "删除账户",
                        subtitle = "永久删除账户和数据",
                        onClick = { viewModel.showDeleteConfirmation() }
                    )
                }

                if (uiState.showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.hideDeleteConfirmation() },
                        title = { Text("确认删除") },
                        text = { Text("删除账户将清除所有云端数据，此操作不可恢复。") },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.deleteAccount() }
                            ) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) {{ Text(subtitle) }} else null,
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
private fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable { onClick() }
    )
}

@Composable
private fun Modifier.clickable(block: () -> Unit): Modifier {
    return androidx.compose.foundation.clickable { block() }
}
