package com.puregallery.data

import android.content.ContentResolver
import android.net.Uri
import com.puregallery.media.MediaStoreScanner
import com.puregallery.model.PhotoItem
import com.puregallery.model.PhotoStatus

class PhotoRepository(
    private val scanner: MediaStoreScanner,
    private val photoDao: PhotoDao
) {
    suspend fun loadPhotos(): List<PhotoItem> {
        val scanned = scanner.scanImages()
        val statusMap = photoDao.getAll().associateBy { it.id }

        val merged = scanned.map { photo ->
            val local = statusMap[photo.id]
            if (local?.status == PhotoStatus.PENDING_DELETE) {
                photo.copy(status = PhotoStatus.PENDING_DELETE)
            } else {
                photo
            }
        }

        photoDao.upsertAll(merged.map { it.toEntity() })
        return merged
    }

    suspend fun updateStatus(photoId: Long, status: PhotoStatus, photos: List<PhotoItem>) {
        val target = photos.firstOrNull { it.id == photoId } ?: return
        photoDao.upsert(target.copy(status = status).toEntity())
    }

    suspend fun getPendingDeletePhotos(): List<PhotoItem> {
        return photoDao.getByStatus(PhotoStatus.PENDING_DELETE).map {
            PhotoItem(
                id = it.id,
                uri = Uri.parse(it.uri),
                dateTaken = it.dateTaken,
                status = it.status
            )
        }
    }

    suspend fun clearDeletedByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            photoDao.deleteByIds(ids)
        }
    }

    suspend fun clearDeletedByUris(contentResolver: ContentResolver, uris: List<Uri>) {
        val ids = uris.mapNotNull { uri ->
            runCatching { uri.lastPathSegment?.toLong() }.getOrNull()
        }
        if (ids.isNotEmpty()) {
            photoDao.deleteByIds(ids)
        }
    }
}

private fun PhotoItem.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        uri = uri.toString(),
        dateTaken = dateTaken,
        status = status
    )
}
