package com.example.zaloclone

object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8000/";

    const val API_ATH_LOGIN = "/api/auth/login";
    const val API_AUTH_REGISTER = "/api/auth/register";
    const val API_AUTH_RESETPW = "/api/auth/resetpw";

    const val API_USERS_ROOT = "/api/users/root";
    fun userRoot(username: String) = "$API_USERS_ROOT/$username";
}