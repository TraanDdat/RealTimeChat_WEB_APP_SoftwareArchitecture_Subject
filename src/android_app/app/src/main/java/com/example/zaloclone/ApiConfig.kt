package com.example.zaloclone

object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8000/";

    // define all config endpoint

    // AuthApiServices endpoint
    const val API_ATH_LOGIN = "/api/auth/login";
    const val API_AUTH_SIGNUP = "/api/auth/signup";
    const val API_AUTH_RESETPW = "/api/auth/resetpw";
    const val API_AUTH_CHECK = "/api/auth/check";
    const val API_AUTH_LOGOUT = "/api/auth/logout";
    const val API_AUTH_UPDATE_PROFILE = "api/auth/update-profile";

    // ChatApiServices endpoint
    const val API_MSG_CONTACTS = "/api/messages/contacts";
    const val API_MSG_CHATS = "/api/messages/chats";
    const val API_MSG = "/api/messages";

    const val API_USERS_ROOT = "/api/users/root";
    fun userRoot(username: String) = "$API_USERS_ROOT/$username";
}