package com.puregallery.data

import androidx.room.TypeConverter
import com.puregallery.model.PhotoStatus

class RoomConverters {
    @TypeConverter
    fun fromPhotoStatus(status: PhotoStatus): String = status.name

    @TypeConverter
    fun toPhotoStatus(value: String): PhotoStatus = PhotoStatus.valueOf(value)
}
