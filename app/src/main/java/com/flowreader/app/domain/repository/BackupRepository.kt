package com.flowreader.app.domain.repository

import android.net.Uri
import com.flowreader.app.data.repository.ImportResult

/**
 * 备份和恢复仓库接口
 * 负责书籍、书签、阅读进度等数据的导出和导入
 */
interface BackupRepository {
    /**
     * 导出数据到指定 URI
     * @param uri 目标文件 URI
     * @return 导出结果
     */
    suspend fun exportData(uri: Uri): Result<Unit>

    /**
     * 从指定 URI 导入数据
     * @param uri 源文件 URI
     * @return 导入结果，包含导入的书籍和书签数量
     */
    suspend fun importData(uri: Uri): Result<ImportResult>

    /**
     * 导出单本书的阅读进度
     * @param bookId 书籍 ID
     * @return JSON 格式的阅读进度数据
     */
    suspend fun exportReadingProgress(bookId: Long): Result<String>
}
