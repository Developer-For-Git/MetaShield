package com.metashield.app.data.repository

import android.content.Context
import android.net.Uri
import com.metashield.app.data.db.dao.VaultDao
import com.metashield.app.data.db.entity.VaultEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDao: VaultDao
) {
    private val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }

    fun getVaultEntries(isDecoy: Boolean): Flow<List<VaultEntity>> = 
        vaultDao.getEntriesByPartition(isDecoy)

    suspend fun moveToVault(
        uri: Uri, 
        fileName: String, 
        mimeType: String?, 
        fileType: String, 
        originalSize: Long,
        isDecoy: Boolean = false
    ): Result<VaultEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val internalName = "${UUID.randomUUID()}_${fileName}"
            val internalFile = File(vaultDir, internalName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                internalFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Could not open input stream")

            val entry = VaultEntity(
                fileName = fileName,
                mimeType = mimeType,
                fileType = fileType,
                internalPath = internalFile.absolutePath,
                originalSize = originalSize,
                isDecoy = isDecoy
            )
            
            val id = vaultDao.insert(entry)
            entry.copy(id = id)
        }
    }

    suspend fun deleteFromVault(entry: VaultEntity) = withContext(Dispatchers.IO) {
        val file = File(entry.internalPath)
        if (file.exists()) file.delete()
        vaultDao.delete(entry)
    }

    suspend fun getVaultFile(entry: VaultEntity): File? = withContext(Dispatchers.IO) {
        val file = File(entry.internalPath)
        if (file.exists()) file else null
    }
}
