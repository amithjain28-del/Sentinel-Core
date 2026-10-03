package com.sentinel.core.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sentinel.core.SentinelApp
import com.sentinel.core.memory.MemoryFact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OmniNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            Log.d("OmniListener", "Notification from $packageName: $title - $text")

            val fact = MemoryFact(
                factKey = "notification_$packageName",
                factValue = "$title: $text"
            )

            serviceScope.launch {
                try {
                    SentinelApp.database.memoryDao().insertFact(fact)
                } catch (e: Exception) {
                    Log.e("OmniListener", "Failed to save notification to memory", e)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
