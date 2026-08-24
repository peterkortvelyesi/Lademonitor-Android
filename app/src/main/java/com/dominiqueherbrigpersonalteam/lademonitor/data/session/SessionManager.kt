package com.dominiqueherbrigpersonalteam.lademonitor.data.session

import com.dominiqueherbrigpersonalteam.lademonitor.data.model.AuthResponse
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.AuthUser
import com.dominiqueherbrigpersonalteam.lademonitor.data.remote.ApiClient
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Port of the iOS `SessionManager`: the central auth state that drives the login-vs-main-app
 * switch. A token present at startup counts as an active session; an invalid one falls out on the
 * first API call through the 401 handling in [ApiClient].
 */
object SessionManager {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser

    fun init() {
        _isAuthenticated.value = TokenStore.readToken() != null
    }

    fun completeAuthentication(response: AuthResponse) {
        TokenStore.saveToken(response.token)
        _currentUser.value = response.user
        _isAuthenticated.value = true
    }

    suspend fun logout() {
        runCatching { ApiClient.logout() }
        clearLocalSession()
    }

    /** Called on a 401 for an authenticated request: the session is no longer valid. */
    fun invalidateSession() {
        clearLocalSession()
    }

    suspend fun refreshCurrentUserIfNeeded() {
        if (!_isAuthenticated.value || _currentUser.value != null) return
        _currentUser.value = runCatching { ApiClient.fetchMe() }.getOrNull()
    }

    private fun clearLocalSession() {
        TokenStore.deleteToken()
        _currentUser.value = null
        _isAuthenticated.value = false
    }
}
