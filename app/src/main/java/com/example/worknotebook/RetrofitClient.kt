package com.example.worknotebook

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.apply
import kotlin.jvm.java

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:5000/"  // TODO тестовый локальный. Поменять на боевом.

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var retrofit: Retrofit

    fun init(context: Context) {

        val session = UserSessionManager(context)

        val authInterceptor = AuthInterceptor(session)

        val gson = GsonBuilder()
            .registerTypeAdapter(GenderType::class.java, GenderTypeAdapter())
            .create()

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)  // Таймаут подключения
            .readTimeout(20, TimeUnit.SECONDS)  // Таймаут чтения данных
            .writeTimeout(20, TimeUnit.SECONDS)  // Таймаут записи данных (опционально)
            .addInterceptor(authInterceptor)  // Добавляем интерсептор (при наличии) с токеном авторизации
            .addInterceptor(logger)  // Логирование запросов/ответов
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

}
