package com.example.bits_helper

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bits_helper.ui.theme.ThemeManager
import com.example.bits_helper.ui.theme.ThemeType
import com.example.bits_helper.data.exportDatabase
import com.example.bits_helper.data.importDatabase
import com.example.bits_helper.data.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, 
    onThemeChanged: () -> Unit, 
    activity: androidx.activity.ComponentActivity,
    onShowDepartmentManagement: () -> Unit = {},
    vm: com.example.bits_helper.ui.CartridgeViewModel? = null,
    onSettingsChanged: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    var currentTheme by remember { mutableStateOf(themeManager.getThemeType()) }
    var scanDelay by remember { mutableStateOf(settingsManager.getScanResultDelaySeconds()) }
    
    // Диалоги для административных функций
    var showClearDatabaseDialog by remember { mutableStateOf(false) }
    var showClearAllDataDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    // Логика экспорта/импорта
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { exportDatabase(context, uri) }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val mainActivity = activity as MainActivity
        scope.launch { 
            importDatabase(context, uri)
            // Полностью перезапускаем приложение для 100% обновления
            withContext(Dispatchers.Main) {
                GlobalScope.launch {
                    kotlinx.coroutines.delay(1000) // Даем время на завершение импорта
                    mainActivity.restartApp()
                }
            }
        }
    }
    
    // Обработка кнопки "Назад" Android
    BackHandler {
        onBack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Тема приложения",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        ThemeOption(
                            title = "Светлая",
                            description = "Всегда использовать светлую тему",
                            isSelected = currentTheme == ThemeType.LIGHT,
                            onClick = {
                                currentTheme = ThemeType.LIGHT
                                themeManager.setThemeType(ThemeType.LIGHT)
                                onThemeChanged()
                            }
                        )
                        
                        ThemeOption(
                            title = "Темная",
                            description = "Всегда использовать темную тему",
                            isSelected = currentTheme == ThemeType.DARK,
                            onClick = {
                                currentTheme = ThemeType.DARK
                                themeManager.setThemeType(ThemeType.DARK)
                                onThemeChanged()
                            }
                        )
                        
                        ThemeOption(
                            title = "Системная",
                            description = "Следовать настройкам системы",
                            isSelected = currentTheme == ThemeType.SYSTEM,
                            onClick = {
                                currentTheme = ThemeType.SYSTEM
                                themeManager.setThemeType(ThemeType.SYSTEM)
                                onThemeChanged()
                            }
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = "Задержка",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Задержка результата сканирования",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Время показа карточки результата сканирования",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Секунд:",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Slider(
                                value = scanDelay.toFloat(),
                                onValueChange = { newValue ->
                                    scanDelay = newValue.toInt()
                                    settingsManager.setScanResultDelaySeconds(scanDelay)
                                },
                                valueRange = 1f..10f,
                                steps = 8, // 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 секунд
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = scanDelay.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                        
                        Text(
                            text = "Рекомендуется: 3-5 секунд",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = "Кнопки синхронизации",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Кнопки синхронизации",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Выберите какие кнопки отображать в верхней панели",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        var syncButtonsDisplay by remember { mutableStateOf(settingsManager.getSyncButtonsDisplay()) }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RadioButtonOption(
                                title = "Только выгрузка",
                                description = "Показывать только кнопку выгрузки на Яндекс.Диск",
                                isSelected = syncButtonsDisplay == "upload",
                                onClick = {
                                    syncButtonsDisplay = "upload"
                                    settingsManager.setSyncButtonsDisplay("upload")
                                    onSettingsChanged()
                                }
                            )
                            
                            RadioButtonOption(
                                title = "Только загрузка",
                                description = "Показывать только кнопку загрузки с Яндекс.Диска",
                                isSelected = syncButtonsDisplay == "download",
                                onClick = {
                                    syncButtonsDisplay = "download"
                                    settingsManager.setSyncButtonsDisplay("download")
                                    onSettingsChanged()
                                }
                            )
                            
                            RadioButtonOption(
                                title = "Обе кнопки",
                                description = "Показывать кнопки выгрузки и загрузки",
                                isSelected = syncButtonsDisplay == "both",
                                onClick = {
                                    syncButtonsDisplay = "both"
                                    settingsManager.setSyncButtonsDisplay("both")
                                    onSettingsChanged()
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Business,
                                contentDescription = "Подразделения",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Управление подразделениями",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Настройка названий подразделений и соответствий кабинетов",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedButton(
                            onClick = onShowDepartmentManagement,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Business,
                                    contentDescription = "Подразделения",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Управление подразделениями")
                            }
                        }
                        
                        Text(
                            text = "Добавляйте, редактируйте и удаляйте подразделения. Настраивайте соответствие кабинетов подразделениям.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Административные функции (только если передан ViewModel)
            if (vm != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Административные функции",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Управление данными и синхронизацией",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Кнопка выгрузки базы на Яндекс.Диск
                            OutlinedButton(
                                onClick = {
                                    val syncManager = SyncManager(context)
                                    if (!syncManager.hasSavedToken()) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            snackbarHostState.showSnackbar("Сначала настройте синхронизацию с Яндекс.Диском")
                                        }
                                        return@OutlinedButton
                                    }
                                    
                                    isUploading = true
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val result = syncManager.performAutoUpload()
                                            withContext(Dispatchers.Main) {
                                                when (result) {
                                                    is com.example.bits_helper.data.SyncResult.Success -> {
                                                        snackbarHostState.showSnackbar("📤 ${result.message}")
                                                    }
                                                    is com.example.bits_helper.data.SyncResult.Error -> {
                                                        snackbarHostState.showSnackbar("⚠️ ${result.message}")
                                                    }
                                                }
                                                isUploading = false
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                snackbarHostState.showSnackbar("Ошибка выгрузки: ${e.message}")
                                                isUploading = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isUploading
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(if (isUploading) "Выгрузка..." else "Выгрузить базу")
                                }
                            }
                            
                            // Кнопка обновления подразделений
                            OutlinedButton(
                                onClick = {
                                    vm.updateMissingDepartments { updatedCount ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val msg = if (updatedCount > 0) {
                                                "Обновлено подразделений: $updatedCount"
                                            } else {
                                                "Все картриджи уже имеют подразделения"
                                            }
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Обновить подразделения")
                            }
                            
                            // Кнопка очистки картриджей
                            OutlinedButton(
                                onClick = { showClearDatabaseDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Очистить все картриджи")
                            }
                            
                            // Кнопка очистки всех данных
                            OutlinedButton(
                                onClick = { showClearAllDataDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Очистить все данные")
                            }
                            
                            Text(
                                text = "Внимание: операции очистки нельзя отменить!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Управление данными",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Экспорт и импорт базы данных картриджей",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Кнопка экспорта
                            OutlinedButton(
                                onClick = { createDoc.launch("bits_helper.db") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = "Экспорт",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Экспорт БД")
                                }
                            }
                            
                            // Кнопка импорта
                            OutlinedButton(
                                onClick = { openDoc.launch(arrayOf("application/octet-stream", "*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDownload,
                                        contentDescription = "Импорт",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Импорт БД")
                                }
                            }
                        }
                        
                        Text(
                            text = "Экспорт: сохранить данные в файл\nИмпорт: загрузить данные из файла",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Snackbar для уведомлений
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxSize()
    )
    
    // Диалог подтверждения очистки базы данных
    if (showClearDatabaseDialog && vm != null) {
        AlertDialog(
            onDismissRequest = { showClearDatabaseDialog = false },
            title = { 
                Text("Очистить все картриджи", fontWeight = FontWeight.SemiBold) 
            },
            text = { 
                Text("Вы действительно хотите удалить ВСЕ картриджи из базы данных?\n\nЭто действие нельзя отменить. Подразделения останутся без изменений.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearAllCartridges { success ->
                            CoroutineScope(Dispatchers.Main).launch {
                                val msg = if (success) {
                                    "Все картриджи удалены"
                                } else {
                                    "Ошибка при удалении картриджей"
                                }
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                        showClearDatabaseDialog = false
                    }
                ) { 
                    Text("Удалить все", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDatabaseDialog = false }) { 
                    Text("Отмена") 
                }
            }
        )
    }
    
    // Диалог подтверждения очистки всех данных
    if (showClearAllDataDialog && vm != null) {
        AlertDialog(
            onDismissRequest = { showClearAllDataDialog = false },
            title = { 
                Text("Очистить все данные", fontWeight = FontWeight.SemiBold) 
            },
            text = { 
                Text("Вы действительно хотите удалить ВСЕ данные приложения?\n\nЭто включает:\n• Все картриджи\n• Токен синхронизации\n• Настройки синхронизации\n\nЭто действие нельзя отменить!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearAllSyncData { success ->
                            CoroutineScope(Dispatchers.Main).launch {
                                val msg = if (success) {
                                    "Все данные очищены (включая токен синхронизации)"
                                } else {
                                    "Ошибка при очистке данных"
                                }
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                        showClearAllDataDialog = false
                    }
                ) { 
                    Text("Удалить все", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDataDialog = false }) { 
                    Text("Отмена") 
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RadioButtonOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
