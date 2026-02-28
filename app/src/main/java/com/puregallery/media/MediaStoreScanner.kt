package com.puregallery.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.puregallery.model.PhotoItem
import com.puregallery.model.PhotoStatus

class MediaStoreScanner(
    private val context: Context
) {
    fun scanImages(): List<PhotoItem> {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val photos = mutableListOf<PhotoItem>()

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val favoriteColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = cursor.getLong(dateTakenColumn)
                val dateAddedSeconds = cursor.getLong(dateAddedColumn)
                val fallbackDate = dateAddedSeconds * 1000
                val effectiveDate = if (dateTaken > 0) dateTaken else fallbackDate
                val isFavorite = favoriteColumn >= 0 && cursor.getInt(favoriteColumn) == 1

                val contentUri = ContentUris.withAppendedId(collection, id)
                photos.add(
                    PhotoItem(
                        id = id,
                        uri = contentUri,
                        dateTaken = effectiveDate,
                        status = if (isFavorite) PhotoStatus.FAVORITE else PhotoStatus.NORMAL
                    )
                )
            }
        }

        return photos
    }
}
