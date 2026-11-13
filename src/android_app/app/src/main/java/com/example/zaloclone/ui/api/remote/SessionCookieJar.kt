package com.example.zaloclone.ui.api.remote

import android.content.SharedPreferences
import com.example.zaloclone.ApiConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class SessionCookieJar(
    private val preferences: SharedPreferences,
    private val origin: HttpUrl = ApiConfig.BASE_URL.toHttpUrl()
) : CookieJar {
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val session = cookies.lastOrNull { it.name == "jwt" } ?: return
        preferences.edit().apply {
            if (session.expiresAt <= System.currentTimeMillis()) remove("jwt")
            else putString("jwt", session.toString())
        }.apply()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val encoded = preferences.getString("jwt", null) ?: return emptyList()
        val cookie = Cookie.parse(origin, encoded)
        if (cookie == null || cookie.expiresAt <= System.currentTimeMillis()) {
            clear()
            return emptyList()
        }
        return if (cookie.matches(url)) listOf(cookie) else emptyList()
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove("jwt").apply()
    }
}