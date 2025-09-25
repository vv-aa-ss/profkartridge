package com.example.bits_helper   // ← замени на свой namespace

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CartridgeRepository(AppDatabase.get(applicationContext).cartridgeDao())
        setContent {
            val vm: CartridgeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CartridgeViewModel(repository) as T
                }
            })
            App(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: CartridgeViewModel) {
    val fluentColors = lightColorScheme(
        primary = Color(0xFF0078D4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD0E7FF),
        onPrimaryContainer = Color(0xFF001A33),
        secondary = Color(0xFF6B7280),
        surface = Color(0xFFF6F8FB),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFE6EAF0),
        outline = Color(0xFFD1D5DB)
    )
    MaterialTheme(colorScheme = fluentColors) {
        var showAddDialog by remember { mutableStateOf(false) }
        var changingId by remember { mutableStateOf<Long?>(null) }
        var showSheet by remember { mutableStateOf(false) }
        var addDialogInitialNumber by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            containerColor = Color(0xFFF5F6F7),
            topBar = { HeaderBar(vm) },      // закреплённая шапка
            bottomBar = { BottomBar(vm, onAddClicked = { showAddDialog = true }, snackbarHostState = snackbarHostState, onQrNotFound = { number -> addDialogInitialNumber = number; showAddDialog = true }) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            val items = vm.cartridges.collectAsState(initial = emptyList()).value
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
                        }
                    )
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
        }
    }
}

/* =================== HEADER (закреплённый) =================== */

@Composable
fun HeaderBar(vm: CartridgeViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F6F7))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val counts = vm.countsByStatus.collectAsState(initial = emptyMap()).value
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickablePill("Роздан: ${counts[Status.ISSUED] ?: 0}", 0xFFE5F8E9, 0xFF16A34A) { vm.setFilter(Status.ISSUED) }
            Spacer(Modifier.width(10.dp))
            ClickablePill("Собран: ${counts[Status.COLLECTED] ?: 0}", 0xFFEFF4FB, 0xFF6B7280) { vm.setFilter(Status.COLLECTED) }
            Spacer(Modifier.weight(1f))
            SummaryPill("Всего: ${counts.values.sum()}")
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickablePill("На заправке: ${counts[Status.IN_REFILL] ?: 0}", 0xFFFFF5CC, 0xFFEAB308) { vm.setFilter(Status.IN_REFILL) }
            Spacer(Modifier.width(10.dp))
            ClickablePill("Потерян: ${counts[Status.LOST] ?: 0}", 0xFFFFE4E6, 0xFFEF4444) { vm.setFilter(Status.LOST) }
        }
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = { vm.setFilter(null) }) { Text("Сбросить фильтр") }
            Spacer(Modifier.weight(1f))
        }
    }
}

/* =================== КНОПКИ СНИЗУ =================== */

@Composable
fun BottomBar(vm: CartridgeViewModel, onAddClicked: () -> Unit, snackbarHostState: SnackbarHostState, onQrNotFound: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filled = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFFEDE9FE)   // мягкая сиреневая заливка
        )
        val ctx = LocalContext.current
        val scope = remember { CoroutineScope(Dispatchers.IO) }
        val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch { exportDatabase(ctx, uri) }
        }
        val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch { importDatabase(ctx, uri) }
        }
        var menuExpanded by remember { mutableStateOf(false) }

        // «+»
        FilledTonalButton(
            onClick = { onAddClicked() },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f).height(56.dp),
            contentPadding = PaddingValues(0.dp),
            colors = filled
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Добавить", modifier = Modifier.size(28.dp))
        }

        // Сканер (QR/штрихкод)
        val scannerLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
            val value = result.data?.getStringExtra("qr_value")?.trim()
            if (!value.isNullOrEmpty()) {
                vm.progressByNumber(value) { next ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (next != null) {
                            val msg = "Статус обновлён: $next"
                            snackbarHostState.showSnackbar(msg)
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
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f).height(56.dp),
            colors = filled
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Сканер", modifier = Modifier.size(28.dp))
        }

        // Меню (импорт/экспорт)
        Box(Modifier.weight(1f).height(56.dp)) {
            FilledTonalButton(
                onClick = { menuExpanded = true },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxSize(),
                colors = filled
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Меню", modifier = Modifier.size(24.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Экспорт БД") },
                    onClick = {
                        menuExpanded = false
                        createDoc.launch("bits_helper.db")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Импорт БД") },
                    onClick = {
                        menuExpanded = false
                        openDoc.launch(arrayOf("application/octet-stream", "*/*"))
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
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(bg))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(dot)))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF0F172A))
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
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF0F172A)) }
}

@Composable
fun CartridgeCard(item: CartridgeUi, modifier: Modifier = Modifier, onStatusClick: () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable { onStatusClick() }) {
                    StatusBadge(item.status)
                }
                Spacer(Modifier.width(12.dp))
                Text(item.number, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Text("Кабинет: ", color = Color(0xFF64748B), fontSize = 16.sp)
                Text(
                    item.room,
                    color = Color(0xFF0F172A),
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
        }
    }
}

@Composable
fun StatusBadge(status: Status) {
    val (bgColor, dotColor, label) = when (status) {
        Status.ISSUED -> Triple(0xFFE5F8E9, 0xFF16A34A, "Роздан")
        Status.IN_REFILL -> Triple(0xFFFFF5CC, 0xFFEAB308, "На заправке")
        Status.COLLECTED -> Triple(0xFFE5E7EB, 0xFF6B7280, "Собран")
        Status.RECEIVED -> Triple(0xFFDBEAFE, 0xFF1D4ED8, "Принят")
        Status.LOST -> Triple(0xFFFFE4E6, 0xFFEF4444, "Потерян")
        Status.WRITTEN_OFF -> Triple(0xFFFCE7F3, 0xFFDB2777, "Списан")
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(bgColor))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(dotColor)))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, color = Color(0xFF0F172A))
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
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Номер") })
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Кабинет") })
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
        row(Status.ISSUED, "Роздан", 0xFFE5F8E9, 0xFF16A34A)
        row(Status.IN_REFILL, "На заправке", 0xFFFFF5CC, 0xFFEAB308)
        row(Status.COLLECTED, "Собран", 0xFFE5E7EB, 0xFF6B7280)
        row(Status.RECEIVED, "Принят", 0xFFDBEAFE, 0xFF1D4ED8)
        row(Status.LOST, "Потерян", 0xFFFFE4E6, 0xFFEF4444)
        row(Status.WRITTEN_OFF, "Списан", 0xFFFCE7F3, 0xFFDB2777)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFE2E8F0)))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color(0xFF64748B), fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color(0xFF0F172A), fontSize = 16.sp)
    }
}

// Демоданные больше не нужны, всё приходит из БД через ViewModel
