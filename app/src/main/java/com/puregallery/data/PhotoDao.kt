package com.puregallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.puregallery.model.PhotoStatus

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo_state")
    suspend fun getAll(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PhotoEntity>)

    @Query("SELECT * FROM photo_state WHERE status = :status")
    suspend fun getByStatus(status: PhotoStatus): List<PhotoEntity>

    @Query("DELETE FROM photo_state WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
