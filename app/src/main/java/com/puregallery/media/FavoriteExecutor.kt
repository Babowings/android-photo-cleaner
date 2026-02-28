package com.puregallery.media

import android.content.Context
import android.content.ContentValues
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class FavoriteExecutor(
    private val context: Context
) {
    fun buildFavoriteIntentSender(uri: Uri, favorite: Boolean = true): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createFavoriteRequest(
            context.contentResolver,
            listOf(uri),
            favorite
        ).intentSender
    }

    fun applyFavoriteDirectly(uri: Uri, favorite: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_FAVORITE, if (favorite) 1 else 0)
        }
        val updated = runCatching {
            context.contentResolver.update(uri, values, null, null)
        }.getOrDefault(0)
        return updated > 0
    }
}

