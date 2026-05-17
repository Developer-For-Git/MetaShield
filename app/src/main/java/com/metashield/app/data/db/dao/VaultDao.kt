package com.metashield.app.data.db.dao

import androidx.room.*
import com.metashield.app.data.db.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault WHERE isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getEntriesByPartition(isDecoy: Boolean): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntity): Long

    @Delete
    suspend fun delete(entry: VaultEntity)

    @Query("SELECT * FROM vault WHERE id = :id")
    suspend fun getById(id: Long): VaultEntity?
}
