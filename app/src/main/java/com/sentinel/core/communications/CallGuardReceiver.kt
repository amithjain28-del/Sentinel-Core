package com.sentinel.core.communications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallGuardReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val currentNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            var state = TelephonyManager.CALL_STATE_IDLE
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> state = TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> state = TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_IDLE -> state = TelephonyManager.CALL_STATE_IDLE
            }

            onCallStateChanged(context, state, currentNumber)
        }
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) {
            return // No change
        }

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                incomingNumber = number
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call answered or outgoing call
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Check if we transitioned directly from RINGING to IDLE (Missed Call)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    incomingNumber?.let { num ->
                        handleMissedCall(context, num)
                    }
                }
            }
        }

        lastState = state
    }

    private fun handleMissedCall(context: Context, number: String) {
        Log.d("CallGuard", "Missed call detected from: $number")

        val hasSmsPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasPhonePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasCallLogPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPerm && hasPhonePerm && hasCallLogPerm) {
            sendAutoReply(context, number)
        } else {
            Log.w("CallGuard", "Missing permissions to send auto-reply SMS.")
        }
    }

    private fun sendAutoReply(context: Context, number: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val message = "I am currently unavailable. Please reply with your message and my AI assistant will save it for me."

            smsManager.sendTextMessage(number, null, message, null, null)
            Log.d("CallGuard", "Auto-reply sent to $number")
        } catch (e: Exception) {
            Log.e("CallGuard", "Failed to send auto-reply", e)
        }
    }
}
