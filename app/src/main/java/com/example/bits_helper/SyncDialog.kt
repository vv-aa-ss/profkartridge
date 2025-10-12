package com.example.bits_helper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.bits_helper.data.YandexDiskDiagnostics
import com.example.bits_helper.data.DiagnosticResult
import com.example.bits_helper.data.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SyncDialog(
    onDismiss: () -> Unit,
    onSync: (String) -> Unit,
    isSyncing: Boolean
) {
    val context = LocalContext.current
    val syncManager = remember { SyncManager(context) }
    
    var accessToken by remember { mutableStateOf("") }
    var showDiagnostics by remember { mutableStateOf(false) }
    var diagnosticResults by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    var isDiagnosing by remember { mutableStateOf(false) }
    var dailyUploadEnabled by remember { mutableStateOf(false) }
    var dailyDownloadEnabled by remember { mutableStateOf(false) }
    
    // Загружаем сохраненный токен и настройки при инициализации
    LaunchedEffect(Unit) {
        val savedToken = syncManager.getSavedAccessToken()
        if (savedToken != null) {
            accessToken = savedToken
        }
        
        // Загружаем настройки ежедневной выгрузки и загрузки
        dailyUploadEnabled = syncManager.isDailyUploadEnabled()
        dailyDownloadEnabled = syncManager.isDailyDownloadEnabled()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text("Синхронизация с Яндекс.Диском", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Введите токен доступа к Яндекс.Диску для синхронизации базы данных между устройствами.")
                
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    label = { Text("Токен доступа") },
                    placeholder = { Text("OAuth токен...") },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Кнопка очистки сохраненного токена
                if (syncManager.hasSavedToken()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = {
                                syncManager.clearAccessToken()
                                accessToken = ""
                            },
                            enabled = !isSyncing && !isDiagnosing
                        ) {
                            Text("Очистить сохраненный токен")
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Настройки автоматической выгрузки
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📤 Автоматическая выгрузка базы данных",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Ежедневная выгрузка
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = dailyUploadEnabled,
                                onCheckedChange = { dailyUploadEnabled = it }
                            )
                            Text(
                                text = "Ежедневная выгрузка при запуске",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                    }
                }
                
                // Ежедневная загрузка
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📥 Автоматическая загрузка базы данных",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Ежедневная загрузка
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = dailyDownloadEnabled,
                                onCheckedChange = { dailyDownloadEnabled = it }
                            )
                            Text(
                                text = "Ежедневная загрузка при запуске",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                    }
                }
                
                if (isSyncing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Синхронизация...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                if (isDiagnosing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Диагностика...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                if (showDiagnostics && diagnosticResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Результаты диагностики:", fontWeight = FontWeight.SemiBold)
                        diagnosticResults.forEach { result ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (result) {
                                        is DiagnosticResult.Success -> Icons.Rounded.CheckCircle
                                        is DiagnosticResult.Warning -> Icons.Rounded.Warning
                                        is DiagnosticResult.Error -> Icons.Rounded.Error
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = when (result) {
                                        is DiagnosticResult.Success -> MaterialTheme.colorScheme.primary
                                        is DiagnosticResult.Warning -> MaterialTheme.colorScheme.tertiary
                                        is DiagnosticResult.Error -> MaterialTheme.colorScheme.error
                                    }
                                )
                                Text(
                                    text = when (result) {
                                        is DiagnosticResult.Success -> result.message
                                        is DiagnosticResult.Warning -> result.message
                                        is DiagnosticResult.Error -> result.message
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { 
                        isDiagnosing = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val diagnostics = YandexDiskDiagnostics(context)
                            val results = diagnostics.fullDiagnostics(accessToken.trim())
                            diagnosticResults = results
                            isDiagnosing = false
                            showDiagnostics = true
                        }
                    },
                    enabled = accessToken.isNotBlank() && !isSyncing && !isDiagnosing
                ) {
                    Text("Диагностика")
                }
                TextButton(
                    onClick = { 
                        // Всегда сохраняем токен
                        syncManager.saveAccessToken(accessToken.trim())
                        
                        // Сохраняем настройки ежедневной выгрузки и загрузки
                        syncManager.setDailyUploadEnabled(dailyUploadEnabled)
                        syncManager.setDailyDownloadEnabled(dailyDownloadEnabled)
                        
                        onSync(accessToken.trim()) 
                    },
                    enabled = accessToken.isNotBlank() && !isSyncing && !isDiagnosing
                ) {
                    Text("Синхронизировать")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSyncing && !isDiagnosing
            ) {
                Text("Отмена")
            }
        }
    )
}
