package com.flowreader.app.ui.screens.wheel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val spinTargetAngle: Float = 0f,
    val spinRequestId: Long = 0L,
    val showResultDialog: Boolean = false,
    val editingMode: Boolean = false,
    val newItemLabel: String = "",
    val error: String? = null
)

@HiltViewModel
class WheelViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WheelUiState())
    val uiState: StateFlow<WheelUiState> = _uiState.asStateFlow()

    fun spin() {
        val state = _uiState.value
        if (state.isSpinning) return
        if (state.items.isEmpty()) {
            _uiState.update { it.copy(error = "请至少添加一个选项后再旋转") }
            return
        }

        val currentAngle = state.rotationAngle
        val selectedIndex = selectWeightedRandom(state.items)
        val selectedItem = state.items[selectedIndex]

        val sliceAngle = 360f / state.items.size
        val targetAngle = 360f - (selectedIndex * sliceAngle + sliceAngle / 2)
        val additionalAngle = ((targetAngle - currentAngle) % 360 + 360) % 360
        val totalRotation = 360f * 5 + additionalAngle

        _uiState.update {
            it.copy(
                isSpinning = true,
                result = com.flowreader.app.domain.model.WheelResult(selectedItem, selectedIndex),
                rotationAngle = currentAngle,
                spinTargetAngle = currentAngle + totalRotation,
                spinRequestId = it.spinRequestId + 1,
                showResultDialog = false,
                error = null
            )
        }
    }

    fun finishSpin() {
        _uiState.update {
            it.copy(
                isSpinning = false,
                showResultDialog = true,
                rotationAngle = it.spinTargetAngle.mod(360f)
            )
        }
    }

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
     * 关闭结果对话框
     */
    fun dismissResult() {
        _uiState.update { it.copy(showResultDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
            0xFF4CAF50,
            0xFF2196F3,
            0xFFFF9800,
            0xFF9C27B0,
            0xFFE91E63,
            0xFF00BCD4,
            0xFFFF5722,
            0xFF607D8B
        )
        val color = colors[state.items.size % colors.size]

        val newItem = com.flowreader.app.domain.model.WheelItem(
            label = state.newItemLabel,
            colorValue = color
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
