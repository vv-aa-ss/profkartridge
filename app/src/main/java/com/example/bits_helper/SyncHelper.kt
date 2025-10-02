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
