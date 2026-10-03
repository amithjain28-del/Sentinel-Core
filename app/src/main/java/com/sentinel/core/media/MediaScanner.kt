package com.sentinel.core.media

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedMedia(
    val uri: Uri,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val dateModified: Long,
    val extraInfo: String = "" // For page counts, image dimensions, etc.
)

class MediaScanner(private val context: Context) {

    suspend fun scanLocalImages(): List<ScannedMedia> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedMedia>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val selection = "${MediaStore.Images.Media.MIME_TYPE} IN (?, ?, ?)"
        val selectionArgs = arrayOf("image/jpeg", "image/png", "image/webp")

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    results.add(
                        ScannedMedia(
                            uri = uri,
                            filename = cursor.getString(nameCol) ?: "Unknown",
                            mimeType = cursor.getString(mimeCol) ?: "image/*",
                            size = cursor.getLong(sizeCol),
                            dateModified = cursor.getLong(dateCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaScanner", "Error scanning images", e)
        }
        return@withContext results
    }

    suspend fun scanLocalPdfs(): List<ScannedMedia> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedMedia>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(collection, id.toString())

                    val pageCount = getPdfPageCount(uri)

                    results.add(
                        ScannedMedia(
                            uri = uri,
                            filename = cursor.getString(nameCol) ?: "Unknown PDF",
                            mimeType = cursor.getString(mimeCol) ?: "application/pdf",
                            size = cursor.getLong(sizeCol),
                            dateModified = cursor.getLong(dateCol),
                            extraInfo = "Pages: $pageCount"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaScanner", "Error scanning PDFs", e)
        }
        return@withContext results
    }

    private fun getPdfPageCount(uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                renderer.pageCount
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e("MediaScanner", "Cannot render PDF for page count", e)
            -1
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }
}
