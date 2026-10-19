package com.daohoangson.n8n.notificationlistener.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListenedAppDao {
    @Query("SELECT * FROM listened_apps")
    suspend fun getAll(): List<ListenedApp>

    @Query("SELECT * FROM listened_apps")
    fun observeAll(): Flow<List<ListenedApp>>

    @Upsert
    suspend fun upsert(app: ListenedApp)
}
