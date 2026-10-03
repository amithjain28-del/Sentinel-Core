package com.sentinel.core.bridge

import android.content.Context
import android.content.Intent
import android.util.Log

class TermuxBridge(private val context: Context) {

    fun executeScript(command: String, scriptType: String = "bash"): Boolean {
        return try {
            val intent = Intent("com.termux.RUN_COMMAND")
            intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/$scriptType")
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            // intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0") // 0 = default, 1 = wait

            context.startService(intent)
            Log.d("TermuxBridge", "Executed Termux script: $command")
            true
        } catch (e: Exception) {
            Log.e("TermuxBridge", "Failed to bridge to Termux", e)
            false
        }
    }
}
