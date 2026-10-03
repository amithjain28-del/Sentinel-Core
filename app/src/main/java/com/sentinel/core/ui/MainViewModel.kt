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

            // Analyze risk
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
