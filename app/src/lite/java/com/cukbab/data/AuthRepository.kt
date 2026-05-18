package com.cukbab.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lite version of AuthRepository without Firebase dependencies.
 * Authentication features are disabled in this version.
 */
object AuthRepository {
    private val _currentUser = MutableStateFlow<Nothing?>(null)
    val currentUser: StateFlow<Nothing?> = _currentUser

    fun signOut() {
        // No-op in lite version
    }

    suspend fun deleteAccount(): Result<Unit> {
        return Result.failure(Exception("Authentication is not available in this version."))
    }

    fun isLoggedIn(): Boolean = false
}
