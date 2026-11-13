const  API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8000";
const API_AUTH_LOGIN = import.meta.env.VITE_API_AUTH_LOGIN || "/api/auth/login";
const API_AUTH_REGISTER = import.meta.env.VITE_API_AUTH_REGISTER || "/api/auth/register";
const API_USER_ADDFRIEND_ADD = import.meta.env.API_USER_ADDFRIEND_ADD || "/api/user/addfriend/add";
const API_USER_ADDFRIEND_REQUEST = import.meta.env.API_USER_ADDFRIEND_REQUEST || "/api/user/addfriend/request";

export {    API_BASE_URL, 
            API_AUTH_LOGIN, 
            API_AUTH_REGISTER, 
            API_USER_ADDFRIEND_ADD, 
            API_USER_ADDFRIEND_REQUEST };