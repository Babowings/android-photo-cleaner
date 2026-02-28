package com.puregallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.puregallery.model.PhotoStatus

@Entity(tableName = "photo_state")
data class PhotoEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val dateTaken: Long,
    val status: PhotoStatus
)
