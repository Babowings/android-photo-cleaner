package com.puregallery.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.puregallery.data.AppDatabase
import com.puregallery.data.PhotoRepository
import com.puregallery.media.MediaStoreScanner
import com.puregallery.model.PhotoItem
import com.puregallery.model.PhotoStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryUiState(
    val isLoading: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val errorMessage: String? = null,
    val pendingDeleteCount: Int = 0
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PhotoRepository(
        scanner = MediaStoreScanner(application),
        photoDao = AppDatabase.getInstance(application).photoDao()
    )

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun loadPhotos(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }

            val result = withContext(Dispatchers.IO) {
                runCatching { repository.loadPhotos() }
            }

            _uiState.value = result.fold(
                onSuccess = { photos ->
                    _uiState.value.copy(
                        isLoading = false,
                        photos = photos,
                        errorMessage = null,
                        pendingDeleteCount = photos.count { it.status == PhotoStatus.PENDING_DELETE }
                    )
                },
                onFailure = { throwable ->
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "读取相册失败"
                    )
                }
            )
        }
    }

    fun toggleDelete(photoId: Long) {
        val current = _uiState.value.photos.firstOrNull { it.id == photoId }?.status ?: return
        val nextStatus = if (current == PhotoStatus.PENDING_DELETE) {
            PhotoStatus.NORMAL
        } else {
            PhotoStatus.PENDING_DELETE
        }
        updatePhotoStatus(photoId, nextStatus)
    }

    fun setFavoriteStatus(photoId: Long, isFavorite: Boolean) {
        val nextStatus = if (isFavorite) PhotoStatus.FAVORITE else PhotoStatus.NORMAL
        updatePhotoStatus(photoId, nextStatus)
    }

    private fun updatePhotoStatus(photoId: Long, status: PhotoStatus) {
        val updated = _uiState.value.photos.map { photo ->
            if (photo.id == photoId) photo.copy(status = status) else photo
        }

        _uiState.value = _uiState.value.copy(
            photos = updated,
            pendingDeleteCount = updated.count { it.status == PhotoStatus.PENDING_DELETE }
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(photoId, status, updated)
        }
    }
}
