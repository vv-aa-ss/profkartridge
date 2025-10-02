package com.example.bits_helper.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Фоновая задача для синхронизации базы данных с Яндекс.Диском
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val syncManager = SyncManager(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Получаем токен доступа из настроек
            val accessToken = getAccessToken()
            if (accessToken.isBlank()) {
                return@withContext Result.failure()
            }
            
            // Проверяем, нужна ли синхронизация
            if (!syncManager.needsSync()) {
                return@withContext Result.success()
            }
            
            // Выполняем синхронизацию
            val syncResult = syncManager.syncDatabase(accessToken)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    Result.success()
                }
                is SyncResult.Error -> {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun getAccessToken(): String {
        val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getString("access_token", "") ?: ""
    }
}
