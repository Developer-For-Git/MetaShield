package com.metashield.app.data.db.dao

import androidx.room.*
import com.metashield.app.data.db.entity.RemovalProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemovalProfileDao {

    @Query("SELECT * FROM removal_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<RemovalProfileEntity>>

    @Query("SELECT * FROM removal_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): RemovalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(entity: RemovalProfileEntity): Long

    @Update
    suspend fun updateProfile(entity: RemovalProfileEntity)

    @Delete
    suspend fun deleteProfile(entity: RemovalProfileEntity)

    @Query("UPDATE removal_profiles SET isDefault = 0")
    suspend fun clearDefaultFlag()
}
