package com.sentinel.core.react

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.sentinel.core.services.DOMCache
import com.sentinel.core.services.AgentAccessibilityService
import com.sentinel.core.network.LlmService
import com.sentinel.core.network.OllamaChatRequest
import com.sentinel.core.network.OllamaMessage
import com.sentinel.core.network.OllamaOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

enum class ReActState {
    REASONING, EXECUTING, VERIFYING, COMPLETED, FAILED
}

class CommandRouter(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val llmService: LlmService,
    private val modelName: String,
    private val temperature: Float
) {

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
        var currentAction = ""

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

                        Respond with exactly one action string using Regex format:
                        CLICK \[ID\]
                        TYPE \[ID\] \[Text\]
                        LAUNCH \[App\]
                        COMPLETED \[Summary\]
                        FAILED \[Reason\]
                    """.trimIndent()

                    try {
                        val request = OllamaChatRequest(
                            model = modelName,
                            messages = listOf(OllamaMessage("user", prompt)),
                            stream = false,
                            options = OllamaOptions(temperature)
                        )
                        val result = llmService.generateChat(request)

                        if (result.isSuccess) {
                            val llmResponse = result.getOrNull()?.message?.content?.trim() ?: "FAILED [Empty response]"
                            onLog("[ReAct] LLM Response: $llmResponse")

                            if (llmResponse.startsWith("COMPLETED")) {
                                finalResult = llmResponse.substringAfter("COMPLETED").trim()
                                state = ReActState.COMPLETED
                            } else if (llmResponse.startsWith("FAILED")) {
                                finalResult = llmResponse.substringAfter("FAILED").trim()
                                state = ReActState.FAILED
                            } else {
                                currentAction = llmResponse
                                state = ReActState.EXECUTING
                            }
                        } else {
                            onLog("[ReAct] LLM Inference Failed: ${result.exceptionOrNull()?.message}")
                            finalResult = "Error communicating with LLM."
                            state = ReActState.FAILED
                        }
                    } catch (e: Exception) {
                        onLog("[ReAct] Error communicating with LLM: ${e.message}")
                        finalResult = "Error communicating with LLM."
                        state = ReActState.FAILED
                    }
                }
                ReActState.EXECUTING -> {
                    onLog("[ReAct] EXECUTING: Applying action '$currentAction'...")
                    // Parse action
                    val clickRegex = Regex("""CLICK \[(\d+)\]""")
                    val typeRegex = Regex("""TYPE \[(\d+)\] \[(.+)\]""")

                    if (clickRegex.matches(currentAction)) {
                        val id = clickRegex.find(currentAction)?.groupValues?.get(1)?.toIntOrNull()
                        if (id != null) {
                            val success = AgentAccessibilityService.instance?.executeTap(id) ?: false
                            onLog("[ReAct] Executing Tap on ID: $id (Success: $success)")
                        }
                    } else if (typeRegex.matches(currentAction)) {
                        val match = typeRegex.find(currentAction)
                        val id = match?.groupValues?.get(1)?.toIntOrNull()
                        val text = match?.groupValues?.get(2)
                        if (id != null && text != null) {
                            val success = AgentAccessibilityService.instance?.executeType(id, text) ?: false
                            onLog("[ReAct] Executing Type '$text' on ID: $id (Success: $success)")
                        }
                    }

                    state = ReActState.VERIFYING
                }
                ReActState.VERIFYING -> {
                    onLog("[ReAct] VERIFYING: Checking updated screen state...")
                    delay(2000)
                    // Trigger DOM re-read
                    onLog("[ReAct] Re-reading UI DOM...")
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
