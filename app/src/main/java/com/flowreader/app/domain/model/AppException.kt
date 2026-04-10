package com.flowreader.app.domain.model

/**
 * Base exception class for app-specific exceptions
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    data class DatabaseError(
        override val message: String = "Database operation failed",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class FileError(
        override val message: String = "File operation failed",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class ParseError(
        override val message: String = "Book parsing failed",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class NetworkError(
        override val message: String = "Network operation failed",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class ValidationError(
        override val message: String = "Validation failed",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class UnknownError(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable? = null
    ) : AppException(message, cause)
}

/**
 * Result wrapper for operations that can fail
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppException) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    fun getOrNull(): T? = (this as? Success)?.data

    fun exceptionOrNull(): AppException? = (this as? Error)?.exception
}