package com.flowreader.app.domain.model

/**
 * 转盘选项
 */
data class WheelItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val colorValue: Long,
    val weight: Float = 1f // 权重，用于影响概率
) {
    companion object {
        fun defaultItems(): List<WheelItem> = listOf(
            WheelItem(label = "阅读 15 分钟", colorValue = 0xFF4CAF50),
            WheelItem(label = "阅读 30 分钟", colorValue = 0xFF2196F3),
            WheelItem(label = "休息 5 分钟", colorValue = 0xFFFF9800),
            WheelItem(label = "做笔记", colorValue = 0xFF9C27B0),
            WheelItem(label = "复习书签", colorValue = 0xFFE91E63),
            WheelItem(label = "探索新书", colorValue = 0xFF00BCD4)
        )
    }
}

/**
 * 转盘配置
 */
data class WheelConfig(
    val items: List<WheelItem> = WheelItem.defaultItems(),
    val title: String = "阅读决策转盘",
    val spinDuration: Long = 4000L // 旋转持续时间（毫秒）
)

/**
 * 转盘结果
 */
data class WheelResult(
    val item: WheelItem,
    val index: Int
)
