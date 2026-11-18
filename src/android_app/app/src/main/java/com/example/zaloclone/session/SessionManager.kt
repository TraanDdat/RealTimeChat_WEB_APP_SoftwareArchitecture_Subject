package com.example.zaloclone.session

class SessionManager {
    var currentUserId: Int? = null
        private set

    var currentEmail: String? = null
        private set

    fun setCurrentUser(userId: Int, email: String) {
        currentUserId = userId
        currentEmail = email
    }

    fun clearSession() {
        currentUserId = null
        currentEmail = null
    }
}