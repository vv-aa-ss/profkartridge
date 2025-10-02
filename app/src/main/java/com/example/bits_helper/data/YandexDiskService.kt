package com.example.bits_helper.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Сервис для работы с Яндекс.Диск API
 * Позволяет загружать и скачивать файлы базы данных
 */
class YandexDiskService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://cloud-api.yandex.net/v1/disk"
    
    /**
     * Загружает файл базы данных на Яндекс.Диск
     * @param accessToken токен доступа к Яндекс.Диск
     * @param localDbFile локальный файл базы данных
     * @param remotePath путь на Яндекс.Диске (например, "/bits_helper/bits_helper.db")
     */
    suspend fun uploadDatabase(
        accessToken: String,
        localDbFile: File,
        remotePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Получаем ссылку для загрузки
            val uploadUrl = getUploadUrl(accessToken, remotePath)
            if (uploadUrl.isFailure) {
                return@withContext uploadUrl
            }
            
            // 2. Загружаем файл
            val uploadResult = uploadFile(uploadUrl.getOrThrow(), localDbFile)
            if (uploadResult.isFailure) {
                return@withContext uploadResult
            }
            
            Result.success("База данных успешно загружена на Яндекс.Диск")
        } catch (e: Exception) {
            Result.failure(IOException("Ошибка загрузки: ${e.message}", e))
        }
    }
    
    /**
     * Скачивает файл базы данных с Яндекс.Диска
     * @param accessToken токен доступа к Яндекс.Диск
     * @param remotePath путь на Яндекс.Диске
     * @param localFile локальный файл для сохранения
     */
    suspend fun downloadDatabase(
        accessToken: String,
        remotePath: String,
        localFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Получаем ссылку для скачивания
            val downloadUrl = getDownloadUrl(accessToken, remotePath)
            if (downloadUrl.isFailure) {
                return@withContext downloadUrl
            }
            
            // 2. Скачиваем файл
            val downloadResult = downloadFile(downloadUrl.getOrThrow(), localFile)
            if (downloadResult.isFailure) {
                return@withContext downloadResult
            }
            
            Result.success("База данных успешно скачана с Яндекс.Диска")
        } catch (e: Exception) {
            Result.failure(IOException("Ошибка скачивания: ${e.message}", e))
        }
    }
    
    /**
     * Проверяет, существует ли файл на Яндекс.Диске
     */
    suspend fun checkFileExists(
        accessToken: String,
        remotePath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/resources?path=$remotePath")
                .addHeader("Authorization", "OAuth $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            when (response.code) {
                200 -> Result.success(true)
                404 -> Result.success(false)
                403 -> Result.failure(IOException("Ошибка 403: Доступ запрещен. Проверьте токен и права доступа"))
                401 -> Result.failure(IOException("Ошибка 401: Неверный токен доступа"))
                429 -> Result.failure(IOException("Ошибка 429: Превышен лимит запросов"))
                else -> Result.failure(IOException("Ошибка проверки файла: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Ошибка проверки файла: ${e.message}", e))
        }
    }
    
    private suspend fun getUploadUrl(accessToken: String, remotePath: String): Result<String> {
        val request = Request.Builder()
            .url("$baseUrl/resources/upload?path=$remotePath&overwrite=true")
            .addHeader("Authorization", "OAuth $accessToken")
            .build()
        
        val response = client.newCall(request).execute()
        return when {
            response.isSuccessful -> {
                val json = response.body?.string() ?: ""
                val href = extractHrefFromJson(json)
                if (href != null) {
                    Result.success(href)
                } else {
                    Result.failure(IOException("Не удалось получить ссылку для загрузки"))
                }
            }
            response.code == 403 -> Result.failure(IOException("Ошибка 403: Доступ запрещен. Проверьте токен и права доступа"))
            response.code == 401 -> Result.failure(IOException("Ошибка 401: Неверный токен доступа"))
            response.code == 429 -> Result.failure(IOException("Ошибка 429: Превышен лимит запросов"))
            else -> Result.failure(IOException("Ошибка получения ссылки для загрузки: ${response.code} - ${response.message}"))
        }
    }
    
    private suspend fun getDownloadUrl(accessToken: String, remotePath: String): Result<String> {
        val request = Request.Builder()
            .url("$baseUrl/resources/download?path=$remotePath")
            .addHeader("Authorization", "OAuth $accessToken")
            .build()
        
        val response = client.newCall(request).execute()
        return when {
            response.isSuccessful -> {
                val json = response.body?.string() ?: ""
                val href = extractHrefFromJson(json)
                if (href != null) {
                    Result.success(href)
                } else {
                    Result.failure(IOException("Не удалось получить ссылку для скачивания"))
                }
            }
            response.code == 403 -> Result.failure(IOException("Ошибка 403: Доступ запрещен. Проверьте токен и права доступа"))
            response.code == 401 -> Result.failure(IOException("Ошибка 401: Неверный токен доступа"))
            response.code == 404 -> Result.failure(IOException("Ошибка 404: Файл не найден"))
            response.code == 429 -> Result.failure(IOException("Ошибка 429: Превышен лимит запросов"))
            else -> Result.failure(IOException("Ошибка получения ссылки для скачивания: ${response.code} - ${response.message}"))
        }
    }
    
    private suspend fun uploadFile(uploadUrl: String, file: File): Result<String> {
        val requestBody = file.asRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder()
            .url(uploadUrl)
            .put(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            Result.success("Файл загружен")
        } else {
            Result.failure(IOException("Ошибка загрузки файла: ${response.code}"))
        }
    }
    
    private suspend fun downloadFile(downloadUrl: String, localFile: File): Result<String> {
        val request = Request.Builder()
            .url(downloadUrl)
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.byteStream()?.use { inputStream ->
                localFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Result.success("Файл скачан")
        } else {
            Result.failure(IOException("Ошибка скачивания файла: ${response.code}"))
        }
    }
    
    private fun extractHrefFromJson(json: String): String? {
        return try {
            val hrefStart = json.indexOf("\"href\":\"") + 8
            val hrefEnd = json.indexOf("\"", hrefStart)
            if (hrefStart > 7 && hrefEnd > hrefStart) {
                json.substring(hrefStart, hrefEnd)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
