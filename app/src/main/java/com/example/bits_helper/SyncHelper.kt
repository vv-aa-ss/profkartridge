package com.example.bits_helper

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.example.bits_helper.data.SyncManager
import com.example.bits_helper.data.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Выполняет автоматическую синхронизацию с сохраненным токеном
 */
fun performAutoSync(
    context: Context,
    isSyncing: Boolean,
    setIsSyncing: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDataRefreshed: (() -> Unit)? = null
) {
    val syncManager = SyncManager(context)
    val savedToken = syncManager.getSavedAccessToken()
    
    if (savedToken == null) {
        CoroutineScope(Dispatchers.Main).launch {
            snackbarHostState.showSnackbar("Сохраненный токен не найден. Используйте ручную синхронизацию.")
        }
        return
    }
    
    performSync(
        context = context,
        accessToken = savedToken,
        isSyncing = isSyncing,
        setIsSyncing = setIsSyncing,
        setShowDialog = { },
        snackbarHostState = snackbarHostState,
        onDataRefreshed = onDataRefreshed
    )
}

/**
 * Выполняет синхронизацию базы данных с Яндекс.Диском
 */
fun performSync(
    context: Context,
    accessToken: String,
    isSyncing: Boolean,
    setIsSyncing: (Boolean) -> Unit,
    setShowDialog: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDataRefreshed: (() -> Unit)? = null
) {
    setIsSyncing(true)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val syncManager = SyncManager(context)
            val result = syncManager.syncDatabase(accessToken)
            
            withContext(Dispatchers.Main) {
                setIsSyncing(false)
                setShowDialog(false)
                when (result) {
                    is SyncResult.Success -> {
                        snackbarHostState.showSnackbar("${result.message}. Приложение будет перезапущено для обновления данных.")
                        // Перезапускаем активность для 100% обновления
                        onDataRefreshed?.invoke()
                    }
                    is SyncResult.Error -> {
                        snackbarHostState.showSnackbar("Ошибка: ${result.message}")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                setIsSyncing(false)
                setShowDialog(false)
                snackbarHostState.showSnackbar("Ошибка синхронизации: ${e.message}")
            }
        }
    }
}

/**
 * Проверяет и выполняет ежедневную выгрузку при запуске приложения
 */
fun checkAndPerformDailyUpload(
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    val syncManager = SyncManager(context)
    
    // Проверяем, что есть сохраненный токен (иначе нечего синхронизировать)
    if (!syncManager.hasSavedToken()) {
        return // Нет токена - синхронизация невозможна
    }
    
    if (!syncManager.needsDailyUpload()) {
        return // Выгрузка не нужна
    }
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val result = syncManager.performDailyUploadIfNeeded()
            
            if (result != null) {
                withContext(Dispatchers.Main) {
                    when (result) {
                        is SyncResult.Success -> {
                            snackbarHostState.showSnackbar("📤 ${result.message}")
                        }
                        is SyncResult.Error -> {
                            snackbarHostState.showSnackbar("⚠️ ${result.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Ошибка ежедневной выгрузки: ${e.message}")
            }
        }
    }
}

/**
 * Проверяет и выполняет ежедневную загрузку при запуске приложения
 */
fun checkAndPerformDailyDownload(
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    val syncManager = SyncManager(context)
    
    // Проверяем, что есть сохраненный токен (иначе нечего синхронизировать)
    if (!syncManager.hasSavedToken()) {
        return // Нет токена - синхронизация невозможна
    }
    
    if (!syncManager.needsDailyDownload()) {
        return // Загрузка не нужна
    }
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val result = syncManager.performDailyDownloadIfNeeded()
            
            if (result != null) {
                withContext(Dispatchers.Main) {
                    when (result) {
                        is SyncResult.Success -> {
                            snackbarHostState.showSnackbar("📥 ${result.message}")
                        }
                        is SyncResult.Error -> {
                            snackbarHostState.showSnackbar("⚠️ ${result.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Ошибка ежедневной загрузки: ${e.message}")
            }
        }
    }
}

/**
 * Выполняет загрузку базы данных с Яндекс.Диска
 */
fun performDownloadFromYandexDisk(
    context: Context,
    isSyncing: Boolean,
    setIsSyncing: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDataRefreshed: (() -> Unit)? = null
) {
    setIsSyncing(true)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val syncManager = SyncManager(context)
            val savedToken = syncManager.getSavedAccessToken()
            
            if (savedToken == null) {
                withContext(Dispatchers.Main) {
                    setIsSyncing(false)
                    snackbarHostState.showSnackbar("Сохраненный токен не найден. Сначала выполните синхронизацию.")
                }
                return@launch
            }
            
            val result = syncManager.downloadDatabase(savedToken)
            
            withContext(Dispatchers.Main) {
                setIsSyncing(false)
                when (result) {
                    is SyncResult.Success -> {
                        snackbarHostState.showSnackbar("${result.message}. Приложение будет перезапущено для обновления данных.")
                        // Перезапускаем активность для 100% обновления
                        onDataRefreshed?.invoke()
                    }
                    is SyncResult.Error -> {
                        snackbarHostState.showSnackbar("Ошибка: ${result.message}")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                setIsSyncing(false)
                snackbarHostState.showSnackbar("Ошибка загрузки: ${e.message}")
            }
        }
    }
}