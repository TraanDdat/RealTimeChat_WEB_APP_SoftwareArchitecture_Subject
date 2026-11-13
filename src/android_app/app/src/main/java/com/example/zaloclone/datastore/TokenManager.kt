package com.example.zaloclone.api.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(
    name = "auth_preferences"
)

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("access_token")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}