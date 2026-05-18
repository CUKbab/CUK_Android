package com.cukbab.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A minimal representation of a user for the Lite version.
 */
data class LiteUser(
    val uid: String = "",
    val email: String? = null,
    val photoUrl: Uri? = null,
    val displayName: String? = null
)

/**
 * Lite version of AuthRepository without Firebase dependencies.
 * Authentication features are disabled in this version.
 */
object AuthRepository {
    private val _currentUser = MutableStateFlow<LiteUser?>(null)
    val currentUser: StateFlow<LiteUser?> = _currentUser

    fun signOut() {
        // No-op in lite version
    }

    suspend fun deleteAccount(): Result<Unit> {
        return Result.failure(Exception("Authentication is not available in this version."))
    }

    fun isLoggedIn(): Boolean = false
}
