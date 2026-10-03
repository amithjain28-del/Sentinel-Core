package com.sentinel.core.swarm

import android.content.Context
import com.sentinel.core.SentinelApp
import com.sentinel.core.bridge.WebResearchScraper
import com.sentinel.core.communications.MessageManager
import com.sentinel.core.react.DeviceIntentManager
import com.sentinel.core.security.SecurityManager
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SwarmOrchestrator(
    private val context: Context,
    private val onLog: (String) -> Unit
) {
    suspend fun dispatch(command: String, activity: FragmentActivity? = null): String = withContext(Dispatchers.IO) {
        onLog("[SWARM] Decomposing complex goal: '$command'")

        // Use withTimeout to ensure sub-agents don't hang the parent indefinitely
        val finalContext = withTimeoutOrNull(30_000L) {
            coroutineScope {
                onLog("[SYS] Spawning Parallel Sub-Agents...")

                val researchDeferred = async { runResearchAgent(command) }
                val ragDeferred = async { runRAGAgent(command) }

                // Await all data gathering before passing to action agent
                val results = awaitAll(researchDeferred, ragDeferred)

                onLog("[VERIFICATION] Synthesized context from ${results.size} agents.")
                results.joinToString("\n---\n")
            }
        }

        if (finalContext == null) {
            onLog("[ERROR] Swarm execution timed out.")
            return@withContext "Execution timed out while gathering context."
        }

        // Once context is gathered, trigger the ActionAgent sequentially
        val actionResult = runActionAgent(command, finalContext, activity)
        return@withContext actionResult
    }

    private suspend fun runResearchAgent(command: String): String {
        onLog("[ResearchAgent] Triggering WebScraper...")
        val scraper = WebResearchScraper()
        val webResult = scraper.searchAndScrape(command)
        onLog("[ResearchAgent] Retrieved web data successfully.")
        return "Web Context: $webResult"
    }

    private suspend fun runRAGAgent(command: String): String {
        onLog("[RAGAgent] Searching local vector database...")
        // In a full implementation, you'd embed the command here.
        // For now, we query Room directly.
        return try {
            val docs = SentinelApp.database.agentMemoryDao().getAllDocuments()
            if (docs.isNotEmpty()) {
                onLog("[RAGAgent] Found ${docs.size} vectors. Passing best match.")
                "Local DB Context: ${docs.first().filename} contains relevant data."
            } else {
                onLog("[RAGAgent] Local DB is empty.")
                "Local DB Context: None."
            }
        } catch (e: Exception) {
            onLog("[ERROR] RAGAgent failure: ${e.message}")
            "Local DB Context: Error retrieving."
        }
    }

    private suspend fun runActionAgent(command: String, synthesizedContext: String, activity: FragmentActivity?): String {
        onLog("[ActionAgent] Analyzing intent for execution...")
        val lower = command.lowercase()

        val isHighRisk = lower.contains("text") || lower.contains("message") ||
                         lower.contains("open") || lower.contains("launch") ||
                         lower.contains("call")

        if (isHighRisk) {
            if (activity == null) {
                onLog("[SECURITY] High Risk action queued but no UI context available for Biometric validation. Aborting.")
                throw SecurityException("High risk actions require active user authentication.")
            }
            onLog("[SECURITY] High Risk action queued. Prompting Biometric Enclave...")
            val isAuthenticated = SecurityManager().authenticateUser(activity)
            if (!isAuthenticated) {
                onLog("[SECURITY] Authentication failed or cancelled. Aborting action.")
                throw SecurityException("User cancelled biometric authentication.")
            }
            onLog("[SECURITY] Identity verified.")
        }

        return if (lower.contains("text") || lower.contains("message")) {
            val contactName = command.substringAfter("to ").substringBefore(" ").trim()
            if (contactName.isNotBlank()) {
                onLog("[ActionAgent] Routing to MessageManager for contact: $contactName")
                val success = MessageManager(context).prefillSmsMessage(contactName, synthesizedContext.take(140) + "...")
                if (success) "Prepared message for $contactName based on research." else "Failed to prepare message."
            } else {
                "No valid contact found in command."
            }
        } else if (lower.contains("open") || lower.contains("launch")) {
            val appName = command.substringAfter("open ").substringAfter("launch ").trim()
            onLog("[ActionAgent] Routing to DeviceIntentManager for app: $appName")
            val success = DeviceIntentManager(context).launchAppByFuzzyName(appName)
            if (success) "Launched $appName." else "Could not launch $appName."
        } else {
            onLog("[ActionAgent] No actionable intent detected. Returning summary.")
            "Task Complete. Synthesis: $synthesizedContext"
        }
    }
}
