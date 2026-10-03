package com.sentinel.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sentinel.core.network.LlmService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    serverUrl: String,
    modelName: String,
    temperature: Float,
    onServerUrlChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    llmService: LlmService
) {
    var isTesting by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("LLM Configuration", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = { Text("Endpoint URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = onModelNameChange,
                label = { Text("Model Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Temperature: ${String.format("%.2f", temperature)}")
            Slider(
                value = temperature,
                onValueChange = onTemperatureChange,
                valueRange = 0f..2f,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isTesting = true
                    connectionResult = null
                    coroutineScope.launch {
                        val result = llmService.testConnection()
                        if (result.isSuccess) {
                            connectionResult = true
                        } else {
                            connectionResult = false
                            errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                        }
                        isTesting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Test Connection")
                }
            }

            if (connectionResult != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (connectionResult == true) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = Color.Green)
                        Text("Connection Successful")
                    } else {
                        Icon(Icons.Filled.Warning, contentDescription = "Error", tint = Color.Red)
                        Text("Connection Failed: $errorMessage", color = Color.Red)
                    }
                }
            }
        }
    }
}
