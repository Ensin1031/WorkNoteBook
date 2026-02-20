package com.example.worknotebook

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val session: UserSessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.getToken()

        val request = chain.request().newBuilder().apply {
            token?.let {
                addHeader("Authorization", "Bearer $it")
            }
        }.build()

        return chain.proceed(request)
    }
}
