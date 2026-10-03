package com.sentinel.core.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sentinel.core.swarm.SwarmOrchestrator

class AgentBackgroundWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskGoal = inputData.getString("GOAL_EXTRA") ?: "Check system status"
        Log.d("AgentWorker", "Waking up invisibly to execute: $taskGoal")

        try {
            // Re-use Swarm to execute the goal in the background
            val swarm = SwarmOrchestrator(appContext, onLog = { Log.d("AgentWorkerLog", it) })
            val result = swarm.dispatch(taskGoal)

            showNotification(taskGoal, result)
            return Result.success()
        } catch (e: Exception) {
            Log.e("AgentWorker", "Background execution failed", e)
            return Result.failure()
        }
    }

    private fun showNotification(goal: String, resultStr: String) {
        val channelId = "SENTINEL_CRON_CHANNEL"
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sentinel Automations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Background task execution results"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Automation Complete: $goal")
            .setContentText(resultStr)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resultStr))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
