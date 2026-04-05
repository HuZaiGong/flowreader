package com.flowreader.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.User
import com.flowreader.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val user: User? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoggedIn = user != null
                    )
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun updateConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, error = null) }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name, error = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "请填写邮箱和密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.signInWithEmail(state.email, state.password)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, user = user, isLoggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "登录失败") }
                }
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.displayName.isBlank()) {
            _uiState.update { it.copy(error = "请填写所有字段") }
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "两次密码不一致") }
            return
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "密码至少6位") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.signUpWithEmail(state.email, state.password)
                .onSuccess { user ->
                    authRepository.updateUserProfile(state.displayName)
                    _uiState.update { it.copy(isLoading = false, user = user, isLoggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "注册失败") }
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, user = user, isLoggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Google登录失败") }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update {
                AuthUiState()
            }
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "请先输入邮箱") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.resetPassword(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = "重置链接已发送到您的邮箱") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "发送失败") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
