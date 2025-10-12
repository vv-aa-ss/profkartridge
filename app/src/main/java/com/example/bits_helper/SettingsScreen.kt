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
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Delete
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
    var filterFontSize by remember { mutableStateOf(settingsManager.getFilterFontSize()) }
    var filterIconSize by remember { mutableStateOf(settingsManager.getFilterIconSize()) }
    var compactModeThreshold by remember { mutableStateOf(settingsManager.getCompactModeThreshold()) }
    
    // Диалоги для административных функций
    var showClearAllDataDialog by remember { mutableStateOf(false) }
    var showSyncSetupDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
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
                                imageVector = Icons.Rounded.CloudSync,
                                contentDescription = "Синхронизация",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Синхронизация с Яндекс.Диском",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Настройка токена доступа и параметров синхронизации",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Статус синхронизации
                        val syncManager = remember { SyncManager(context) }
                        val isSyncConfigured = remember { mutableStateOf(syncManager.hasSavedToken()) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSyncConfigured.value) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = "Статус",
                                tint = if (isSyncConfigured.value) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSyncConfigured.value) "Синхронизация настроена" else "Синхронизация не настроена",
                                fontSize = 16.sp,
                                color = if (isSyncConfigured.value) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Кнопка настройки синхронизации
                            OutlinedButton(
                                onClick = { showSyncSetupDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = "Настроить",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(if (isSyncConfigured.value) "Изменить" else "Настроить")
                                }
                            }
                            
                            // Кнопка очистки настроек синхронизации (только если настроена)
                            if (isSyncConfigured.value) {
                                OutlinedButton(
                                    onClick = {
                                        syncManager.clearAccessToken()
                                        isSyncConfigured.value = false
                                        CoroutineScope(Dispatchers.Main).launch {
                                            snackbarHostState.showSnackbar("Настройки синхронизации очищены")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Очистить",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Очистить")
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = "Настройте токен доступа для синхронизации данных с Яндекс.Диском",
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
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Настройки интерфейса",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Настройки интерфейса",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Настройка кнопки фильтров",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Размер шрифта фильтров
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TextFields,
                                contentDescription = "Размер шрифта",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Шрифт:",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Slider(
                                value = filterFontSize,
                                onValueChange = { newValue ->
                                    filterFontSize = newValue
                                    settingsManager.setFilterFontSize(filterFontSize)
                                    onSettingsChanged()
                                },
                                valueRange = 8f..18f,
                                steps = 9, // 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = "${filterFontSize.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                        
                        // Размер иконок фильтров
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Размер иконок",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Кружок:",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Slider(
                                value = filterIconSize,
                                onValueChange = { newValue ->
                                    filterIconSize = newValue
                                    settingsManager.setFilterIconSize(filterIconSize)
                                    onSettingsChanged()
                                },
                                valueRange = 4f..16f,
                                steps = 11, // 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = "${filterIconSize.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                        
                        // Порог компактного режима
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Порог компактного режима",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Компакт режим:",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Slider(
                                value = compactModeThreshold.toFloat(),
                                onValueChange = { newValue ->
                                    compactModeThreshold = newValue.toInt()
                                    settingsManager.setCompactModeThreshold(compactModeThreshold)
                                    onSettingsChanged()
                                },
                                valueRange = 300f..600f,
                                steps = 11, // 300, 330, 360, 390, 420, 450, 480, 510, 540, 570, 600
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = "${compactModeThreshold}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                        
                        Text(
                            text = "Компактный режим активируется для экранов уже указанной ширины. В компактном режиме используются сокращенные названия фильтров.",
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
                            text = "Экспорт, импорт и управление данными приложения",
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
                        
                        // Кнопка обновления подразделений (только если передан ViewModel)
                        if (vm != null) {
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
                        }
                        
                        // Кнопка очистки всех данных (только если передан ViewModel)
                        if (vm != null) {
                            OutlinedButton(
                                onClick = { showClearAllDataDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Очистить все данные")
                            }
                        }
                        
                        if (vm != null) {
                            Text(
                                text = "Внимание: операции очистки нельзя отменить!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
    
    // Диалог настройки синхронизации
    if (showSyncSetupDialog) {
        SyncDialog(
            onDismiss = { 
                showSyncSetupDialog = false
                // Обновляем статус синхронизации после закрытия диалога
                val syncManager = SyncManager(context)
                // Здесь можно обновить isSyncConfigured если нужно
            },
            onSync = { accessToken ->
                // Выполняем синхронизацию
                isSyncing = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val syncManager = SyncManager(context)
                        val result = syncManager.syncDatabase(accessToken)
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is com.example.bits_helper.data.SyncResult.Success -> {
                                    snackbarHostState.showSnackbar("📤 ${result.message}")
                                }
                                is com.example.bits_helper.data.SyncResult.Error -> {
                                    snackbarHostState.showSnackbar("⚠️ ${result.message}")
                                }
                            }
                            isSyncing = false
                            showSyncSetupDialog = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("Ошибка синхронизации: ${e.message}")
                            isSyncing = false
                            showSyncSetupDialog = false
                        }
                    }
                }
            },
            isSyncing = isSyncing
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
