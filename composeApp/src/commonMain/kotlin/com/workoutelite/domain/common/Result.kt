package com.workoutelite.domain.common

interface AppError

sealed interface Result<out D, out E : AppError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : AppError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : AppError, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(transform(data))
    }
