package com.sentinel.core.scheduler

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.Calendar

class AgentWorkManager(private val context: Context) {

    fun scheduleTask(timePhrase: String, actionGoal: String): String {
        val workManager = WorkManager.getInstance(context)
        val data = Data.Builder().putString("GOAL_EXTRA", actionGoal).build()

        return when {
            timePhrase.contains("in") && timePhrase.contains("minutes") -> {
                // e.g. "in 30 minutes"
                val regex = Regex("\\d+")
                val match = regex.find(timePhrase)
                val minutes = match?.value?.toLongOrNull() ?: 15L

                val request = OneTimeWorkRequestBuilder<AgentBackgroundWorker>()
                    .setInitialDelay(minutes, TimeUnit.MINUTES)
                    .setInputData(data)
                    .build()

                workManager.enqueue(request)
                "Scheduled '$actionGoal' to run in $minutes minutes."
            }
            timePhrase.contains("every morning at 8") -> {
                // e.g. "every morning at 8 AM"
                val request = PeriodicWorkRequestBuilder<AgentBackgroundWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(calculateDelayToNext8AM(), TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "MorningRoutine_$actionGoal",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                "Scheduled '$actionGoal' to run every morning at 8 AM."
            }
            else -> {
                // Fallback immediate execution
                val request = OneTimeWorkRequestBuilder<AgentBackgroundWorker>()
                    .setInputData(data)
                    .build()
                workManager.enqueue(request)
                "Executing '$actionGoal' in the background right now."
            }
        }
    }

    private fun calculateDelayToNext8AM(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
