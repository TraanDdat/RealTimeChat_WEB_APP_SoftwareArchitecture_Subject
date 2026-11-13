//package com.example.zaloclone.api.remote
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import com.example.zaloclone.api.remote.ResetPasswordApiServices
//
//suspend fun resetPw(request: ResetpwRequest): Result<ResetpwResponse>
//{
//    return withContext(Dispatchers.IO) {
//        try {
//            val response = RetrofitClient.ResetPwApi.resetPw(request)
//            Result.success(response)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}