package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.domain.model.Category
import com.flowreader.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun deleteCategoryById(id: Long) {
        categoryDao.deleteCategoryById(id)
    }
}
