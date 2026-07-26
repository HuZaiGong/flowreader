package com.flowreader.app.ui.screens.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.ui.screens.wheel.components.WheelPointer
import com.flowreader.app.ui.screens.wheel.components.WheelSpinner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(
    viewModel: WheelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val error by remember { derivedStateOf { uiState.error } }
    val result by remember { derivedStateOf { uiState.result } }
    val rotation = remember { Animatable(uiState.rotationAngle) }

    LaunchedEffect(uiState.spinRequestId) {
        if (uiState.isSpinning) {
            rotation.snapTo(uiState.rotationAngle)
            rotation.animateTo(
                targetValue = uiState.spinTargetAngle,
                animationSpec = tween(durationMillis = 4_000, easing = FastOutSlowInEasing)
            )
            rotation.snapTo(rotation.value.mod(360f))
            viewModel.finishSpin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("决策转盘") },
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WheelPointer(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    WheelSpinner(
                        items = uiState.items,
                        rotationAngle = if (uiState.isSpinning) rotation.value else uiState.rotationAngle,
                        size = 280.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.spin() },
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

            val currentError = error
            if (currentError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentError,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("确定")
                        }
                    }
                }
            }

            val currentResult = result
            if (currentResult != null && !uiState.isSpinning) {
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
                            text = currentResult.item.label,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(currentResult.item.colorValue)
                        )
                    }
                }
            }

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

        val dialogResult = result
        if (uiState.showResultDialog && dialogResult != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("🎯 转盘结果") },
                text = {
                    Text(
                        text = dialogResult.item.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(dialogResult.item.colorValue)
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
                items(items, key = { it.id }) { item ->
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
                                    drawCircle(color = Color(item.colorValue))
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
