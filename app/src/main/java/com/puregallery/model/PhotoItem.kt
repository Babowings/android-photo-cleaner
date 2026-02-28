package com.puregallery.model

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val dateTaken: Long,
    val status: PhotoStatus = PhotoStatus.NORMAL
)
