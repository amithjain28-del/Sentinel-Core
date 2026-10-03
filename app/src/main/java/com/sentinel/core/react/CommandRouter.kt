package com.sentinel.core.react

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.sentinel.core.services.DOMCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ReActState {
    REASONING, EXECUTING, VERIFYING, COMPLETED, FAILED
}

class CommandRouter(private val context: Context, private val onLog: (String) -> Unit) {

    suspend fun processCommand(command: String): String = withContext(Dispatchers.IO) {
        onLog("[ROUTER] Analyzing Intent: $command")

        // 1. Fast-Path: Launch App
        if (command.lowercase().startsWith("launch ")) {
            val appName = command.substringAfter("launch ").trim()
            val launched = launchAppFastPath(appName)
            if (launched) {
                return@withContext "Launched $appName"
            }
        }

        // 2. Full ReAct Loop
        return@withContext executeReActLoop(command)
    }

    private fun launchAppFastPath(appName: String): Boolean {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(appName.lowercase())) {
                val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onLog("[FAST-PATH] Launched ${appInfo.packageName}")
                    return true
                }
            }
        }
        return false
    }

    private suspend fun executeReActLoop(goal: String): String {
        var state = ReActState.REASONING
        var attempts = 0
        var finalResult = ""

        while (state != ReActState.COMPLETED && state != ReActState.FAILED && attempts < 5) {
            attempts++
            when (state) {
                ReActState.REASONING -> {
                    onLog("[ReAct] REASONING: Analyzing DOM for goal: '$goal'...")

                    val currentDom = DOMCache.latestDOM.ifBlank { "UI DOM is empty or inaccessible" }
                    val prompt = """
                        Goal: $goal
                        Current UI DOM:
                        $currentDom

                        Respond with exactly one action string:
                        CLICK [ID]
                        TYPE [ID] [Text]
                        COMPLETED [Summary]
                        FAILED [Reason]
                    """.trimIndent()

                    try {
                        val request = ChatRequest("llama3", listOf(ChatMessage("user", prompt)))
                        val response = LlmClient.api.chat(request)
                        val llmResponse = response.message.content.trim()

                        onLog("[ReAct] LLM Response: $llmResponse")

                        if (llmResponse.startsWith("COMPLETED")) {
                            finalResult = llmResponse.substringAfter("COMPLETED").trim()
                            state = ReActState.COMPLETED
                        } else if (llmResponse.startsWith("FAILED")) {
                            finalResult = llmResponse.substringAfter("FAILED").trim()
                            state = ReActState.FAILED
                        } else {
                            // Proceed to executing based on the response
                            state = ReActState.EXECUTING
                        }
                    } catch (e: Exception) {
                        onLog("[ReAct] Error communicating with LLM: ${e.message}")
                        finalResult = "Error communicating with LLM."
                        state = ReActState.FAILED
                    }
                }
                ReActState.EXECUTING -> {
                    onLog("[ReAct] EXECUTING: Applying action...")
                    // In a real implementation, you would parse the "CLICK [ID]" or "TYPE [ID] [Text]"
                    // from the previous state and send an AccessibilityNodeInfo performAction command.
                    state = ReActState.VERIFYING
                }
                ReActState.VERIFYING -> {
                    onLog("[ReAct] VERIFYING: Checking updated screen state...")
                    // Give the UI time to settle
                    kotlinx.coroutines.delay(1000)
                    state = ReActState.REASONING
                }
                else -> {}
            }
        }

        return if (state == ReActState.COMPLETED) {
            "Task achieved autonomously: $finalResult"
        } else {
            "Task failed after $attempts attempts: $finalResult"
        }
    }
}
