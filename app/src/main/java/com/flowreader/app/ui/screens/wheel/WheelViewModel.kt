package com.flowreader.app.ui.screens.wheel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 转盘 UI 状态
 */
data class WheelUiState(
    val items: List<com.flowreader.app.domain.model.WheelItem> = com.flowreader.app.domain.model.WheelItem.defaultItems(),
    val isSpinning: Boolean = false,
    val result: com.flowreader.app.domain.model.WheelResult? = null,
    val rotationAngle: Float = 0f,
    val showResultDialog: Boolean = false,
    val editingMode: Boolean = false,
    val newItemLabel: String = ""
)

@HiltViewModel
class WheelViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WheelUiState())
    val uiState: StateFlow<WheelUiState> = _uiState.asStateFlow()

    /**
     * 旋转转盘
     */
    suspend fun spin() {
        val state = _uiState.value
        if (state.isSpinning || state.items.isEmpty()) return

        _uiState.update { it.copy(isSpinning = true, result = null, showResultDialog = false) }

        // 随机选择结果
        val selectedIndex = selectWeightedRandom(state.items)
        val selectedItem = state.items[selectedIndex]

        // 计算旋转角度：旋转到选中项的位置
        val sliceAngle = 360f / state.items.size
        val targetAngle = 360f - (selectedIndex * sliceAngle + sliceAngle / 2)
        val totalRotation = 360f * 5 + targetAngle // 旋转 5 圈 + 目标角度

        // 动画旋转
        val steps = 60
        val stepDuration = 4000L / steps
        for (i in 1..steps) {
            val progress = i.toFloat() / steps
            val easedProgress = easeOutCubic(progress)
            _uiState.update {
                it.copy(rotationAngle = totalRotation * easedProgress)
            }
            delay(stepDuration)
        }

        _uiState.update {
            it.copy(
                isSpinning = false,
                result = com.flowreader.app.domain.model.WheelResult(selectedItem, selectedIndex),
                showResultDialog = true,
                rotationAngle = totalRotation % 360f
            )
        }
    }

    /**
     * 加权随机选择
     */
    private fun selectWeightedRandom(items: List<com.flowreader.app.domain.model.WheelItem>): Int {
        val totalWeight = items.sumOf { it.weight.toDouble() }
        var random = Math.random() * totalWeight
        for ((index, item) in items.withIndex()) {
            random -= item.weight
            if (random <= 0) return index
        }
        return items.size - 1
    }

    /**
     * 缓动函数（减速）
     */
    private fun easeOutCubic(t: Float): Float {
        val oneMinusT = 1 - t
        return 1 - (oneMinusT * oneMinusT * oneMinusT)
    }

    /**
     * 关闭结果对话框
     */
    fun dismissResult() {
        _uiState.update { it.copy(showResultDialog = false) }
    }

    /**
     * 切换编辑模式
     */
    fun toggleEditMode() {
        _uiState.update { it.copy(editingMode = !it.editingMode) }
    }

    /**
     * 添加新选项
     */
    fun addItem() {
        val state = _uiState.value
        if (state.newItemLabel.isBlank()) return

        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF4CAF50),
            androidx.compose.ui.graphics.Color(0xFF2196F3),
            androidx.compose.ui.graphics.Color(0xFFFF9800),
            androidx.compose.ui.graphics.Color(0xFF9C27B0),
            androidx.compose.ui.graphics.Color(0xFFE91E63),
            androidx.compose.ui.graphics.Color(0xFF00BCD4),
            androidx.compose.ui.graphics.Color(0xFFFF5722),
            androidx.compose.ui.graphics.Color(0xFF607D8B)
        )
        val color = colors[state.items.size % colors.size]

        val newItem = com.flowreader.app.domain.model.WheelItem(
            label = state.newItemLabel,
            color = color
        )
        _uiState.update {
            it.copy(
                items = it.items + newItem,
                newItemLabel = ""
            )
        }
    }

    /**
     * 删除选项
     */
    fun removeItem(id: String) {
        _uiState.update {
            it.copy(items = it.items.filter { item -> item.id != id })
        }
    }

    /**
     * 更新选项标签
     */
    fun updateItemLabel(id: String, label: String) {
        _uiState.update {
            it.copy(
                items = it.items.map { item ->
                    if (item.id == id) item.copy(label = label) else item
                }
            )
        }
    }

    /**
     * 重置为默认选项
     */
    fun resetToDefaults() {
        _uiState.update {
            it.copy(items = com.flowreader.app.domain.model.WheelItem.defaultItems())
        }
    }

    /**
     * 更新新选项输入
     */
    fun updateNewItemInput(label: String) {
        _uiState.update { it.copy(newItemLabel = label) }
    }
}
