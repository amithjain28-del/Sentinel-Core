package com.sentinel.core.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.core.security.BiometricEnclave
import com.sentinel.core.swarm.SwarmOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Intent
import com.sentinel.core.react.DeviceIntentManager
import com.sentinel.core.communications.MessageManager

data class ChatMessage(val content: String, val isUser: Boolean)

data class MainUiState(
    val messages: List<ChatMessage> = emptyList()
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<String>>(emptyList())
    val executionLogs: StateFlow<List<String>> = _executionLogs.asStateFlow()

    private val swarmOrchestrator = SwarmOrchestrator(
        onLog = { logMsg -> addLog(logMsg) }
    )

    private val biometricEnclave = BiometricEnclave()

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

            // Analyze risk and fallback to Swarm / ReAct
            val isHighRisk = checkRiskLevel(command)
            if (isHighRisk) {
                addLog("[SECURITY] High Risk action detected. Requesting Biometric Auth.")
                addMessage("This is a sensitive action. Please authenticate.", isUser = false)

                val authResult = biometricEnclave.authenticate(activity)
                if (authResult) {
                    addLog("[SECURITY] Biometric Auth SUCCESS.")
                    executeSwarm(command)
                } else {
                    addLog("[SECURITY] Biometric Auth FAILED or CANCELED.")
                    addMessage("Authentication failed. Action aborted.", isUser = false)
                }
            } else {
                executeSwarm(command)
            }
        }
    }

    private suspend fun executeSwarm(command: String) {
        addLog("[ORCHESTRATOR] Spawning swarm agents...")
        addMessage("Processing your request...", isUser = false)

        val result = swarmOrchestrator.dispatch(command)

        addLog("[ORCHESTRATOR] Swarm task complete.")
        addMessage(result, isUser = false)
    }

    private fun checkRiskLevel(command: String): Boolean {
        // Simple heuristic for now; eventually LLM-driven
        val lower = command.lowercase()
        return lower.contains("delete") ||
               lower.contains("transfer") ||
               lower.contains("password")
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
