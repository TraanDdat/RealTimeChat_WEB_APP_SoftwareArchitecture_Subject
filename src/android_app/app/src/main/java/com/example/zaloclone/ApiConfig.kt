package com.example.zaloclone

object ApiConfig {
    const val BASE_URL = "http://15.135.82.200:8000/";

    // define all config endpoint

    // AuthApiServices endpoint
    const val API_ATH_LOGIN = "/api/auth/login";
    const val API_AUTH_SIGNUP = "/api/auth/signup";
    const val API_AUTH_RESETPW = "/api/auth/resetpw";
    const val API_AUTH_CHECK = "/api/auth/check";
    const val API_AUTH_LOGOUT = "/api/auth/logout";
    const val API_AUTH_UPDATE_PROFILE = "/api/auth/update-profile";

    // ChatApiServices endpoint
    const val API_MSG_CONTACTS = "/api/messages/contacts";
    const val API_MSG_CHATS = "/api/messages/chats";
    const val API_MSG = "/api/messages";

    // FriendApiServices endpoint
    const val USER_ADDFRIEND_ADD_URL = "/api/user/addfriend/add";
    const val USER_ADDFRIEND_GET_REQUEST_URL = "/api/user/addfriend/requests";

    // GroupApiServices endpoint
    const val GROUP_URL = "/api/groups";
}