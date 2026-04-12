package com.flowreader.app.ui.screens.wheel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.ui.screens.wheel.components.WheelPointer
import com.flowreader.app.ui.screens.wheel.components.WheelSpinner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(
    onBackClick: () -> Unit = {},
    viewModel: WheelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("决策转盘") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleEditMode() }) {
                        Icon(
                            if (uiState.editingMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (uiState.editingMode) "完成" else "编辑"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 转盘区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 指针
                    WheelPointer(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // 转盘
                    WheelSpinner(
                        items = uiState.items,
                        rotationAngle = uiState.rotationAngle,
                        size = 280.dp
                    )
                }
            }

            // 旋转按钮
            Spacer(modifier = Modifier.height(16.dp))
            var triggerSpin by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!uiState.isSpinning) {
                        triggerSpin = true
                    }
                },
                enabled = !uiState.isSpinning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Default.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isSpinning) "旋转中..." else "开始旋转",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            LaunchedEffect(triggerSpin) {
                if (triggerSpin) {
                    viewModel.spin()
                    triggerSpin = false
                }
            }

            // 结果展示
            val result = uiState.result
            if (result != null && !uiState.isSpinning) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 结果",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.item.label,
                            style = MaterialTheme.typography.headlineMedium,
                            color = result.item.color
                        )
                    }
                }
            }

            // 编辑模式
            if (uiState.editingMode) {
                Spacer(modifier = Modifier.height(16.dp))
                EditPanel(
                    items = uiState.items,
                    newItemLabel = uiState.newItemLabel,
                    onNewItemLabelChange = { viewModel.updateNewItemInput(it) },
                    onAddItem = { viewModel.addItem() },
                    onRemoveItem = { viewModel.removeItem(it) },
                    onResetDefaults = { viewModel.resetToDefaults() }
                )
            }
        }

        // 结果对话框
        val showDialogResult = uiState.showResultDialog && uiState.result != null
        if (showDialogResult) {
            val result = uiState.result!!
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("🎯 转盘结果") },
                text = {
                    Text(
                        text = result.item.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = result.item.color
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissResult() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@Composable
private fun EditPanel(
    items: List<com.flowreader.app.domain.model.WheelItem>,
    newItemLabel: String,
    onNewItemLabelChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onResetDefaults: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "编辑选项",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 添加新选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newItemLabel,
                    onValueChange = onNewItemLabelChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("新选项") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onAddItem() }
                    )
                )
                Button(onClick = onAddItem) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 选项列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    drawCircle(color = item.color)
                                }
                            }
                            Text(item.label)
                        }
                        IconButton(
                            onClick = { onRemoveItem(item.id) },
                            enabled = items.size > 2
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 重置按钮
            OutlinedButton(
                onClick = onResetDefaults,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重置为默认选项")
            }
        }
    }
}
