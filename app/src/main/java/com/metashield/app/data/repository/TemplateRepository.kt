package com.metashield.app.data.repository

import com.metashield.app.data.db.dao.TemplateDao
import com.metashield.app.data.db.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao
) {
    fun getAllTemplates(): Flow<List<TemplateEntity>> = templateDao.getAllTemplates()
    suspend fun getTemplateById(id: Long) = templateDao.getTemplateById(id)
    suspend fun saveTemplate(entity: TemplateEntity) = templateDao.insertTemplate(entity)
    suspend fun updateTemplate(entity: TemplateEntity) = templateDao.updateTemplate(entity)
    suspend fun deleteTemplate(entity: TemplateEntity) = templateDao.deleteTemplate(entity)
    suspend fun deleteTemplateById(id: Long) = templateDao.deleteTemplateById(id)
}
