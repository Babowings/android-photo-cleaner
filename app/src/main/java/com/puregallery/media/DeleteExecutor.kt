package com.puregallery.media

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class DeleteExecutor(
    private val context: Context
) {
    fun buildDeleteIntentSender(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    fun deleteDirectlyForApi29(contentResolver: ContentResolver, uris: List<Uri>): Int {
        var deletedCount = 0
        uris.forEach { uri ->
            val result = runCatching {
                contentResolver.delete(uri, null, null)
            }.getOrDefault(0)
            deletedCount += result
        }
        return deletedCount
    }
}
