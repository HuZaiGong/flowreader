package com.flowreader.app.data.repository

import com.flowreader.app.domain.model.User
import com.flowreader.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override val isLoggedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser()
                ?: return Result.failure(Exception("登录失败"))
            createUserDocument(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser()
                ?: return Result.failure(Exception("注册失败"))
            createUserDocument(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user?.toUser()
                ?: return Result.failure(Exception("Google登录失败"))
            createUserDocument(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.toUser()
    }

    override suspend fun updateUserProfile(displayName: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("用户未登录"))
            
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            
            user.updateProfile(profileUpdates).await()
            
            firestore.collection("users").document(user.uid)
                .update("displayName", displayName)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("用户未登录"))
            
            firestore.collection("users").document(user.uid)
                .delete()
                .await()
            
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createUserDocument(user: User) {
        try {
            firestore.collection("users").document(user.id)
                .set(mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "syncEnabled" to false
                ))
                .await()
        } catch (e: Exception) {
        }
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            id = uid,
            email = email ?: "",
            displayName = displayName ?: "",
            photoUrl = photoUrl?.toString(),
            createdAt = metadata?.creationTimestamp?.let { Date(it) } ?: Date(),
            lastSyncTime = null,
            syncEnabled = false
        )
    }
}
