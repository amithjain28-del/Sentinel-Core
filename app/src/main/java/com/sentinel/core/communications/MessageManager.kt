package com.sentinel.core.communications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

class MessageManager(private val context: Context) {

    fun prefillSmsMessage(contactNameQuery: String, messagePayload: String): Boolean {
        val phoneNumber = resolveContactNumber(contactNameQuery)

        if (phoneNumber == null) {
            Log.w("MessageManager", "Could not resolve contact number for query: '$contactNameQuery'")
            return false
        }

        return launchSmsIntent(phoneNumber, messagePayload)
    }

    private fun resolveContactNumber(nameQuery: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex >= 0) {
                        return cursor.getString(numberIndex)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("MessageManager", "Missing READ_CONTACTS permission", e)
        } catch (e: Exception) {
            Log.e("MessageManager", "Error querying contacts", e)
        }
        return null
    }

    private fun launchSmsIntent(phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("MessageManager", "Launched SMS intent prefilled for $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e("MessageManager", "Failed to launch SMS intent", e)
            false
        }
    }
}
