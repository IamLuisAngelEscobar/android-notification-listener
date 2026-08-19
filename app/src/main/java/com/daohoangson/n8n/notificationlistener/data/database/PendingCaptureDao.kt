package com.daohoangson.n8n.notificationlistener.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingCaptureDao {
    /**
     * Buffer a capture. IGNORE on the UNIQUE `reference` so an OS re-post of the
     * same notification is a local no-op; returns -1 when it was already queued.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(capture: PendingCapture): Long

    @Query("SELECT * FROM pending_captures ORDER BY id ASC")
    suspend fun getAll(): List<PendingCapture>

    @Query("DELETE FROM pending_captures WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_captures SET attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: Long)

    @Query("SELECT COUNT(*) FROM pending_captures")
    fun countFlow(): Flow<Int>
}
