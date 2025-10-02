package com.example.bits_helper

import androidx.compose.foundation.layout.*
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
    var saveToken by remember { mutableStateOf(false) }
    
    // Загружаем сохраненный токен при инициализации
    LaunchedEffect(Unit) {
        val savedToken = syncManager.getSavedAccessToken()
        if (savedToken != null) {
            accessToken = savedToken
        }
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
                
                // Чекбокс для сохранения токена
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = saveToken,
                        onCheckedChange = { saveToken = it }
                    )
                    Text(
                        text = "Сохранить токен для повторного использования",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
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
                            tint = Color(0xFFF44336)
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
                                        is DiagnosticResult.Success -> Color(0xFF4CAF50)
                                        is DiagnosticResult.Warning -> Color(0xFFFF9800)
                                        is DiagnosticResult.Error -> Color(0xFFF44336)
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
                        if (saveToken) {
                            syncManager.saveAccessToken(accessToken.trim())
                        }
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
