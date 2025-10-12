package com.example.bits_helper   // ← замени на свой namespace

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bits_helper.data.AppDatabase
import com.example.bits_helper.data.CartridgeRepository
import com.example.bits_helper.ui.CartridgeUi
import com.example.bits_helper.ui.CartridgeViewModel
import com.example.bits_helper.data.exportDatabase
import com.example.bits_helper.data.importDatabase
import com.example.bits_helper.data.Status
import com.example.bits_helper.data.getRussianName
import com.example.bits_helper.data.SyncManager
import com.example.bits_helper.data.SyncResult
import com.example.bits_helper.StatisticsScreen
import com.example.bits_helper.SyncDialog
import com.example.bits_helper.performSync
import com.example.bits_helper.performAutoSync
import com.example.bits_helper.checkAndPerformDailyUpload
import com.example.bits_helper.performDownloadFromYandexDisk
import com.example.bits_helper.ui.theme.ThemeManager
import com.example.bits_helper.ui.theme.ThemeType
import com.example.bits_helper.ui.theme.Bits_helperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.get(applicationContext)
        val repository = CartridgeRepository(database.cartridgeDao(), database.departmentDao())
        setContent {
            val vm: CartridgeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CartridgeViewModel(repository, applicationContext) as T
                }
            })
            App(vm, this)
        }
    }
    
    /**
     * Полностью перезапускает приложение
     */
    fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Process.killProcess(Process.myPid())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: CartridgeViewModel, activity: ComponentActivity) {
    Bits_helperTheme {
        var showAddDialog by remember { mutableStateOf(false) }
        var changingId by remember { mutableStateOf<Long?>(null) }
        var showSheet by remember { mutableStateOf(false) }
        var addDialogInitialNumber by remember { mutableStateOf("") }
        var showEditDialog by remember { mutableStateOf(false) }
        var editingCartridge by remember { mutableStateOf<CartridgeUi?>(null) }
        var showContextMenu by remember { mutableStateOf<Long?>(null) }
        var showStatistics by remember { mutableStateOf(false) }
        var showSyncDialog by remember { mutableStateOf(false) }
        var forceSyncDialog by remember { mutableStateOf(false) }
        var showDownloadDialog by remember { mutableStateOf(false) }
        var isDownloading by remember { mutableStateOf(false) }
        var isSyncing by remember { mutableStateOf(false) }
        var isUploading by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var settingsChanged by remember { mutableStateOf(0) }
        var showDepartmentManagement by remember { mutableStateOf(false) }
        var showScanResult by remember { mutableStateOf(false) }
        var scanResult by remember { mutableStateOf<com.example.bits_helper.data.StatusUpdateResult?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val items = vm.cartridges.collectAsState(initial = emptyList()).value
        
        // Проверяем ежедневную выгрузку и загрузку при запуске приложения
        LaunchedEffect(Unit) {
            checkAndPerformDailyUpload(activity, snackbarHostState)
            checkAndPerformDailyDownload(activity, snackbarHostState)
        }
        if (showStatistics) {
            StatisticsScreen(
                vm = vm,
                onBack = { showStatistics = false }
            )
        } else if (showDepartmentManagement) {
            DepartmentManagementScreen(
                vm = vm,
                onBack = { showDepartmentManagement = false }
            )
        } else if (showSettings) {
            SettingsScreen(
                onBack = { showSettings = false },
                onThemeChanged = { (activity as ComponentActivity).recreate() },
                activity = activity,
                onShowDepartmentManagement = { showDepartmentManagement = true },
                vm = vm,
                onSettingsChanged = { settingsChanged++ }
            )
        } else {
            Scaffold(
                topBar = { 
                    val context = LocalContext.current
                    HeaderBar(
                        vm, 
                        onSyncClick = { showDownloadDialog = true }, 
                        onForceSyncClick = { forceSyncDialog = true },
                        isUploading = isUploading,
                        settingsChanged = settingsChanged,
                        onUploadClick = {
                            val syncManager = com.example.bits_helper.data.SyncManager(context)
                            
                            if (!syncManager.hasSavedToken()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    snackbarHostState.showSnackbar("Сначала настройте синхронизацию с Яндекс.Диском")
                                }
                                return@HeaderBar
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
                        }
                    ) 
                },      // закреплённая шапка
                bottomBar = { 
                    val context = LocalContext.current
                    BottomBar(vm, onAddClicked = { showAddDialog = true }, snackbarHostState = snackbarHostState, onQrNotFound = { number -> addDialogInitialNumber = number; showAddDialog = true }, onShowStatistics = { showStatistics = true }, onDataRefreshed = { (context as ComponentActivity).recreate() }, onShowSettings = { showSettings = true }, onScanResult = { result -> scanResult = result; showScanResult = true }, activity = activity, isUploading = isUploading, setIsUploading = { isUploading = it }) 
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 12.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item ->
                        CartridgeCard(
                            item,
                            Modifier.fillMaxWidth(),
                            onStatusClick = {
                                changingId = item.id
                                showSheet = true
                            },
                            onLongClick = {
                                showContextMenu = item.id
                            }
                        )
                    }
                }
            }
            
            if (showAddDialog) {
                AddCartridgeDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { number, room, model, date, status, notes ->
                        vm.add(number, room, model, date, status, notes)
                        showAddDialog = false
                        addDialogInitialNumber = ""
                    },
                    initialNumber = addDialogInitialNumber
                )
            }
            if (showSheet && changingId != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
                    StatusSelectList(onPick = { st ->
                        vm.updateStatus(changingId!!, st)
                        showSheet = false
                    })
                }
            }
            if (showEditDialog && editingCartridge != null) {
                EditCartridgeDialog(
                    cartridge = editingCartridge!!,
                    onDismiss = { 
                        showEditDialog = false
                        editingCartridge = null
                    },
                    onSave = { number, room, model, date, status, notes ->
                        vm.updateCartridge(editingCartridge!!.id, number, room, model, date, status, notes)
                        showEditDialog = false
                        editingCartridge = null
                    }
                )
            }
            if (showContextMenu != null) {
                val cartridgeId = showContextMenu!!
                val cartridge = items.find { it.id == cartridgeId }
                if (cartridge != null) {
                    ContextMenuDialog(
                        cartridge = cartridge,
                        onDismiss = { showContextMenu = null },
                        onEdit = {
                            showContextMenu = null
                            editingCartridge = cartridge
                            showEditDialog = true
                        },
                        onDelete = {
                            vm.deleteById(cartridgeId)
                            showContextMenu = null
                            CoroutineScope(Dispatchers.Main).launch {
                                snackbarHostState.showSnackbar("Картридж удален")
                            }
                        }
                    )
                }
            }
        }
        
        // Диалог синхронизации
        if (showSyncDialog) {
            val context = LocalContext.current
            val syncManager = remember { SyncManager(context) }
            
            // Если есть сохраненный токен, предлагаем автоматическую синхронизацию
            if (syncManager.hasSavedToken()) {
                val mainActivity = activity as MainActivity
                LaunchedEffect(Unit) {
                    performAutoSync(context, isSyncing, { isSyncing = it }, snackbarHostState) {
                        // Полностью перезапускаем приложение для 100% обновления
                        GlobalScope.launch {
                            kotlinx.coroutines.delay(1000) // Даем время на завершение синхронизации
                            mainActivity.restartApp()
                        }
                    }
                    showSyncDialog = false
                }
            } else {
                SyncDialog(
                    onDismiss = { showSyncDialog = false },
                    onSync = { accessToken ->
                        val mainActivity = activity as MainActivity
                        performSync(context, accessToken, isSyncing, { isSyncing = it }, { showSyncDialog = it }, snackbarHostState) {
                            // Полностью перезапускаем приложение для 100% обновления
                            GlobalScope.launch {
                                kotlinx.coroutines.delay(1000) // Даем время на завершение синхронизации
                                mainActivity.restartApp()
                            }
                        }
                    },
                    isSyncing = isSyncing
                )
            }
        }
        
        // Принудительный диалог синхронизации (для смены токена)
        if (forceSyncDialog) {
            val context = LocalContext.current
            SyncDialog(
                onDismiss = { forceSyncDialog = false },
                onSync = { accessToken ->
                    val mainActivity = activity as MainActivity
                    performSync(context, accessToken, isSyncing, { isSyncing = it }, { forceSyncDialog = it }, snackbarHostState) {
                        // Полностью перезапускаем приложение для 100% обновления
                        GlobalScope.launch {
                            kotlinx.coroutines.delay(1000) // Даем время на завершение синхронизации
                            mainActivity.restartApp()
                        }
                    }
                },
                isSyncing = isSyncing
            )
        }
        
        // Диалог подтверждения загрузки с Яндекс.Диска
        if (showDownloadDialog) {
            DownloadConfirmationDialog(
                onDismiss = { showDownloadDialog = false },
                onConfirm = {
                    showDownloadDialog = false
                    isDownloading = true
                }
            )
        }
        
        // Выполняем загрузку после подтверждения
        if (isDownloading) {
            val context = LocalContext.current
            val mainActivity = activity as MainActivity
            LaunchedEffect(Unit) {
                performDownloadFromYandexDisk(context, isDownloading, { isDownloading = it }, snackbarHostState) {
                    // Полностью перезапускаем приложение для 100% обновления
                    GlobalScope.launch {
                        kotlinx.coroutines.delay(1000) // Даем время на завершение загрузки
                        mainActivity.restartApp()
                    }
                }
            }
        }
        

        // Карточка результата сканирования
        if (showScanResult && scanResult != null) {
            val settingsManager = remember { SettingsManager(activity) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(settingsManager.getScanResultDelay()) // Используем настраиваемую задержку
                showScanResult = false
                scanResult = null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showScanResult = false; scanResult = null },
                contentAlignment = Alignment.Center
            ) {
                ScanResultCard(
                    result = scanResult!!,
                    onDismiss = { showScanResult = false; scanResult = null }
                )
            }
        }
    }
}

/* =================== HEADER (закреплённый) =================== */

@Composable
fun HeaderBar(vm: CartridgeViewModel, onSyncClick: () -> Unit, onForceSyncClick: () -> Unit, onUploadClick: () -> Unit, isUploading: Boolean = false, settingsChanged: Int = 0) {
    val counts = vm.countsByStatus.collectAsState(initial = emptyMap()).value
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isCompactScreen = screenWidth < 400 // Компактный режим для экранов меньше 400dp
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val showUploadButton = settingsManager.showUploadButton()
    val showDownloadButton = settingsManager.showDownloadButton()
    
    // Перезагружаем настройки при изменении
    LaunchedEffect(settingsChanged) {
        // Настройки обновятся автоматически через remember
    }
    
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isCompactScreen) {
            // Компактный режим для маленьких экранов
        Row(
            Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Только самые важные статусы
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ClickablePill("З: ${counts[Status.IN_REFILL] ?: 0}", 0xFFFFF5CC, 0xFFEAB308) { vm.setFilter(Status.IN_REFILL) }
                    ClickablePill("С: ${counts[Status.COLLECTED] ?: 0}", 0xFFEFF4FB, 0xFF6B7280) { vm.setFilter(Status.COLLECTED) }
                }
                
                // Общий счетчик и кнопки
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TotalCountPill(counts.values.sum()) { vm.setFilter(null) }
                    
                    // Кнопка выгрузки (если включена)
                    if (showUploadButton) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isUploading) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onUploadClick() },
                                        onLongPress = { onForceSyncClick() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = "Выгрузить",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    
                    // Кнопка загрузки (если включена)
                    if (showDownloadButton) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onSyncClick() },
                                        onLongPress = { onForceSyncClick() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "Загрузить",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Обычный режим для больших экранов
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая часть: статусы (адаптивно)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Сокращенные названия для экономии места
                    ClickablePill("Заправка: ${counts[Status.IN_REFILL] ?: 0}", 0xFFFFF5CC, 0xFFEAB308) { vm.setFilter(Status.IN_REFILL) }
            ClickablePill("Собран: ${counts[Status.COLLECTED] ?: 0}", 0xFFEFF4FB, 0xFF6B7280) { vm.setFilter(Status.COLLECTED) }
                }
                
                // Правая часть: общий счетчик и кнопки
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
            TotalCountPill(counts.values.sum()) { vm.setFilter(null) }
                    
                    // Кнопка выгрузки (если включена)
                    if (showUploadButton) {
            Box(
                modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isUploading) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onUploadClick() },
                                        onLongPress = { onForceSyncClick() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = "Выгрузить",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // Кнопка загрузки (если включена)
                    if (showDownloadButton) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onSyncClick() },
                            onLongPress = { onForceSyncClick() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "Загрузить",
                    tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Вторая строка: остальные статусы (адаптивно)
        if (!isCompactScreen) {
        Row(
            Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ClickablePill("Принят: ${counts[Status.RECEIVED] ?: 0}", 0xFFDBEAFE, 0xFF1D4ED8) { vm.setFilter(Status.RECEIVED) }
            ClickablePill("Потерян: ${counts[Status.LOST] ?: 0}", 0xFFFFE4E6, 0xFFEF4444) { vm.setFilter(Status.LOST) }
            ClickablePill("Списан: ${counts[Status.WRITTEN_OFF] ?: 0}", 0xFFF3E8FF, 0xFF8B5CF6) { vm.setFilter(Status.WRITTEN_OFF) }
            }
        } else {
            // В компактном режиме показываем остальные статусы в одной строке с сокращениями
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ClickablePill("П: ${counts[Status.RECEIVED] ?: 0}", 0xFFDBEAFE, 0xFF1D4ED8) { vm.setFilter(Status.RECEIVED) }
                ClickablePill("По: ${counts[Status.LOST] ?: 0}", 0xFFFFE4E6, 0xFFEF4444) { vm.setFilter(Status.LOST) }
                ClickablePill("Сп: ${counts[Status.WRITTEN_OFF] ?: 0}", 0xFFF3E8FF, 0xFF8B5CF6) { vm.setFilter(Status.WRITTEN_OFF) }
            }
        }
    }
}

/* =================== КНОПКИ СНИЗУ =================== */

@Composable
fun BottomBar(vm: CartridgeViewModel, onAddClicked: () -> Unit, snackbarHostState: SnackbarHostState, onQrNotFound: (String) -> Unit, onShowStatistics: () -> Unit, onDataRefreshed: () -> Unit, onShowSettings: () -> Unit, onScanResult: (com.example.bits_helper.data.StatusUpdateResult) -> Unit, activity: ComponentActivity, isUploading: Boolean = false, setIsUploading: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isCompactScreen = screenWidth < 400
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
            .padding(if (isCompactScreen) 12.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filled = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        val ctx = LocalContext.current
        var menuExpanded by remember { mutableStateOf(false) }

        // «+»
        FilledTonalButton(
            onClick = { onAddClicked() },
            shape = RoundedCornerShape(if (isCompactScreen) 16.dp else 18.dp),
            modifier = Modifier.weight(1f).height(if (isCompactScreen) 48.dp else 56.dp),
            contentPadding = PaddingValues(0.dp),
            colors = filled
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Добавить", modifier = Modifier.size(if (isCompactScreen) 24.dp else 28.dp))
        }

        // Сканер (QR/штрихкод)
        val scannerLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
            val value = result.data?.getStringExtra("qr_value")?.trim()
            if (!value.isNullOrEmpty()) {
                vm.progressByNumber(value) { result ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (result != null) {
                            // Показываем подробную карточку с информацией о картридже
                            onScanResult(result)
                        } else {
                            // Картридж не найден - открываем форму добавления с предзаполненным номером
                            onQrNotFound(value)
                        }
                    }
                }
            }
        }
        FilledTonalButton(
            onClick = { scannerLauncher.launch(Intent(ctx, ScannerActivity::class.java)) },
            shape = RoundedCornerShape(if (isCompactScreen) 16.dp else 18.dp),
            modifier = Modifier.weight(1f).height(if (isCompactScreen) 48.dp else 56.dp),
            colors = filled
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Сканер", modifier = Modifier.size(if (isCompactScreen) 24.dp else 28.dp))
        }

        // Меню (импорт/экспорт)
        Box(Modifier.weight(1f).height(if (isCompactScreen) 48.dp else 56.dp)) {
            FilledTonalButton(
                onClick = { menuExpanded = true },
                shape = RoundedCornerShape(if (isCompactScreen) 16.dp else 18.dp),
                modifier = Modifier.fillMaxSize(),
                colors = filled
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Меню", modifier = Modifier.size(if (isCompactScreen) 20.dp else 24.dp))
            }
            DropdownMenu(
                expanded = menuExpanded, 
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Отдать собранные") },
                    onClick = {
                        menuExpanded = false
                        vm.updateCollectedToRefill { updatedCount ->
                            CoroutineScope(Dispatchers.Main).launch {
                                val msg = if (updatedCount > 0) {
                                    "Обновлено картриджей: $updatedCount"
                                } else {
                                    "Нет картриджей со статусом 'Собран'"
                                }
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Статистика") },
                    onClick = {
                        menuExpanded = false
                        onShowStatistics()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Настройки") },
                    onClick = {
                        menuExpanded = false
                        onShowSettings()
                    }
                )
            }
        }
    }
}

/* =================== ЭЛЕМЕНТЫ UI =================== */

@Composable
fun PillStat(text: String, bg: Long, dot: Long) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(bg))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(dot)))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF0F172A))
    }
}

@Composable
fun ClickablePill(text: String, bg: Long, dot: Long, onClick: () -> Unit) {
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF2F2B26)
    val adaptiveBg = if (isDarkTheme) {
        // В темной теме все статусы имеют одинаковый цвет фона
        0xFF4A3F36L // Темно-бежевый цвет, гармонирующий с темой
    } else bg
    
    val adaptiveTextColor = if (isDarkTheme) Color(0xFFF5F5DC) else Color(0xFF2F2B26)
    
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(adaptiveBg))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(dot)))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text, 
            fontSize = 12.sp, 
            color = adaptiveTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TotalCountPill(count: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun PillNeutral(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFEFF4FB))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6B7280)))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF334155))
    }
}

@Composable
fun SummaryPill(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface) }
}

@Composable
fun CartridgeCard(item: CartridgeUi, modifier: Modifier = Modifier, onStatusClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable { onStatusClick() }) {
                    StatusBadge(item.status)
                }
                Spacer(Modifier.width(12.dp))
                Text(item.number, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text("Кабинет: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Text(
                    item.room,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            InfoRow("Модель:", item.model)
            Spacer(Modifier.height(6.dp))
            InfoRow("Дата:", item.date)
            if (!item.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                InfoRow("Заметки:", item.notes ?: "")
            }
            if (!item.department.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    DepartmentTag(item.department ?: "")
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: Status) {
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF2F2B26)
    val (bgColor, dotColor, textColor, label) = when (status) {
        Status.ISSUED -> if (isDarkTheme) 
            listOf(0xFF4A3F36, 0xFF10B981, 0xFFF5F5DC, "Роздан")
        else 
            listOf(0xFFD1FAE5, 0xFF10B981, 0xFF2F2B26, "Роздан")
        Status.IN_REFILL -> if (isDarkTheme)
            listOf(0xFF4A3F36, 0xFFEAB308, 0xFFF5F5DC, "На заправке")
        else
            listOf(0xFFFFF5CC, 0xFFEAB308, 0xFF2F2B26, "На заправке")
        Status.COLLECTED -> if (isDarkTheme)
            listOf(0xFF4A3F36, 0xFF6B7280, 0xFFF5F5DC, "Собран")
        else
            listOf(0xFFEFF4FB, 0xFF6B7280, 0xFF2F2B26, "Собран")
        Status.RECEIVED -> if (isDarkTheme)
            listOf(0xFF4A3F36, 0xFF1D4ED8, 0xFFF5F5DC, "Принят")
        else
            listOf(0xFFDBEAFE, 0xFF1D4ED8, 0xFF2F2B26, "Принят")
        Status.LOST -> if (isDarkTheme)
            listOf(0xFF4A3F36, 0xFFEF4444, 0xFFF5F5DC, "Потерян")
        else
            listOf(0xFFFFE4E6, 0xFFEF4444, 0xFF2F2B26, "Потерян")
        Status.WRITTEN_OFF -> if (isDarkTheme)
            listOf(0xFF4A3F36, 0xFF8B5CF6, 0xFFF5F5DC, "Списан")
        else
            listOf(0xFFF3E8FF, 0xFF8B5CF6, 0xFF2F2B26, "Списан")
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(bgColor as Long))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(dotColor as Long)))
        Spacer(Modifier.width(8.dp))
        Text(label as String, fontSize = 14.sp, color = Color(textColor as Long))
    }
}

@Composable
fun DepartmentTag(department: String) {
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Text(
            text = department,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun AddCartridgeDialog(
    onDismiss: () -> Unit,
    onSave: (number: String, room: String, model: String, date: String, status: Status, notes: String?) -> Unit,
    initialNumber: String = ""
) {
    var number by remember { mutableStateOf(initialNumber) }
    var room by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(Status.COLLECTED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val today = java.time.LocalDate.now().toString()
                onSave(number.trim(), room.trim(), model.trim(), today, status, notes.ifBlank { null })
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Новый картридж", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number, 
                    onValueChange = { number = it }, 
                    label = { Text("Номер") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = room, 
                    onValueChange = { room = it }, 
                    label = { Text("Кабинет") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Модель") })
                StatusDropdown(status = status, onChange = { status = it })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") })
            }
        }
    )
}

@Composable
fun StatusDropdown(status: Status, onChange: (Status) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(12.dp)) {
            Text(
                when (status) {
                    Status.ISSUED -> "Роздан"
                    Status.IN_REFILL -> "На заправке"
                    Status.COLLECTED -> "Собран"
                    Status.RECEIVED -> "Получен"
                    Status.LOST -> "Потерян"
                    Status.WRITTEN_OFF -> "Списан"
                }
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            @Composable
            fun item(opt: Status, label: String) {
                DropdownMenuItem(text = { Text(label) }, onClick = { onChange(opt); expanded = false })
            }
            item(Status.ISSUED, "Роздан")
            item(Status.IN_REFILL, "На заправке")
            item(Status.COLLECTED, "Собран")
            item(Status.RECEIVED, "Принят")
            item(Status.LOST, "Потерян")
            item(Status.WRITTEN_OFF, "Списан")
        }
    }
}

@Composable
fun StatusSelectList(onPick: (Status) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        @Composable
        fun row(st: Status, label: String, bg: Long, dot: Long) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFFFFF))
                    .clickable { onPick(st) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(dot)))
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = 16.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = { onPick(st) }, label = { Text("Выбрать") })
            }
        }
        row(Status.ISSUED, "Роздан", 0xFFD1FAE5, 0xFF10B981)
        row(Status.IN_REFILL, "На заправке", 0xFFF5F5DC, 0xFFDEB887)
        row(Status.COLLECTED, "Собран", 0xFFF5F5DC, 0xFFE6D7C3)
        row(Status.RECEIVED, "Принят", 0xFFF5F5DC, 0xFFD2B48C)
        row(Status.LOST, "Потерян", 0xFFF5F5DC, 0xFFCD853F)
        row(Status.WRITTEN_OFF, "Списан", 0xFFF5F5DC, 0xFFDEB887)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ContextMenuDialog(
    cartridge: CartridgeUi,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Действия с картриджем", fontWeight = FontWeight.SemiBold) },
        text = { Text("Номер: ${cartridge.number}\nКабинет: ${cartridge.room}") },
        confirmButton = {
            Row {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Изменить", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Удалить", modifier = Modifier.size(20.dp))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditCartridgeDialog(
    cartridge: CartridgeUi,
    onDismiss: () -> Unit,
    onSave: (number: String, room: String, model: String, date: String, status: Status, notes: String?) -> Unit
) {
    var number by remember { mutableStateOf(cartridge.number) }
    var room by remember { mutableStateOf(cartridge.room) }
    var model by remember { mutableStateOf(cartridge.model) }
    var notes by remember { mutableStateOf(cartridge.notes ?: "") }
    var status by remember { mutableStateOf(cartridge.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(number.trim(), room.trim(), model.trim(), cartridge.date, status, notes.ifBlank { null })
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Редактировать картридж", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number, 
                    onValueChange = { number = it }, 
                    label = { Text("Номер") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = room, 
                    onValueChange = { room = it }, 
                    label = { Text("Кабинет") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Модель") })
                StatusDropdown(status = status, onChange = { status = it })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") })
            }
        }
    )
}

@Composable
fun ScanResultCard(
    result: com.example.bits_helper.data.StatusUpdateResult,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок с иконкой
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Успешно",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Статус обновлен",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Номер картриджа
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = "Номер",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Номер:",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.number,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Кабинет
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = "Кабинет",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Кабинет:",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.room,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Модель
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = "Модель",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Модель:",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.model,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Подразделение (если есть)
            if (!result.department.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalShipping,
                        contentDescription = "Подразделение",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Подразделение:",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = result.department,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Новый статус
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(result.newStatus)
                Text(
                    text = "→",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Новый статус",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DownloadConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text("Загрузка с Яндекс.Диска", fontWeight = FontWeight.SemiBold)
            }
        },
        text = { 
            Text("Вы действительно хотите загрузить базу данных с Яндекс.Диска? Это действие заменит текущие данные.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { 
                Text("Загрузить") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Отмена") 
            }
        }
    )
}


