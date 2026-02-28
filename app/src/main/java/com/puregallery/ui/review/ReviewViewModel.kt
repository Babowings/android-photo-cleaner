package com.puregallery.ui.review

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.puregallery.data.AppDatabase
import com.puregallery.data.PhotoRepository
import com.puregallery.media.DeleteExecutor
import com.puregallery.media.MediaStoreScanner
import com.puregallery.model.PhotoItem
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewUiState(
    val isLoading: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val errorMessage: String? = null
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PhotoRepository(
        scanner = MediaStoreScanner(application),
        photoDao = AppDatabase.getInstance(application).photoDao()
    )

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val deleteExecutor = DeleteExecutor(application)

    fun loadPending() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.getPendingDeletePhotos() }
            }
            _uiState.value = result.fold(
                onSuccess = { list ->
                    ReviewUiState(isLoading = false, photos = list)
                },
                onFailure = {
                    ReviewUiState(
                        isLoading = false,
                        photos = emptyList(),
                        errorMessage = it.message ?: "读取待删列表失败"
                    )
                }
            )
        }
    }

    fun buildDeleteIntentSender(uris: List<Uri>) = deleteExecutor.buildDeleteIntentSender(uris)

    fun deleteDirectlyForApi29(contentResolver: ContentResolver, uris: List<Uri>, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = deleteExecutor.deleteDirectlyForApi29(contentResolver, uris)
            if (deleted > 0) {
                repository.clearDeletedByUris(contentResolver, uris)
            }
            withContext(Dispatchers.Main) {
                onFinished(deleted > 0)
                loadPending()
            }
        }
    }

    fun onSystemDeleteSucceeded(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearDeletedByIds(ids)
            withContext(Dispatchers.Main) {
                loadPending()
            }
        }
    }
}
