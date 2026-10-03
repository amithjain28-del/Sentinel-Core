package com.sentinel.core.react

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatMessage(val role: String, val content: String)
data class ChatRequest(val model: String, val messages: List<ChatMessage>, val stream: Boolean = false)
data class ChatResponse(val message: ChatMessage)

interface OllamaApi {
    @POST("/api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

object LlmClient {
    private const val BASE_URL = "http://localhost:11434"

    val api: OllamaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OllamaApi::class.java)
    }
}
