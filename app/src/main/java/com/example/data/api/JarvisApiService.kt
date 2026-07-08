package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface JarvisApiService {
    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @POST("register_device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): CommonResponse

    @POST("forget")
    suspend fun forget(@Body request: ForgetRequest): CommonResponse

    @GET("health")
    suspend fun health(): Map<String, Any>
}

object RetrofitBuilder {
    private var currentUrl: String? = null
    private var cachedService: JarvisApiService? = null

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    fun getApiService(baseUrl: String): JarvisApiService {
        // Normalize trailing slash
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        synchronized(this) {
            if (cachedService == null || currentUrl != normalizedUrl) {
                currentUrl = normalizedUrl
                val retrofit = Retrofit.Builder()
                    .baseUrl(normalizedUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                cachedService = retrofit.create(JarvisApiService::class.java)
            }
            return cachedService!!
        }
    }
}
