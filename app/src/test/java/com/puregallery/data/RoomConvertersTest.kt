package com.puregallery.data

import com.puregallery.model.PhotoStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomConvertersTest {
    private val converters = RoomConverters()

    @Test
    fun fromPhotoStatus_shouldReturnEnumName() {
        val value = converters.fromPhotoStatus(PhotoStatus.PENDING_DELETE)
        assertEquals("PENDING_DELETE", value)
    }

    @Test
    fun toPhotoStatus_shouldRestoreEnumValue() {
        val status = converters.toPhotoStatus("FAVORITE")
        assertEquals(PhotoStatus.FAVORITE, status)
    }

    @Test
    fun roundTrip_shouldKeepSameStatus() {
        val original = PhotoStatus.NORMAL
        val serialized = converters.fromPhotoStatus(original)
        val restored = converters.toPhotoStatus(serialized)
        assertEquals(original, restored)
    }
}
