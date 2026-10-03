package com.sentinel.core.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OllamaMessage(val role: String, val content: String)
data class OllamaOptions(val temperature: Float, @SerializedName("top_p") val topP: Float = 0.9f)

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions
)

data class OllamaChatResponse(
    val message: OllamaMessage,
    val done: Boolean,
    @SerializedName("total_duration") val totalDuration: Long
)

// Data class for testing the connection
data class OllamaTagsResponse(val models: List<Any>)

interface OllamaApiService {
    @POST("/api/chat")
    suspend fun chat(@Body request: OllamaChatRequest): OllamaChatResponse

    @GET("/api/tags")
    suspend fun getTags(): OllamaTagsResponse
}

class NetworkModule(private var baseUrl: String) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private var retrofit = buildRetrofit(baseUrl)
    var apiService = retrofit.create(OllamaApiService::class.java)
        private set

    fun updateBaseUrl(newUrl: String) {
        if (newUrl != baseUrl && newUrl.isNotBlank()) {
            val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
            baseUrl = formattedUrl
            retrofit = buildRetrofit(formattedUrl)
            apiService = retrofit.create(OllamaApiService::class.java)
        }
    }

    private fun buildRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

class LlmService(private val networkModule: NetworkModule) {

    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = networkModule.apiService.getTags()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateChat(request: OllamaChatRequest): Result<OllamaChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = networkModule.apiService.chat(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
