package com.example.bits_helper.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Менеджер синхронизации базы данных с Яндекс.Диском
 * Обрабатывает конфликты и обеспечивает целостность данных
 */
class SyncManager(private val context: Context) {
    
    private val yandexDiskService = YandexDiskService(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    
    private val remoteDbPath = "/bits_helper/bits_helper.db"
    private val backupPath = "/bits_helper/backups"
    
    companion object {
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_LOCAL_VERSION = "local_version"
        private const val KEY_REMOTE_VERSION = "remote_version"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
    
    /**
     * Синхронизирует локальную базу данных с Яндекс.Диском
     * @param accessToken токен доступа к Яндекс.Диск
     * @return результат синхронизации
     */
    suspend fun syncDatabase(accessToken: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localDbFile = context.getDatabasePath("bits_helper.db")
            val remoteExists = yandexDiskService.checkFileExists(accessToken, remoteDbPath)
            
            if (remoteExists.isFailure) {
                return@withContext SyncResult.Error("Ошибка проверки удаленного файла: ${remoteExists.exceptionOrNull()?.message}")
            }
            
            if (!remoteExists.getOrThrow()) {
                // Удаленного файла нет - загружаем локальный
                return@withContext uploadLocalDatabase(accessToken, localDbFile)
            }
            
            // Удаленный файл существует - скачиваем его
            // Это обеспечивает получение актуальных данных с Яндекс.Диска
            return@withContext downloadRemoteDatabase(accessToken, localDbFile)
            
        } catch (e: Exception) {
            SyncResult.Error("Ошибка синхронизации: ${e.message}")
        }
    }
    
    /**
     * Загружает локальную базу данных на Яндекс.Диск
     */
    private suspend fun uploadLocalDatabase(accessToken: String, localDbFile: File): SyncResult {
        // Создаем резервную копию перед загрузкой
        createBackup(accessToken)
        
        val uploadResult = yandexDiskService.uploadDatabase(accessToken, localDbFile, remoteDbPath)
        if (uploadResult.isFailure) {
            return SyncResult.Error("Ошибка загрузки: ${uploadResult.exceptionOrNull()?.message}")
        }
        
        // Обновляем информацию о синхронизации
        updateSyncInfo()
        
        return SyncResult.Success("Локальная база данных загружена на Яндекс.Диск")
    }
    
    /**
     * Скачивает удаленную базу данных с Яндекс.Диска
     */
    private suspend fun downloadRemoteDatabase(accessToken: String, localDbFile: File): SyncResult {
        // Создаем локальную резервную копию
        val backupFile = createLocalBackup(localDbFile)
        
        // Принудительно закрываем подключение к базе данных
        try {
            val database = AppDatabase.get(context)
            database.close()
        } catch (e: Exception) {
            // Игнорируем ошибки закрытия
        }
        
        // Удаляем вспомогательные файлы Room (WAL, SHM)
        val walFile = File(localDbFile.parent, "${localDbFile.name}-wal")
        val shmFile = File(localDbFile.parent, "${localDbFile.name}-shm")
        walFile.delete()
        shmFile.delete()
        
        val downloadResult = yandexDiskService.downloadDatabase(accessToken, remoteDbPath, localDbFile)
        if (downloadResult.isFailure) {
            // Восстанавливаем из резервной копии при ошибке
            if (backupFile.exists()) {
                backupFile.copyTo(localDbFile, overwrite = true)
            }
            return SyncResult.Error("Ошибка скачивания: ${downloadResult.exceptionOrNull()?.message}")
        }
        
        // Проверяем, что файл действительно скачался и имеет размер
        if (!localDbFile.exists() || localDbFile.length() == 0L) {
            return SyncResult.Error("Скачанный файл пуст или не существует")
        }
        
        // Обновляем информацию о синхронизации
        updateSyncInfo()
        
        return SyncResult.Success("Удаленная база данных скачана с Яндекс.Диска (размер: ${localDbFile.length()} байт)")
    }
    
    /**
     * Объединяет изменения из локальной и удаленной баз данных
     */
    private suspend fun mergeDatabases(accessToken: String, localDbFile: File): SyncResult {
        // Создаем резервные копии
        createBackup(accessToken)
        val localBackup = createLocalBackup(localDbFile)
        
        // Скачиваем удаленную версию во временный файл
        val tempFile = File(context.cacheDir, "temp_remote.db")
        val downloadResult = yandexDiskService.downloadDatabase(accessToken, remoteDbPath, tempFile)
        
        if (downloadResult.isFailure) {
            return SyncResult.Error("Ошибка скачивания для объединения: ${downloadResult.exceptionOrNull()?.message}")
        }
        
        // Здесь должна быть логика объединения данных
        // Для простоты используем стратегию "последний выигрывает"
        val mergeResult = performDataMerge(localDbFile, tempFile)
        
        // Очищаем временный файл
        tempFile.delete()
        
        if (mergeResult.isFailure) {
            // Восстанавливаем из резервной копии при ошибке
            if (localBackup.exists()) {
                localBackup.copyTo(localDbFile, overwrite = true)
            }
            return SyncResult.Error("Ошибка объединения: ${mergeResult.exceptionOrNull()?.message}")
        }
        
        // Загружаем объединенную версию
        val uploadResult = yandexDiskService.uploadDatabase(accessToken, localDbFile, remoteDbPath)
        if (uploadResult.isFailure) {
            return SyncResult.Error("Ошибка загрузки объединенной версии: ${uploadResult.exceptionOrNull()?.message}")
        }
        
        updateSyncInfo()
        return SyncResult.Success("Базы данных успешно объединены")
    }
    
    /**
     * Разрешает конфликты между локальной и удаленной версиями
     */
    private suspend fun resolveConflicts(accessToken: String, localDbFile: File): Result<ConflictResolution> {
        val localModified = getLocalLastModified(localDbFile)
        val remoteModified = getRemoteLastModified(accessToken)
        
        return when {
            localModified > remoteModified -> Result.success(ConflictResolution.UPLOAD_LOCAL)
            remoteModified > localModified -> Result.success(ConflictResolution.DOWNLOAD_REMOTE)
            else -> Result.success(ConflictResolution.MERGE) // Времена равны - нужна ручная проверка
        }
    }
    
    /**
     * Выполняет объединение данных из двух баз
     */
    private suspend fun performDataMerge(localFile: File, remoteFile: File): Result<String> {
        // Здесь должна быть сложная логика объединения данных
        // Для демонстрации используем простую стратегию
        return try {
            // В реальном приложении здесь нужно:
            // 1. Загрузить данные из обеих баз
            // 2. Сравнить записи по уникальным идентификаторам
            // 3. Объединить изменения
            // 4. Сохранить результат
            
            Result.success("Объединение выполнено")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Создает резервную копию на Яндекс.Диске
     */
    private suspend fun createBackup(accessToken: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupPath = "$backupPath/backup_$timestamp.db"
        
        val localDbFile = context.getDatabasePath("bits_helper.db")
        yandexDiskService.uploadDatabase(accessToken, localDbFile, backupPath)
    }
    
    /**
     * Создает локальную резервную копию
     */
    private fun createLocalBackup(originalFile: File): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val randomSuffix = (1000..9999).random()
        val backupFile = File(originalFile.parent, "bits_helper_backup_${timestamp}_${randomSuffix}.db")
        
        // Удаляем существующий файл, если он есть
        if (backupFile.exists()) {
            backupFile.delete()
        }
        
        originalFile.copyTo(backupFile)
        return backupFile
    }
    
    private fun getLocalLastModified(file: File): Long {
        return file.lastModified()
    }
    
    private suspend fun getRemoteLastModified(accessToken: String): Long {
        // В реальном приложении нужно получить метаданные файла с Яндекс.Диска
        // Для демонстрации возвращаем текущее время
        return System.currentTimeMillis()
    }
    
    private fun updateSyncInfo() {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Получает время последней синхронизации
     */
    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0)
    }
    
    /**
     * Проверяет, нужна ли синхронизация
     */
    fun needsSync(): Boolean {
        val lastSync = getLastSyncTime()
        val now = System.currentTimeMillis()
        // Синхронизируем, если прошло больше 1 часа
        return (now - lastSync) > 3600000
    }
    
    /**
     * Сохраняет токен доступа
     */
    fun saveAccessToken(token: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }
    
    /**
     * Получает сохраненный токен доступа
     */
    fun getSavedAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Очищает сохраненный токен
     */
    fun clearAccessToken() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }
    
    /**
     * Проверяет, есть ли сохраненный токен
     */
    fun hasSavedToken(): Boolean {
        return getSavedAccessToken() != null
    }
}

/**
 * Результат синхронизации
 */
sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * Стратегии разрешения конфликтов
 */
enum class ConflictResolution {
    UPLOAD_LOCAL,    // Загрузить локальную версию
    DOWNLOAD_REMOTE, // Скачать удаленную версию
    MERGE            // Объединить изменения
}
