package com.sentinel.core.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class SecurityManager {

    suspend fun authenticateUser(activity: FragmentActivity): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)

            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Wait for success or error to resume coroutine.
                        // Do not resume on intermediate failure.
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authorize Sentinel Action")
                .setSubtitle("Confirm physical identity for high-risk autonomous intent.")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }
}
