package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.AnnotationDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.dao.UserDao
import com.flowreader.app.data.local.entity.UserEntity
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.AnnotationEntity
import com.flowreader.app.domain.model.User
import com.flowreader.app.domain.model.SyncData
import com.flowreader.app.domain.model.ReadingProgress
import com.flowreader.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val annotationDao: AnnotationDao,
    private val chapterDao: ChapterDao
) : UserRepository {

    private var currentUserId: Long? = null

    override suspend fun getUserById(id: Long): User? {
        return userDao.getUserById(id)?.toDomain()
    }

    override suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)?.toDomain()
    }

    override suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)?.toDomain()
    }

    override suspend fun register(
        username: String,
        password: String,
        email: String?,
        phone: String?
    ): Result<User> {
        return try {
            // Check if username already exists
            if (userDao.getUserByUsername(username) != null) {
                return Result.failure(Exception("用户名已存在"))
            }

            // Check if email already exists
            email?.let {
                if (userDao.getUserByEmail(it) != null) {
                    return Result.failure(Exception("邮箱已被注册"))
                }
            }

            val passwordHash = hashPassword(password)
            val syncToken = generateSyncToken()

            val userEntity = UserEntity(
                username = username,
                email = email,
                phone = phone,
                passwordHash = passwordHash,
                syncToken = syncToken,
                syncEnabled = false
            )

            val userId = userDao.insertUser(userEntity)
            currentUserId = userId

            Result.success(
                User(
                    id = userId,
                    username = username,
                    email = email,
                    phone = phone,
                    syncEnabled = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            val userEntity = userDao.getUserByUsername(username)
                ?: return Result.failure(Exception("用户不存在"))

            val passwordHash = hashPassword(password)
            if (userEntity.passwordHash != passwordHash) {
                return Result.failure(Exception("密码错误"))
            }

            userDao.updateLastLoginTime(userEntity.id)
            currentUserId = userEntity.id

            Result.success(userEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        currentUserId = null
    }

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun updateSyncSettings(enabled: Boolean, token: String?) {
        currentUserId?.let { userId ->
            userDao.updateSyncSettings(userId, token, enabled)
        }
    }

    override suspend fun exportSyncData(): SyncData {
        // This would normally sync to cloud, but for MVP we'll export local data
        return SyncData()
    }

    override suspend fun importSyncData(syncData: SyncData): Result<Unit> {
        return try {
            // This would normally import from cloud
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSyncToken(): String {
        return UUID.randomUUID().toString()
    }
}
