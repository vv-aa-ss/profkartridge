package com.example.bits_helper.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Диагностика подключения к Яндекс.Диск API
 */
class YandexDiskDiagnostics(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://cloud-api.yandex.net/v1/disk"
    
    /**
     * Проверяет валидность токена доступа
     */
    suspend fun checkTokenValidity(accessToken: String): DiagnosticResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/")
                .addHeader("Authorization", "OAuth $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val body = response.body?.string() ?: ""
                    DiagnosticResult.Success("Токен действителен. Диск доступен.")
                }
                401 -> DiagnosticResult.Error("Ошибка 401: Неверный токен доступа. Получите новый токен.")
                403 -> DiagnosticResult.Error("Ошибка 403: Доступ запрещен. Проверьте права приложения в OAuth.")
                429 -> DiagnosticResult.Error("Ошибка 429: Превышен лимит запросов. Попробуйте позже.")
                else -> DiagnosticResult.Error("Ошибка ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            DiagnosticResult.Error("Ошибка подключения: ${e.message}")
        }
    }
    
    /**
     * Проверяет доступ к конкретной папке
     */
    suspend fun checkFolderAccess(accessToken: String, folderPath: String): DiagnosticResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/resources?path=$folderPath")
                .addHeader("Authorization", "OAuth $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> DiagnosticResult.Success("Папка $folderPath доступна")
                404 -> DiagnosticResult.Warning("Папка $folderPath не найдена. Создайте папку на Яндекс.Диске")
                403 -> DiagnosticResult.Error("Ошибка 403: Нет доступа к папке $folderPath. Проверьте права приложения")
                401 -> DiagnosticResult.Error("Ошибка 401: Неверный токен доступа")
                else -> DiagnosticResult.Error("Ошибка ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            DiagnosticResult.Error("Ошибка проверки папки: ${e.message}")
        }
    }
    
    /**
     * Проверяет возможность создания файла
     */
    suspend fun checkFileCreationAccess(accessToken: String, filePath: String): DiagnosticResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/resources/upload?path=$filePath&overwrite=true")
                .addHeader("Authorization", "OAuth $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> DiagnosticResult.Success("Можно создавать файлы в $filePath")
                403 -> DiagnosticResult.Error("Ошибка 403: Нет прав на создание файлов. Проверьте права приложения")
                401 -> DiagnosticResult.Error("Ошибка 401: Неверный токен доступа")
                404 -> DiagnosticResult.Warning("Папка не найдена. Создайте папку на Яндекс.Диске")
                else -> DiagnosticResult.Error("Ошибка ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            DiagnosticResult.Error("Ошибка проверки создания файла: ${e.message}")
        }
    }
    
    /**
     * Полная диагностика подключения
     */
    suspend fun fullDiagnostics(accessToken: String): List<DiagnosticResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiagnosticResult>()
        
        // 1. Проверка токена
        results.add(checkTokenValidity(accessToken))
        
        // 2. Проверка доступа к папке
        results.add(checkFolderAccess(accessToken, "/bits_helper"))
        
        // 3. Проверка возможности создания файла
        results.add(checkFileCreationAccess(accessToken, "/bits_helper/bits_helper.db"))
        
        results
    }
}

/**
 * Результат диагностики
 */
sealed class DiagnosticResult {
    data class Success(val message: String) : DiagnosticResult()
    data class Warning(val message: String) : DiagnosticResult()
    data class Error(val message: String) : DiagnosticResult()
}

