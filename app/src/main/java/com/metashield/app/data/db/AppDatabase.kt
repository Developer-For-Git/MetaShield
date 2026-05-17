package com.metashield.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.metashield.app.data.db.dao.HistoryDao
import com.metashield.app.data.db.dao.RemovalProfileDao
import com.metashield.app.data.db.dao.TemplateDao
import com.metashield.app.data.db.dao.VaultDao
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.data.db.entity.RemovalProfileEntity
import com.metashield.app.data.db.entity.TemplateEntity
import com.metashield.app.data.db.entity.VaultEntity

@Database(
    entities = [
        HistoryEntity::class,
        TemplateEntity::class,
        RemovalProfileEntity::class,
        VaultEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun templateDao(): TemplateDao
    abstract fun removalProfileDao(): RemovalProfileDao
    abstract fun vaultDao(): VaultDao
}
