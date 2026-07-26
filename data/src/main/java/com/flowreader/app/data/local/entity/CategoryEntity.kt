package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
) {
    fun toDomain(bookCount: Int = 0): Category = Category(
        id = id,
        name = name,
        bookCount = bookCount
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name
        )
    }
}
