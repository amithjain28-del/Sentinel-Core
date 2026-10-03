package com.sentinel.core.react

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class DeviceIntentManager(private val context: Context) {

    fun launchAppByFuzzyName(appName: String): Boolean {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val lowerTarget = appName.lowercase()

        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(lowerTarget)) {
                val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                        Log.d("DeviceIntentManager", "Launched app: ${appInfo.packageName} matching '$appName'")
                        return true
                    } catch (e: Exception) {
                        Log.e("DeviceIntentManager", "Failed to launch app: ${appInfo.packageName}", e)
                    }
                }
            }
        }

        Log.w("DeviceIntentManager", "No matching app found for: $appName")
        return false
    }
}
