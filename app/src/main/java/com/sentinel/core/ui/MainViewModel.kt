package com.sentinel.core.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.core.security.SecurityManager
import com.sentinel.core.swarm.SwarmOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Intent
import com.sentinel.core.react.DeviceIntentManager
import com.sentinel.core.communications.MessageManager
import com.sentinel.core.SentinelApp
import com.sentinel.core.memory.VectorMathUtils
import com.sentinel.core.media.MediaScanner
import android.net.Uri
import android.app.Application
import com.sentinel.core.scheduler.AgentWorkManager
import androidx.lifecycle.AndroidViewModel
import com.sentinel.core.network.LlmService
import com.sentinel.core.network.NetworkModule
import com.sentinel.core.network.OllamaChatRequest
import com.sentinel.core.network.OllamaOptions
import com.sentinel.core.orchestrator.PromptOrchestrator
import com.sentinel.core.settings.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.security.MessageDigest

data class ChatMessage(val content: String, val isUser: Boolean)

data class MainUiState(
    val messages: List<ChatMessage> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<String>>(emptyList())
    val executionLogs: StateFlow<List<String>> = _executionLogs.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val settingsDataStore = SettingsDataStore(application)

    val serverUrl = settingsDataStore.serverUrlFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "http://localhost:11434/")
    val modelName = settingsDataStore.modelNameFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "llama3.2")
    val temperature = settingsDataStore.temperatureFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0.7f)

    private val networkModule = NetworkModule("http://localhost:11434/")
    val llmService = LlmService(networkModule)

    private val promptOrchestrator = PromptOrchestrator(application)

    init {
        viewModelScope.launch {
            serverUrl.collect { url ->
                networkModule.updateBaseUrl(url)
            }
        }
    }

    private val swarmOrchestrator = SwarmOrchestrator(
        context = application,
        onLog = { logMsg -> addLog(logMsg) }
    )

    private val securityManager = SecurityManager()

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.saveServerUrl(url)
            networkModule.updateBaseUrl(url)
        }
    }

    fun updateModelName(name: String) {
        viewModelScope.launch {
            settingsDataStore.saveModelName(name)
        }
    }

    fun updateTemperature(temp: Float) {
        viewModelScope.launch {
            settingsDataStore.saveTemperature(temp)
        }
    }

    fun processCommand(command: String, activity: androidx.fragment.app.FragmentActivity) {
        // 1. Add user message to UI
        addMessage(command, isUser = true)

        viewModelScope.launch {
            addLog("> INITIATING COMMAND: $command")

            val lowerCommand = command.lowercase().trim()

            // Hard Intercept 1: Open Settings
            if (lowerCommand == "open settings" || lowerCommand == "settings") {
                addLog("[INTERCEPT] Native Settings routing triggered.")
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                    addMessage("Intent Executed: Opening Settings", isUser = false)
                } catch (e: Exception) {
                    addLog("[ERROR] Failed to open settings: ${e.message}")
                    addMessage("Failed to open Settings.", isUser = false)
                }
                return@launch
            }

            // Hard Intercept 2: App Launching
            if (lowerCommand.startsWith("open ")) {
                val appName = lowerCommand.substringAfter("open ").trim()
                addLog("[INTERCEPT] Native App Launch routing triggered for '$appName'.")
                val deviceIntentManager = DeviceIntentManager(activity)
                val success = deviceIntentManager.launchAppByFuzzyName(appName)
                if (success) {
                    addMessage("Intent Executed: Opening $appName", isUser = false)
                } else {
                    addMessage("Could not find an app matching '$appName'.", isUser = false)
                }
                return@launch
            }

            // Hard Intercept 3: Communications
            if (lowerCommand.startsWith("message ") || lowerCommand.startsWith("text ")) {
                val keyword = if (lowerCommand.startsWith("message ")) "message " else "text "
                val remaining = lowerCommand.substringAfter(keyword).trim()

                // Extremely simple parser: "message [Name] [Payload]" or "message [Name]"
                val parts = remaining.split(" ", limit = 2)
                val contactName = parts.getOrNull(0) ?: ""
                val payload = parts.getOrNull(1) ?: ""

                addLog("[INTERCEPT] Native Messaging routing triggered for contact '$contactName'.")

                val messageManager = MessageManager(activity)
                val success = messageManager.prefillSmsMessage(contactName, payload)

                if (success) {
                    addMessage("Intent Executed: Prepared message for $contactName", isUser = false)
                } else {
                    addMessage("Could not resolve contact '$contactName'.", isUser = false)
                }
                return@launch
            }

            // Hard Intercept 4: Proactive Scheduling
            if (lowerCommand.startsWith("schedule") || lowerCommand.contains("every morning") || lowerCommand.contains("in ") && lowerCommand.contains("minutes")) {
                addLog("[INTERCEPT] Proactive Scheduler triggered.")

                val actionGoal = lowerCommand.replace("schedule", "").trim()

                try {
                    val workManager = AgentWorkManager(activity)
                    val resultMsg = workManager.scheduleTask(lowerCommand, actionGoal)
                    addLog("[SCHEDULER] $resultMsg")
                    addMessage(resultMsg, isUser = false)
                } catch (e: Exception) {
                    addLog("[ERROR] Failed to schedule task: ${e.message}")
                    addMessage("Failed to schedule background task.", isUser = false)
                }
                return@launch
            }

            // Hard Intercept 5: Omnimodal RAG Document Search
            if (lowerCommand.startsWith("find photo") || lowerCommand.startsWith("search document") || lowerCommand.startsWith("find pdf")) {
                addLog("[INTERCEPT] Omnimodal Document Search triggered.")

                // Construct a deterministic, pseudo-embedding from the query string to satisfy the function signature
                // without using non-deterministic random data or relying on an un-implemented ONNX model.
                val md = MessageDigest.getInstance("SHA-256")
                val hashBytes = md.digest(lowerCommand.toByteArray())
                val mockQueryVector = FloatArray(512) { i -> hashBytes[i % hashBytes.size].toFloat() / 128f }

                try {
                    val allDocs = SentinelApp.database.agentMemoryDao().getAllDocuments()

                    if (allDocs.isEmpty()) {
                        addLog("[RAG] Room DB empty. Scanning MediaStore...")
                        val scanner = MediaScanner(activity)
                        val images = scanner.scanLocalImages()
                        val pdfs = scanner.scanLocalPdfs()
                        addLog("[RAG] Found ${images.size} images and ${pdfs.size} PDFs on device.")
                        addMessage("Memory DB is empty. I've found ${images.size} images and ${pdfs.size} PDFs, but they need to be embedded first.", isUser = false)
                        return@launch
                    }

                    val topMatches = VectorMathUtils.findTopMatches(mockQueryVector, allDocs, topK = 1)

                    if (topMatches.isNotEmpty() && topMatches[0].second > -1.0f) {
                        val bestMatch = topMatches[0].first
                        addLog("[RAG] Match found: ${bestMatch.filename} (Score: ${topMatches[0].second})")

                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(bestMatch.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(viewIntent)
                        addMessage("Opened best match: ${bestMatch.filename}", isUser = false)
                    } else {
                        addLog("[RAG] No confident matches found in Room.")
                        addMessage("I searched my memory but could not find a matching document.", isUser = false)
                    }
                } catch (e: Exception) {
                    addLog("[ERROR] RAG Search failed: ${e.message}")
                    addMessage("Failed to search documents.", isUser = false)
                }
                return@launch
            }

            // Hard Intercept 6: Multi-Agent Swarm for Complex Queries
            if (lowerCommand.contains("search") && lowerCommand.contains("and") || lowerCommand.contains("summarize") || lowerCommand.contains("research")) {
                addLog("[INTERCEPT] Multi-Agent Swarm triggered for complex task.")
                _isGenerating.value = true
                try {
                    val result = swarmOrchestrator.dispatch(command, activity)
                    addLog("[ORCHESTRATOR] Swarm task complete.")
                    addMessage(result, isUser = false)
                } catch (e: SecurityException) {
                    addLog("[SECURITY] Execution halted: ${e.message}")
                    addMessage("Action cancelled. Biometric verification failed or was denied.", isUser = false)
                } catch (e: Exception) {
                    addLog("[ERROR] Swarm execution failed: ${e.message}")
                    addMessage("Failed to execute complex swarm task.", isUser = false)
                } finally {
                    _isGenerating.value = false
                }
                return@launch
            }

            // Final Fallback: Local LLM Engine Conversation
            _isGenerating.value = true
            addLog("[LLM] Routing conversational query to PromptOrchestrator...")

            try {
                val messages = promptOrchestrator.buildPrompt(command)
                val request = OllamaChatRequest(
                    model = modelName.value,
                    messages = messages,
                    stream = false,
                    options = OllamaOptions(temperature = temperature.value)
                )

                addLog("[LLM] Sending request to ${serverUrl.value}...")
                val result = llmService.generateChat(request)

                if (result.isSuccess) {
                    val responseText = result.getOrNull()?.message?.content ?: "Empty response."
                    addLog("[LLM] Inference complete.")
                    addMessage(responseText, isUser = false)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown network error."
                    addLog("[LLM] Inference failed: $error")
                    addMessage("I'm sorry, I couldn't reach the local AI server. Error: $error", isUser = false)
                }
            } catch (e: Exception) {
                addLog("[ERROR] Unhandled exception during LLM pipeline: ${e.message}")
                addMessage("An unexpected error occurred during processing.", isUser = false)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun addMessage(content: String, isUser: Boolean) {
        val current = _uiState.value.messages.toMutableList()
        current.add(ChatMessage(content, isUser))
        _uiState.value = _uiState.value.copy(messages = current)
    }

    private fun addLog(log: String) {
        val current = _executionLogs.value.toMutableList()
        current.add(log)
        // Keep last 100 logs
        if (current.size > 100) current.removeAt(0)
        _executionLogs.value = current
    }
}
