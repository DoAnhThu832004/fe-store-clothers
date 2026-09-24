package org.example.project.core.common

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val throwable: Throwable) : Resource<Nothing>
}
