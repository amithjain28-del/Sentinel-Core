package com.sentinel.core.communications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log

class CommunicationsManager(private val context: Context) {

    fun executeDynamicContactAction(nameQuery: String, message: String? = null): String {
        val contactNumber = resolveContactNumber(nameQuery)

        return if (contactNumber != null) {
            if (message != null) {
                sendSms(contactNumber, message)
                "Sent message to $nameQuery"
            } else {
                callContact(contactNumber)
                "Calling $nameQuery"
            }
        } else {
            "Could not find contact for $nameQuery"
        }
    }

    private fun resolveContactNumber(nameQuery: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    return cursor.getString(numberIndex)
                }
            }
        }
        return null
    }

    private fun callContact(number: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: SecurityException) {
            Log.e("CommManager", "Missing CALL_PHONE permission", e)
        }
    }

    private fun sendSms(number: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
        } catch (e: Exception) {
            Log.e("CommManager", "Failed to send SMS", e)
        }
    }
}
