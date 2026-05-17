package com.metashield.app.data.repository

import com.metashield.app.data.db.dao.HistoryDao
import com.metashield.app.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)
    fun getRecentHistory(): Flow<List<HistoryEntity>> = historyDao.getRecentHistory()
    fun getHistoryCount(): Flow<Int> = historyDao.getAllHistory().map { it.size }
    suspend fun getHistoryById(id: Long) = historyDao.getHistoryById(id)
    suspend fun addEntry(entity: HistoryEntity) = historyDao.insertHistory(entity)
    suspend fun deleteEntry(entity: HistoryEntity) = historyDao.deleteHistory(entity)
    suspend fun clearAll() = historyDao.clearAll()
    suspend fun deleteOlderThan(timestamp: Long) = historyDao.deleteOlderThan(timestamp)
}
