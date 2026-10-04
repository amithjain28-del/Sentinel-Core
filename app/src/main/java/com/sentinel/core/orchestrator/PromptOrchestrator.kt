package com.sentinel.core.orchestrator

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.sentinel.core.SentinelApp
import com.sentinel.core.bridge.WebResearchScraper
import com.sentinel.core.network.OllamaMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PromptOrchestrator(private val context: Context) {

    suspend fun buildPrompt(userGoal: String): List<OllamaMessage> = withContext(Dispatchers.IO) {
        val systemTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceContext = DeviceContextState(context).getHardwareContextString()

        // Retrieve RAG Context (Just mock or fetch recent for now, full RAG requires embedding the goal)
        // Here we just fetch recent memory facts to give basic context.
        val recentMemory = try {
            val facts = SentinelApp.database.memoryDao().searchMemory("") // Get all or recent
            facts.takeLast(3).joinToString("\n") { "- ${it.factValue}" }
        } catch (e: Exception) {
            "No local memory available."
        }

        // Basic web search for external context if explicitly requested
        val webContext = if (userGoal.lowercase().contains("search") || userGoal.lowercase().contains("who is")) {
            val scraper = WebResearchScraper()
            scraper.searchAndScrape(userGoal)
        } else {
            "No web context."
        }

        val systemPrompt = """
            You are Sentinel-Core, an elite autonomous AI agent running directly on the user's Android device.
            Current System Time: $systemTime
            $deviceContext

            Recent Device Memory / Notifications:
            $recentMemory

            Web Context:
            $webContext

            Instructions:
            - Provide a concise, direct answer in a single sentence or two.
            - Do not use markdown, bullet points, or complex formatting.
            - Write in a natural conversational tone suitable for a Text-to-Speech engine.
            - Do not explain your reasoning unless explicitly asked.
        """.trimIndent()

        listOf(
            OllamaMessage(role = "system", content = systemPrompt),
            OllamaMessage(role = "user", content = userGoal)
        )
    }
}
